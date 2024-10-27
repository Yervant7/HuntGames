// IHuntService.aidl
package com.yervant.huntgames;

interface IHuntService {

    String executeRootCommand(String command);

    boolean icheckRootJNI();

    long igetpidtarget(String pkgname);

    int ireadMemoryInt(long address, long pid);

    long ireadMemoryLong(long address, long pid);

    float ireadMemoryFloat(long address, long pid);

    double ireadMemoryDouble(long address, long pid);

    void iwriteMemoryInt(long pid, long address, int value);

    void iwriteMemoryLong(long pid, long address, long value);

    void iwriteMemoryFloat(long pid, long address, float value);

    void iwriteMemoryDouble(long pid, long address, double value);

    long[] isearchMemoryInt(long pid, int searchValue, int range, boolean physicalMemoryOnly);

    long[] isearchMemoryLong(long pid, long searchValue, int range, boolean physicalMemoryOnly);

    long[] isearchMemoryFloat(long pid, float searchValue, int range, boolean physicalMemoryOnly);

    long[] isearchMemoryDouble(long pid, double searchValue, int range, boolean physicalMemoryOnly);

    long[] ifilterMemoryInt(long pid, in long[] addressArray, int filterValue);

    long[] ifilterMemoryLong(long pid, in long[] addressArray, long filterValue);

    long[] ifilterMemoryFloat(long pid, in long[] addressArray, float filterValue);

    long[] ifilterMemoryDouble(long pid, in long[] addressArray, double filterValue);
}
