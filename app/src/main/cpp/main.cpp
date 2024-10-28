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
        LOGD("Success reading");
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
        T value2,
        std::vector<uint64_t> addresses
) {
    std::vector<uint64_t> vSearchResult;
    T check1 = 0;
    T check2 = 0.0;
    bool between = false;
    if (value2 != check1 || value2 != check2) {
        between = true;
    }

    for(uint64_t addr : addresses) {
        T readBuf;
        size_t real_read = 0;
        BOOL read_res = pRwDriver->ReadProcessMemory(hProcess, addr, &readBuf, sizeof(T), &real_read, TRUE);

        if (read_res && real_read == sizeof(T) && readBuf == value) {
            vSearchResult.push_back(addr);
        } else if (read_res && real_read == sizeof(T) && between && readBuf > value && readBuf < value2) {
            vSearchResult.push_back(addr);
        }
    }

    return vSearchResult;
}

template <typename T>
std::vector<ADDR_RESULT_INFO> normal_val_search(CMemoryReaderWriter *pRwDriver, uint64_t hProcess, size_t nWorkThreadCount, T searchValue, T searchValue2, RangeType range, SCAN_TYPE scantype, bool physicalMemoryOnly) {

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
                searchValue2,
                0.001,
                scantype,
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
                searchValue2,
                0.001, //误差范围
                scantype, //搜索类型: 精确搜索
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
        case 1:
            return RangeType::RECOMMENDED;
        case 2:
            return RangeType::B_BAD;
        case 3:
            return RangeType::C_ALLOC;
        case 4:
            return RangeType::C_BSS;
        case 5:
            return RangeType::C_DATA;
        case 6:
            return RangeType::C_HEAP;
        case 7:
            return RangeType::JAVA_HEAP;
        case 8:
            return RangeType::A_ANONMYOUS;
        case 9:
            return RangeType::CODE_SYSTEM;
        case 10:
            return RangeType::STACK;
        case 11:
            return RangeType::ASHMEM;
        case 12:
            return RangeType::X;
        case 13:
            return RangeType::R0_0;
        case 14:
            return RangeType::RW_0;
        default:
            return RangeType::RECOMMENDED;
    }
}

SCAN_TYPE getscantype(int type) {
    switch(type) {
        case 0:
            return SCAN_TYPE::ACCURATE_VAL;
        case 1:
            return SCAN_TYPE::BETWEEN_VAL;
        default:
            return SCAN_TYPE::ACCURATE_VAL;
    }
}

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_yervant_huntgames_backend_HuntService_checkrootjni(JNIEnv *env, jobject) {
    return getuid() == 0;
}

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_getPidList(JNIEnv *env, jobject) {
    BOOL bOutListCompleted;
    BOOL b;
    std::vector<int> vPID;

    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return nullptr;
    }

    b = rwDriver.GetProcessPidList(vPID, FALSE, bOutListCompleted);
    if (!b) {
        LOGE("Failed to get pid list");
        return nullptr;
    }

    jlongArray resultArray = env->NewLongArray(vPID.size());
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        return nullptr;
    }

    std::vector<jlong> pidArray(vPID.size());
    for (size_t i = 0; i < vPID.size(); ++i) {
        pidArray[i] = static_cast<jlong>(vPID[i]);
    }

    env->SetLongArrayRegion(resultArray, 0, vPID.size(), pidArray.data());

    return resultArray;
}

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_getPidRss(JNIEnv *env, jobject, jlongArray pidlist) {
    CMemoryReaderWriter rwDriver;
    int err;
    BOOL b;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return nullptr;
    }

    jsize length = env->GetArrayLength(pidlist);
    std::vector<jlong> PidList(length);

    jlong *elements = env->GetLongArrayElements(pidlist, nullptr);
    for (jsize i = 0; i < length; ++i) {
        PidList[i] = elements[i];
    }
    env->ReleaseLongArrayElements(pidlist, elements, 0);

    std::vector<jlong> rss;

    for (jlong pid : PidList) {
        uint64_t hProcess = rwDriver.OpenProcess(static_cast<long>(pid));
        if (!hProcess) { continue; }

        uint64_t outRss = 0;
        if (!rwDriver.GetProcessRSS(hProcess, outRss)) {
            LOGE("Failed to get RSS for PID %ld", pid);
            rwDriver.CloseHandle(hProcess);
            continue;
        }

        rss.push_back(static_cast<jlong>(outRss));
        rwDriver.CloseHandle(hProcess);
    }

    jlongArray resultArray = env->NewLongArray(rss.size());
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        return nullptr;
    }

    env->SetLongArrayRegion(resultArray, 0, rss.size(), rss.data());
    return resultArray;
}

