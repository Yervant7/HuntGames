// IHuntService.aidl
package com.yervant.huntgames;

interface IHuntService {

    String executeRootCommand(String command);

    boolean icheckRootJNI();

    int[] ireadMultipleInt(in long[] address, long pid);

    long[] ireadMultipleLong(in long[] address, long pid);

    float[] ireadMultipleFloat(in long[] address, long pid);

    double[] ireadMultipleDouble(in long[] address, long pid);

    void iwriteMultipleInt(long pid, in long[] address, int value);

    void iwriteMultipleLong(long pid, in long[] address, long value);

    void iwriteMultipleFloat(long pid, in long[] address, float value);

    void iwriteMultipleDouble(long pid, in long[] address, double value);

    long[] isearchMemoryInt(long pid, int searchValue, int searchValue2, int range, int scantype, boolean physicalMemoryOnly);

    long[] isearchMemoryLong(long pid, long searchValue, long searchValue2, int range, int scantype, boolean physicalMemoryOnly);

    long[] isearchMemoryFloat(long pid, float searchValue, float searchValue2, int range, int scantype, boolean physicalMemoryOnly);

    long[] isearchMemoryDouble(long pid, double searchValue, double searchValue2, int range, int scantype, boolean physicalMemoryOnly);

    long[] isearchMultiInt(long pid, in int[] searchValues, int range, long distance, boolean physicalMemoryOnly);

    long[] isearchMultiLong(long pid, in long[] searchValues, int range, long distance, boolean physicalMemoryOnly);

    long[] isearchMultiFloat(long pid, in float[] searchValues, int range, long distance, boolean physicalMemoryOnly);

    long[] isearchMultiDouble(long pid, in double[] searchValues, int range, long distance, boolean physicalMemoryOnly);

    long[] ifilterMemoryInt(long pid, in long[] addressArray, int filterValue, int filterValue2, int scantype);

    long[] ifilterMemoryLong(long pid, in long[] addressArray, long filterValue, long filterValue2, int scantype);

    long[] ifilterMemoryFloat(long pid, in long[] addressArray, float filterValue, float filterValue2, int scantype);

    long[] ifilterMemoryDouble(long pid, in long[] addressArray, double filterValue, double filterValue2, int scantype);
}
