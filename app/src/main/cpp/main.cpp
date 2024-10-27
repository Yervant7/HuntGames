#include <jni.h>
#include <cstdio>
#include <cstring>
#include <memory>
#include <vector>
#include <fstream>
#include <sstream>
#include <dirent.h>
#include <cinttypes>
#include <iostream>
#include <mutex>
#include <android/log.h>
#include "MapRegionHelper.h"
#include "MemSearchKit/MemSearchKitUmbrella.h"
#include "MemoryReaderWriter37.h"
#include "MapRegionType.h"

using namespace MemorySearchKit;

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "rwProcMem", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "rwProcMem", __VA_ARGS__)

template <typename T>
T read_memory(CMemoryReaderWriter *pRwDriver, uint64_t hProcess, uint64_t pBuf) {
    T readBuf;
    size_t real_read = 0;
    BOOL read_res = pRwDriver->ReadProcessMemory(hProcess, pBuf, &readBuf, sizeof(T), &real_read, TRUE);

    if (read_res && real_read == sizeof(T)) {
        LOGD("Success in reading");
    }
    return readBuf;
}

template <typename T>
void write_memory(CMemoryReaderWriter *pRwDriver, uint64_t hProcess, uint64_t address, T userValue) {
    size_t real_write = 0;
    BOOL write_res = pRwDriver->WriteProcessMemory(
            hProcess, address, &userValue, sizeof(T), &real_write, TRUE
    );

    if (write_res) {
        LOGD("WriteProcessMemory at address: %lu, actual write size: %zu\n", address, real_write);
    } else {
        LOGE("Failed to write memory at address: %lu", address);
    }
}

template <typename T>
std::vector<uint64_t> filter_memory(
        CMemoryReaderWriter *pRwDriver,
        uint64_t hProcess,
        T value,
        std::vector<uint64_t> addresses
) {
    std::vector<uint64_t> vSearchResult;

    for(uint64_t addr : addresses) {
        T readBuf;
        size_t real_read = 0;
        BOOL read_res = pRwDriver->ReadProcessMemory(hProcess, addr, &readBuf, sizeof(T), &real_read, TRUE);

        if (read_res && real_read == sizeof(T) && readBuf == value) {
            vSearchResult.push_back(addr);
        }
    }

    return vSearchResult;
}

template <typename T>
std::vector<ADDR_RESULT_INFO> normal_val_search(CMemoryReaderWriter *pRwDriver, uint64_t hProcess, size_t nWorkThreadCount, T searchValue, RangeType range, bool physicalMemoryOnly) {

    std::vector<ADDR_RESULT_INFO> vSearchResult;
    std::vector<DRIVER_REGION_INFO> vScanMemMaps;
    GetMemRegion(pRwDriver, hProcess,
                 range,
                 physicalMemoryOnly,
                 vScanMemMaps);
    if (vScanMemMaps.empty()) {
        LOGE("No memory to search");
        pRwDriver->CloseHandle(hProcess);
        LOGD("Call driver CloseHandle:%" PRIu64, hProcess);
        return vSearchResult;
    }

    std::shared_ptr<MemSearchSafeWorkSecWrapper> spvWaitScanMemSec = std::make_shared<MemSearchSafeWorkSecWrapper>();
    if (!spvWaitScanMemSec) {
        return vSearchResult;
    }
    for (auto & item : vScanMemMaps) {
        spvWaitScanMemSec->push_back(item.baseaddress, item.size, 0, item.size);
    }

    {
        SearchValue<T>(
                pRwDriver,
                hProcess,
                spvWaitScanMemSec,
                searchValue,
                0,
                0.001,
                SCAN_TYPE::ACCURATE_VAL,
                nWorkThreadCount,
                vSearchResult,
                sizeof(T));

        LOGD("A total of %zu addresses were searched", vSearchResult.size());
    }

    //Search again
    if (!vSearchResult.empty()) {
        std::vector<ADDR_RESULT_INFO> vWaitSearchAddr; //List of memory addresses to be searched

        vWaitSearchAddr = vSearchResult;

        //Search again
        vSearchResult.clear();

        std::vector<ADDR_RESULT_INFO> vErrorList;
        SearchAddrNextValue<T>(
                pRwDriver,
                hProcess,
                vWaitSearchAddr, //待搜索的内存地址列表
                searchValue, //搜索数值
                0,
                0.001, //误差范围
                SCAN_TYPE::ACCURATE_VAL, //搜索类型: 精确搜索
                nWorkThreadCount, //搜索线程数
                vSearchResult,
                vErrorList); //搜索后的结果
        if (!vErrorList.empty()) {
            LOGD("Filtered Addresses %zu", vErrorList.size());
        }
    }

    return vSearchResult;
}