JNIEXPORT jobjectArray JNICALL Java_com_yervant_huntgames_backend_HuntService_getPidCmdLine(JNIEnv *env, jobject, jlongArray pidlist) {
    CMemoryReaderWriter rwDriver;
    int err;
    BOOL b;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
    if (!b) {
        LOGE("Failed to connect to driver");
        return nullptr;
    }

    jsize length = env->GetArrayLength(pidlist);
    std::vector<jlong> PidList(length);

    jlong *elements = env->GetLongArrayElements(pidlist, nullptr);
    for (jsize i = 0; i < length; ++i) {
        PidList[i] = elements[i];
    }
    env->ReleaseLongArrayElements(pidlist, elements, 0);

    jobjectArray cmdLineArray = env->NewObjectArray(length, env->FindClass("java/lang/String"), nullptr);
    if (cmdLineArray == nullptr) {
        LOGE("Failed to create jobjectArray for cmdLine");
        return nullptr;
    }

    for (jsize i = 0; i < length; i++) {
        uint64_t hProcess = rwDriver.OpenProcess(static_cast<long>(PidList[i]));
        if (!hProcess) {
            env->SetObjectArrayElement(cmdLineArray, i, nullptr);
            continue;
        }

        char cmdline[100] = { 0 };
        if (!rwDriver.GetProcessCmdline(hProcess, cmdline, sizeof(cmdline))) {
            LOGE("Failed to get cmdline for PID %ld", PidList[i]);
            rwDriver.CloseHandle(hProcess);
            env->SetObjectArrayElement(cmdLineArray, i, nullptr);
            continue;
        }

        jstring jcmdline = env->NewStringUTF(cmdline);
        if (jcmdline == nullptr) {
            LOGE("Failed to create jstring for cmdline");
            rwDriver.CloseHandle(hProcess);
            env->SetObjectArrayElement(cmdLineArray, i, nullptr);
            continue;
        }

        env->SetObjectArrayElement(cmdLineArray, i, jcmdline);
        env->DeleteLocalRef(jcmdline);
        rwDriver.CloseHandle(hProcess);
    }

    return cmdLineArray;
}


JNIEXPORT jintArray JNICALL Java_com_yervant_huntgames_backend_HuntService_readMultipleInt(JNIEnv *env, jobject, jlongArray addressesArray, jlong pid) {
    BOOL b;
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
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

    jsize length = env->GetArrayLength(addressesArray);
    std::vector<uint64_t> addresses(length);

    jlong *elements = env->GetLongArrayElements(addressesArray, nullptr);
    for (jsize i = 0; i < length; ++i) {
        addresses[i] = static_cast<uint64_t>(elements[i]);
    }
    env->ReleaseLongArrayElements(addressesArray, elements, 0);

    jintArray resultArray = env->NewIntArray(length);
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        rwDriver.CloseHandle(hProcess);
        return nullptr;
    }

    std::vector<jint> valuesArray(length);
    for (size_t i = 0; i < length; ++i) {
        auto res = read_memory<int>(&rwDriver, hProcess, addresses[i]);
        valuesArray[i] = static_cast<jint>(res);
    }

    env->SetIntArrayRegion(resultArray, 0, valuesArray.size(), valuesArray.data());

    rwDriver.CloseHandle(hProcess);
    return resultArray;
}

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_readMultipleLong(JNIEnv *env, jobject, jlongArray addressesArray, jlong pid) {
    BOOL b;
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
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

    jsize length = env->GetArrayLength(addressesArray);
    std::vector<uint64_t> addresses(length);

    jlong *elements = env->GetLongArrayElements(addressesArray, nullptr);
    for (jsize i = 0; i < length; ++i) {
        addresses[i] = static_cast<uint64_t>(elements[i]);
    }
    env->ReleaseLongArrayElements(addressesArray, elements, 0);

    jlongArray resultArray = env->NewLongArray(length);
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        rwDriver.CloseHandle(hProcess);
        return nullptr;
    }

    std::vector<jlong> valuesArray(length);
    for (size_t i = 0; i < length; ++i) {
        auto res = read_memory<long>(&rwDriver, hProcess, addresses[i]);
        valuesArray[i] = static_cast<jlong>(res);
    }

    env->SetLongArrayRegion(resultArray, 0, valuesArray.size(), valuesArray.data());

    rwDriver.CloseHandle(hProcess);
    return resultArray;
}

