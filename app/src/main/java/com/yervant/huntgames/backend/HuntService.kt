package com.yervant.huntgames.backend

import android.content.Intent
import android.os.IBinder
import android.os.Process
import com.topjohnwu.superuser.ipc.RootService
import com.yervant.huntgames.IHuntService

class HuntService : RootService() {

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private val binder = object : IHuntService.Stub() {
        override fun ireadmultiple(addresses: LongArray, pid: Long, datatype: String): Array<String> {
            return readmultiple(addresses, pid, datatype)
        }

        override fun iwritemultiple(addresses: LongArray, pid: Long, datatype: String, value: String) {
            writemultiple(addresses, pid, datatype, value)
        }

        override fun isearchvalues(pid: Long, datatype: String, value1: String, value2: String, scantype: Int, regions: String): LongArray {
            return searchvalues(pid, datatype, value1, value2, scantype, regions)
        }

        override fun ifiltervalues(pid: Long, datatype: String, value1: String, value2: String, scantype: Int, addresses: LongArray): LongArray {
            return filtervalues(pid, datatype, value1, value2, scantype, addresses)
        }

        override fun isearchgroupvalues(pid: Long, datatype: String, values: Array<String>, proxi: Long, regions: String): LongArray {
            return searchgroupvalues(pid, datatype, values, proxi, regions)
        }

        override fun ifiltergroupvalues(pid: Long, datatype: String, values: Array<String>, addresses: LongArray): LongArray {
            return filtergroupvalues(pid, datatype, values, addresses)
        }
    }

    external fun readmultiple(addresses: LongArray, pid: Long, datatype: String): Array<String>

    external fun writemultiple(addresses: LongArray, pid: Long, datatype: String, value: String)

    external fun searchvalues(pid: Long, datatype: String, value1: String, value2: String, scantype: Int, regions: String): LongArray

    external fun filtervalues(pid: Long, datatype: String, value1: String, value2: String, scantype: Int, addresses: LongArray): LongArray

    external fun searchgroupvalues(pid: Long, datatype: String, values: Array<String>, proxi: Long, regions: String): LongArray

    external fun filtergroupvalues(pid: Long, datatype: String, values: Array<String>, addresses: LongArray): LongArray

    companion object {
        init {
            if (Process.myUid() == 0) {
                System.loadLibrary("rwmem")
            }
        }
    }
}