size_t getThreads() {
    size_t numThreads = std::thread::hardware_concurrency();
    if (numThreads == 0) {
        return 1;
    } else {
        return numThreads;
    }
}

RangeType getrange(int rangenumber) {
    switch(rangenumber) {
        case 1000:
            return RangeType::RECOMMENDED;
        case 2000:
            return RangeType::B_BAD;
        case 3000:
            return RangeType::C_ALLOC;
        case 4000:
            return RangeType::C_BSS;
        case 5000:
            return RangeType::C_DATA;
        case 6000:
            return RangeType::C_HEAP;
        case 7000:
            return RangeType::JAVA_HEAP;
        case 8000:
            return RangeType::A_ANONMYOUS;
        case 9000:
            return RangeType::CODE_SYSTEM;
        case 10000:
            return RangeType::STACK;
        case 11000:
            return RangeType::ASHMEM;
        case 12000:
            return RangeType::X;
        case 13000:
            return RangeType::R0_0;
        case 14000:
            return RangeType::RW_0;
        default:
            return RangeType::RECOMMENDED;
    }
}

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_yervant_huntgames_backend_HuntService_checkrootjni(JNIEnv *env, jobject) {
    return getuid() == 0;
}

JNIEXPORT jlong JNICALL Java_com_yervant_huntgames_backend_HuntService_getpidtarget(JNIEnv *env, jobject, jstring pkgname) {
    BOOL bOutListCompleted;
    BOOL b;
    std::vector<int> vPID;

    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return -1;
    }
    b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return -1;
    }

    b = rwDriver.GetProcessPidList(vPID, FALSE, bOutListCompleted);
    if (!b) {
        LOGE("Failed to get pid list");
        return -1;
    }

    const char *pkgnameStr = env->GetStringUTFChars(pkgname, nullptr);

    long pidWithMaxRss = -1;
    uint64_t maxRss = 0;

    for (int pid : vPID) {
        uint64_t hProcess = rwDriver.OpenProcess(pid);
        if (!hProcess) { continue; }

        uint64_t outRss = 0;
        rwDriver.GetProcessRSS(hProcess, outRss);

        char cmdline[100] = { 0 };
        rwDriver.GetProcessCmdline(hProcess, cmdline, sizeof(cmdline));

        if (strstr(cmdline, pkgnameStr) != nullptr) {
            if (outRss > maxRss) {
                maxRss = outRss;
                pidWithMaxRss = pid;
            }
        }

        rwDriver.CloseHandle(hProcess);
    }

    env->ReleaseStringUTFChars(pkgname, pkgnameStr);

    return pidWithMaxRss;
}

JNIEXPORT jint JNICALL Java_com_yervant_huntgames_backend_HuntService_readMemoryInt(JNIEnv *env, jobject, jlong address, jlong pid) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return -1;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return -1;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return -1;
    }

    LOGD("Opened process handle: %" PRIu64, hProcess);

    auto addr = static_cast<uint64_t>(address);

    auto res = read_memory<int>(&rwDriver, hProcess, addr);

    rwDriver.CloseHandle(hProcess);
    return res;
}

JNIEXPORT jlong JNICALL Java_com_yervant_huntgames_backend_HuntService_readMemoryLong(JNIEnv *env, jobject, jlong address, jlong pid) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return -1;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return -1;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return -1;
    }

    LOGD("Opened process handle: %" PRIu64, hProcess);

    auto addr = static_cast<uint64_t>(address);

    auto res = read_memory<long>(&rwDriver, hProcess, addr);

    rwDriver.CloseHandle(hProcess);
    return res;
}