JNIEXPORT jfloatArray JNICALL Java_com_yervant_huntgames_backend_HuntService_readMultipleFloat(JNIEnv *env, jobject, jlongArray addressesArray, jlong pid) {
    BOOL b;
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
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

    jsize length = env->GetArrayLength(addressesArray);
    std::vector<uint64_t> addresses(length);

    jlong *elements = env->GetLongArrayElements(addressesArray, nullptr);
    for (jsize i = 0; i < length; ++i) {
        addresses[i] = static_cast<uint64_t>(elements[i]);
    }
    env->ReleaseLongArrayElements(addressesArray, elements, 0);

    jfloatArray resultArray = env->NewFloatArray(length);
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        rwDriver.CloseHandle(hProcess);
        return nullptr;
    }

    std::vector<jfloat> valuesArray(length);
    for (size_t i = 0; i < length; ++i) {
        auto res = read_memory<float>(&rwDriver, hProcess, addresses[i]);
        valuesArray[i] = static_cast<jfloat>(res);
    }

    env->SetFloatArrayRegion(resultArray, 0, valuesArray.size(), valuesArray.data());

    rwDriver.CloseHandle(hProcess);
    return resultArray;
}

JNIEXPORT jdoubleArray JNICALL Java_com_yervant_huntgames_backend_HuntService_readMultipleDouble(JNIEnv *env, jobject, jlongArray addressesArray, jlong pid) {
    BOOL b;
    CMemoryReaderWriter rwDriver;
    int err;

    if (getuid() != 0) {
        LOGE("Root access missing");
        return nullptr;
    }
    b = rwDriver.ConnectDriver(RWPROCMEM_FILE_NODE, FALSE, err);
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

    jsize length = env->GetArrayLength(addressesArray);
    std::vector<uint64_t> addresses(length);

    jlong *elements = env->GetLongArrayElements(addressesArray, nullptr);
    for (jsize i = 0; i < length; ++i) {
        addresses[i] = static_cast<uint64_t>(elements[i]);
    }
    env->ReleaseLongArrayElements(addressesArray, elements, 0);

    jdoubleArray resultArray = env->NewDoubleArray(length);
    if (resultArray == nullptr) {
        LOGE("Failed to create jlongArray");
        rwDriver.CloseHandle(hProcess);
        return nullptr;
    }

    std::vector<jdouble> valuesArray(length);
    for (size_t i = 0; i < length; ++i) {
        auto res = read_memory<double>(&rwDriver, hProcess, addresses[i]);
        valuesArray[i] = static_cast<jdouble>(res);
    }

    env->SetDoubleArrayRegion(resultArray, 0, valuesArray.size(), valuesArray.data());

    rwDriver.CloseHandle(hProcess);
    return resultArray;
}

JNIEXPORT void JNICALL Java_com_yervant_huntgames_backend_HuntService_writeMultipleInt(JNIEnv *env, jobject, jlong pid, jlongArray addresses, jint value) {
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

    jsize length = env->GetArrayLength(addresses);
    std::vector<uint64_t> addrs(length);

    jlong *elements = env->GetLongArrayElements(addresses, nullptr);
    for (jsize i = 0; i < length; ++i) {
        addrs[i] = static_cast<uint64_t>(elements[i]);
    }
    env->ReleaseLongArrayElements(addresses, elements, 0);

    for (uint64_t addr : addrs) {
        write_memory<int>(&rwDriver, hProcess, addr, value);
    }
    rwDriver.CloseHandle(hProcess);
}

JNIEXPORT void JNICALL Java_com_yervant_huntgames_backend_HuntService_writeMultipleLong(JNIEnv *env, jobject, jlong pid, jlongArray addresses, jlong value) {
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

    jsize length = env->GetArrayLength(addresses);
    std::vector<uint64_t> addrs(length);

    jlong *elements = env->GetLongArrayElements(addresses, nullptr);
    for (jsize i = 0; i < length; ++i) {
        addrs[i] = static_cast<uint64_t>(elements[i]);
    }
    env->ReleaseLongArrayElements(addresses, elements, 0);

    for (uint64_t addr : addrs) {
        write_memory<long>(&rwDriver, hProcess, addr, value);
    }
    rwDriver.CloseHandle(hProcess);
}

JNIEXPORT void JNICALL Java_com_yervant_huntgames_backend_HuntService_writeMultipleFloat(JNIEnv *env, jobject, jlong pid, jlongArray addresses, jfloat value) {
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

    jsize length = env->GetArrayLength(addresses);
    std::vector<uint64_t> addrs(length);

    jlong *elements = env->GetLongArrayElements(addresses, nullptr);
    for (jsize i = 0; i < length; ++i) {
        addrs[i] = static_cast<uint64_t>(elements[i]);
    }
    env->ReleaseLongArrayElements(addresses, elements, 0);

    for (uint64_t addr : addrs) {
        write_memory<float>(&rwDriver, hProcess, addr, value);
    }
    rwDriver.CloseHandle(hProcess);
}

