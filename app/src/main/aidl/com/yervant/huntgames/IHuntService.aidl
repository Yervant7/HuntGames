// IHuntService.aidl
package com.yervant.huntgames;

interface IHuntService {

    String[] ireadmultiple(in long[] addresses, long pid, String datatype);

    void iwritemultiple(in long[] addresses, long pid, String datatype, String value);

    void istartFreezeExecution(in long[] addresses, long pid, String datatype, String value);

    void istopFreezeExecution();

    long[] isearchvalues(long pid, String datatype, String value1, String value2, int scantype, String regions);

    long[] ifiltervalues(long pid, String datatype, String value1, String value2, int scantype, in long[] addressArray);

    long[] isearchgroupvalues(long pid, String datatype, in String[] values, long proxi, String regions);

    long[] ifiltergroupvalues(long pid, String datatype, in String[] values, in long[] addressArray);
}