JNIEXPORT jfloat JNICALL Java_com_yervant_huntgames_backend_HuntService_readMemoryFloat(JNIEnv *env, jobject, jlong address, jlong pid) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return -1;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return -1;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return -1;
    }

    LOGD("Opened process handle: %" PRIu64, hProcess);

    auto addr = static_cast<uint64_t>(address);

    auto res = read_memory<float>(&rwDriver, hProcess, addr);

    rwDriver.CloseHandle(hProcess);
    return res;
}

JNIEXPORT jdouble JNICALL Java_com_yervant_huntgames_backend_HuntService_readMemoryDouble(JNIEnv *env, jobject, jlong address, jlong pid) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return -1;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return -1;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return -1;
    }

    LOGD("Opened process handle: %" PRIu64, hProcess);

    auto addr = static_cast<uint64_t>(address);

    auto res = read_memory<double>(&rwDriver, hProcess, addr);

    rwDriver.CloseHandle(hProcess);
    return res;
}

JNIEXPORT void JNICALL Java_com_yervant_huntgames_backend_HuntService_writeMemoryInt(JNIEnv *env, jobject, jlong pid, jlong address, jint value) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return;
    }

    LOGD("Opened process handle: %" PRIu64, hProcess);

    auto addr = static_cast<uint64_t>(address);

    write_memory<int>(&rwDriver, hProcess, addr, value);
    rwDriver.CloseHandle(hProcess);
}

JNIEXPORT void JNICALL Java_com_yervant_huntgames_backend_HuntService_writeMemoryLong(JNIEnv *env, jobject, jlong pid, jlong address, jlong value) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return;
    }

    LOGD("Opened process handle: %" PRIu64, hProcess);

    auto addr = static_cast<uint64_t>(address);

    write_memory<long>(&rwDriver, hProcess, addr, value);
    rwDriver.CloseHandle(hProcess);
}

JNIEXPORT void JNICALL Java_com_yervant_huntgames_backend_HuntService_writeMemoryFloat(JNIEnv *env, jobject, jlong pid, jlong address, jfloat value) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return;
    }

    LOGD("Opened process handle: %" PRIu64, hProcess);

    auto addr = static_cast<uint64_t>(address);

    write_memory<float>(&rwDriver, hProcess, addr, value);
    rwDriver.CloseHandle(hProcess);
}