JNIEXPORT void JNICALL Java_com_yervant_huntgames_backend_HuntService_writeMultipleDouble(JNIEnv *env, jobject, jlong pid, jlongArray addresses, jdouble value) {
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

    jsize length = env->GetArrayLength(addresses);
    std::vector<uint64_t> addrs(length);

    jlong *elements = env->GetLongArrayElements(addresses, nullptr);
    for (jsize i = 0; i < length; ++i) {
        addrs[i] = static_cast<uint64_t>(elements[i]);
    }
    env->ReleaseLongArrayElements(addresses, elements, 0);

    for (uint64_t addr : addrs) {
        write_memory<double>(&rwDriver, hProcess, addr, value);
    }
    rwDriver.CloseHandle(hProcess);
}

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_searchMemoryInt(JNIEnv *env, jobject, jlong pid, jint searchValue, jint searchValue2, jint range, jint scantype, jboolean physicalMemoryOnly) {
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

    RangeType rangeType = getrange(static_cast<int>(range));
    size_t threads = getThreads();
    SCAN_TYPE scanType = getscantype(scantype);
    std::vector<ADDR_RESULT_INFO> results = normal_val_search<int>(&rwDriver, hProcess, threads, static_cast<int>(searchValue), static_cast<int>(searchValue2), rangeType, scanType, physicalMemoryOnly);

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

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_searchMemoryLong(JNIEnv *env, jobject, jlong pid, jlong searchValue, jlong searchValue2, jint range, jint scantype, jboolean physicalMemoryOnly) {
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

    RangeType rangeType = getrange(static_cast<int>(range));
    SCAN_TYPE scanType = getscantype(scantype);
    size_t threads = getThreads();
    std::vector<ADDR_RESULT_INFO> results = normal_val_search<long>(&rwDriver, hProcess, threads, static_cast<long>(searchValue), static_cast<long>(searchValue2), rangeType, scanType, physicalMemoryOnly);

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

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_searchMemoryFloat(JNIEnv *env, jobject, jlong pid, jfloat searchValue, jfloat searchValue2, jint range, jint scantype, jboolean physicalMemoryOnly) {
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

    RangeType rangeType = getrange(static_cast<int>(range));
    size_t threads = getThreads();
    SCAN_TYPE scanType = getscantype(scantype);
    std::vector<ADDR_RESULT_INFO> results = normal_val_search<float>(&rwDriver, hProcess, threads, static_cast<float>(searchValue), static_cast<float>(searchValue2), rangeType, scanType, physicalMemoryOnly);

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

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_searchMemoryDouble(JNIEnv *env, jobject, jlong pid, jdouble searchValue, jdouble searchValue2, jint range, jint scantype, jboolean physicalMemoryOnly) {
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

    RangeType rangeType = getrange(static_cast<int>(range));
    size_t threads = getThreads();
    SCAN_TYPE scanType = getscantype(scantype);
    std::vector<ADDR_RESULT_INFO> results = normal_val_search<double>(&rwDriver, hProcess, threads, static_cast<double>(searchValue), static_cast<double>(searchValue2), rangeType, scanType, physicalMemoryOnly);

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


JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_filterMemoryInt(JNIEnv *env, jobject, jlong pid, jlongArray addressArray, jint filterValue, jint filterValue2) {
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

    std::vector<uint64_t> filteredAddresses = filter_memory<int>(&rwDriver, hProcess, static_cast<int>(filterValue), static_cast<int>(filterValue2), addresses);

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

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_filterMemoryLong(JNIEnv *env, jobject, jlong pid, jlongArray addressArray, jlong filterValue, jlong filterValue2) {
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

    std::vector<uint64_t> filteredAddresses = filter_memory<long>(&rwDriver, hProcess, static_cast<long>(filterValue), static_cast<long>(filterValue2), addresses);

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

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_filterMemoryFloat(JNIEnv *env, jobject, jlong pid, jlongArray addressArray, jfloat filterValue, jfloat filterValue2) {
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

    std::vector<uint64_t> filteredAddresses = filter_memory<float>(&rwDriver, hProcess, static_cast<float>(filterValue), static_cast<float>(filterValue2), addresses);

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

JNIEXPORT jlongArray JNICALL Java_com_yervant_huntgames_backend_HuntService_filterMemoryDouble(JNIEnv *env, jobject, jlong pid, jlongArray addressArray, jdouble filterValue, jdouble filterValue2) {
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

    std::vector<uint64_t> filteredAddresses = filter_memory<double>(&rwDriver, hProcess, static_cast<double>(filterValue), static_cast<double>(filterValue2), addresses);

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