JNIEXPORT void JNICALL Java_com_yervant_huntgames_backend_HuntService_writeMemoryDouble(JNIEnv *env, jobject, jlong pid, jlong address, jdouble value) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return;
    }

    LOGD("Opened process handle: %" PRIu64, hProcess);

    auto addr = static_cast<uint64_t>(address);

    write_memory<double>(&rwDriver, hProcess, addr, value);
    rwDriver.CloseHandle(hProcess);
}

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_searchMemoryInt(JNIEnv *env, jobject, jlong pid, jint searchValue, jint range, jboolean physicalMemoryOnly) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return nullptr;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return nullptr;
    }

    RangeType rangeType = getrange(static_cast<int>(range));

    LOGD("Opened process handle: %" PRIu64, hProcess);
    size_t threads = getThreads();
    std::vector<ADDR_RESULT_INFO> results = normal_val_search<int>(&rwDriver, hProcess, threads, static_cast<int>(searchValue), rangeType, physicalMemoryOnly);

    jlongArray resultArray = env->NewLongArray(results.size());
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        rwDriver.CloseHandle(hProcess);
        return nullptr;
    }

    std::vector<jlong> addrArray(results.size());
    for (size_t i = 0; i < results.size(); ++i) {
        addrArray[i] = static_cast<jlong>(results[i].addr);
    }

    env->SetLongArrayRegion(resultArray, 0, results.size(), addrArray.data());

    rwDriver.CloseHandle(hProcess);
    return resultArray;
}

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_searchMemoryLong(JNIEnv *env, jobject, jlong pid, jlong searchValue, jint range, jboolean physicalMemoryOnly) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return nullptr;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return nullptr;
    }

    RangeType rangeType = getrange(static_cast<int>(range));

    LOGD("Opened process handle: %" PRIu64, hProcess);
    size_t threads = getThreads();
    std::vector<ADDR_RESULT_INFO> results = normal_val_search<long>(&rwDriver, hProcess, threads, static_cast<long>(searchValue), rangeType, physicalMemoryOnly);

    jlongArray resultArray = env->NewLongArray(results.size());
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        rwDriver.CloseHandle(hProcess);
        return nullptr;
    }

    std::vector<jlong> addrArray(results.size());
    for (size_t i = 0; i < results.size(); ++i) {
        addrArray[i] = static_cast<jlong>(results[i].addr);
    }

    env->SetLongArrayRegion(resultArray, 0, results.size(), addrArray.data());

    rwDriver.CloseHandle(hProcess);
    return resultArray;
}

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_searchMemoryFloat(JNIEnv *env, jobject, jlong pid, jfloat searchValue, jint range, jboolean physicalMemoryOnly) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return nullptr;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return nullptr;
    }

    RangeType rangeType = getrange(static_cast<int>(range));

    LOGD("Opened process handle: %" PRIu64, hProcess);
    size_t threads = getThreads();
    std::vector<ADDR_RESULT_INFO> results = normal_val_search<float>(&rwDriver, hProcess, threads, static_cast<float>(searchValue), rangeType, physicalMemoryOnly);

    jlongArray resultArray = env->NewLongArray(results.size());
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        rwDriver.CloseHandle(hProcess);
        return nullptr;
    }

    std::vector<jlong> addrArray(results.size());
    for (size_t i = 0; i < results.size(); ++i) {
        addrArray[i] = static_cast<jlong>(results[i].addr);
    }

    env->SetLongArrayRegion(resultArray, 0, results.size(), addrArray.data());

    rwDriver.CloseHandle(hProcess);
    return resultArray;
}

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_searchMemoryDouble(JNIEnv *env, jobject, jlong pid, jdouble searchValue, jint range, jboolean physicalMemoryOnly) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return nullptr;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return nullptr;
    }

    RangeType rangeType = getrange(static_cast<int>(range));

    LOGD("Opened process handle: %" PRIu64, hProcess);
    size_t threads = getThreads();
    std::vector<ADDR_RESULT_INFO> results = normal_val_search<double>(&rwDriver, hProcess, threads, static_cast<double>(searchValue), rangeType, physicalMemoryOnly);

    jlongArray resultArray = env->NewLongArray(results.size());
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        rwDriver.CloseHandle(hProcess);
        return nullptr;
    }

    std::vector<jlong> addrArray(results.size());
    for (size_t i = 0; i < results.size(); ++i) {
        addrArray[i] = static_cast<jlong>(results[i].addr);
    }

    env->SetLongArrayRegion(resultArray, 0, results.size(), addrArray.data());

    rwDriver.CloseHandle(hProcess);
    return resultArray;
}


JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_filterMemoryInt(JNIEnv *env, jobject, jlong pid, jlongArray addressArray, jint filterValue) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return nullptr;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return nullptr;
    }

    LOGD("Opened process handle: %" PRIu64, hProcess);

    jsize length = env->GetArrayLength(addressArray);
    std::vector<uint64_t> addresses(length);

    jlong *elements = env->GetLongArrayElements(addressArray, nullptr);
    for (jsize i = 0; i < length; ++i) {
        addresses[i] = static_cast<uint64_t>(elements[i]);
    }
    env->ReleaseLongArrayElements(addressArray, elements, 0);

    std::vector<uint64_t> filteredAddresses = filter_memory<int>(&rwDriver, hProcess, static_cast<int>(filterValue), addresses);

    jlongArray resultArray = env->NewLongArray(filteredAddresses.size());
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        rwDriver.CloseHandle(hProcess);
        return nullptr;
    }

    std::vector<jlong> addrArray(filteredAddresses.size());
    for (size_t i = 0; i < filteredAddresses.size(); ++i) {
        addrArray[i] = static_cast<jlong>(filteredAddresses[i]);
    }

    env->SetLongArrayRegion(resultArray, 0, filteredAddresses.size(), addrArray.data());

    rwDriver.CloseHandle(hProcess);
    return resultArray;
}

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_filterMemoryLong(JNIEnv *env, jobject, jlong pid, jlongArray addressArray, jlong filterValue) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return nullptr;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return nullptr;
    }

    LOGD("Opened process handle: %" PRIu64, hProcess);

    jsize length = env->GetArrayLength(addressArray);
    std::vector<uint64_t> addresses(length);

    jlong *elements = env->GetLongArrayElements(addressArray, nullptr);
    for (jsize i = 0; i < length; ++i) {
        addresses[i] = static_cast<uint64_t>(elements[i]);
    }
    env->ReleaseLongArrayElements(addressArray, elements, 0);

    std::vector<uint64_t> filteredAddresses = filter_memory<long>(&rwDriver, hProcess, static_cast<long>(filterValue), addresses);

    jlongArray resultArray = env->NewLongArray(filteredAddresses.size());
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        rwDriver.CloseHandle(hProcess);
        return nullptr;
    }

    std::vector<jlong> addrArray(filteredAddresses.size());
    for (size_t i = 0; i < filteredAddresses.size(); ++i) {
        addrArray[i] = static_cast<jlong>(filteredAddresses[i]);
    }

    env->SetLongArrayRegion(resultArray, 0, filteredAddresses.size(), addrArray.data());

    rwDriver.CloseHandle(hProcess);
    return resultArray;
}

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_filterMemoryFloat(JNIEnv *env, jobject, jlong pid, jlongArray addressArray, jfloat filterValue) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return nullptr;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return nullptr;
    }

    LOGD("Opened process handle: %" PRIu64, hProcess);

    jsize length = env->GetArrayLength(addressArray);
    std::vector<uint64_t> addresses(length);

    jlong *elements = env->GetLongArrayElements(addressArray, nullptr);
    for (jsize i = 0; i < length; ++i) {
        addresses[i] = static_cast<uint64_t>(elements[i]);
    }
    env->ReleaseLongArrayElements(addressArray, elements, 0);

    std::vector<uint64_t> filteredAddresses = filter_memory<float>(&rwDriver, hProcess, static_cast<float>(filterValue), addresses);

    jlongArray resultArray = env->NewLongArray(filteredAddresses.size());
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        rwDriver.CloseHandle(hProcess);
        return nullptr;
    }

    std::vector<jlong> addrArray(filteredAddresses.size());
    for (size_t i = 0; i < filteredAddresses.size(); ++i) {
        addrArray[i] = static_cast<jlong>(filteredAddresses[i]);
    }

    env->SetLongArrayRegion(resultArray, 0, filteredAddresses.size(), addrArray.data());

    rwDriver.CloseHandle(hProcess);
    return resultArray;
}

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_filterMemoryDouble(JNIEnv *env, jobject, jlong pid, jlongArray addressArray, jdouble filterValue) {
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    BOOL b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return nullptr;
    }
    auto target_pid = (pid_t)pid;
    uint64_t hProcess = rwDriver.OpenProcess(target_pid);
    if (!hProcess) {
        LOGE("Failed to open process");
        return nullptr;
    }

    LOGD("Opened process handle: %" PRIu64, hProcess);

    jsize length = env->GetArrayLength(addressArray);
    std::vector<uint64_t> addresses(length);

    jlong *elements = env->GetLongArrayElements(addressArray, nullptr);
    for (jsize i = 0; i < length; ++i) {
        addresses[i] = static_cast<uint64_t>(elements[i]);
    }
    env->ReleaseLongArrayElements(addressArray, elements, 0);

    std::vector<uint64_t> filteredAddresses = filter_memory<double>(&rwDriver, hProcess, static_cast<double>(filterValue), addresses);

    jlongArray resultArray = env->NewLongArray(filteredAddresses.size());
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        rwDriver.CloseHandle(hProcess);
        return nullptr;
    }

    std::vector<jlong> addrArray(filteredAddresses.size());
    for (size_t i = 0; i < filteredAddresses.size(); ++i) {
        addrArray[i] = static_cast<jlong>(filteredAddresses[i]);
    }

    env->SetLongArrayRegion(resultArray, 0, filteredAddresses.size(), addrArray.data());

    rwDriver.CloseHandle(hProcess);
    return resultArray;
}

}

