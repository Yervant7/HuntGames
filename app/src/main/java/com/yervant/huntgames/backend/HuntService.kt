package com.yervant.huntgames.backend

import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import com.yervant.huntgames.IHuntService

class HuntService : RootService() {

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private val binder = object : IHuntService.Stub() {
        override fun executeRootCommand(command: String): String {
            try {
                var res: String
                val process = Runtime.getRuntime().exec(command)
                process.inputStream.bufferedReader().use { reader ->
                    res = reader.readText()
                }
                process.waitFor()

                val exitValue = process.exitValue()
                Log.d("HuntService", "Executed command with exit code: $exitValue")
                return res
            } catch (e: Exception) {
                Log.e("HuntService", "Error executing command: $command ", e)
            }
            return ""
        }

        override fun icheckRootJNI(): Boolean {
            return checkrootjni()
        }

        override fun ireadMultipleInt(addresses: LongArray, pid: Long): IntArray {
            return readMultipleInt(addresses, pid)
        }

        override fun ireadMultipleLong(addresses: LongArray, pid: Long): LongArray {
            return readMultipleLong(addresses, pid)
        }

        override fun ireadMultipleFloat(addresses: LongArray, pid: Long): FloatArray {
            return readMultipleFloat(addresses, pid)
        }

        override fun ireadMultipleDouble(addresses: LongArray, pid: Long): DoubleArray {
            return readMultipleDouble(addresses, pid)
        }

        override fun iwriteMultipleInt(pid: Long, addresses: LongArray, value: Int) {
            writeMultipleInt(pid, addresses, value)
        }

        override fun iwriteMultipleLong(pid: Long, addresses: LongArray, value: Long) {
            writeMultipleLong(pid, addresses, value)
        }

        override fun iwriteMultipleFloat(pid: Long, addresses: LongArray, value: Float) {
            writeMultipleFloat(pid, addresses, value)
        }

        override fun iwriteMultipleDouble(pid: Long, addresses: LongArray, value: Double) {
            writeMultipleDouble(pid, addresses, value)
        }

        override fun isearchMemoryInt(pid: Long, searchValue: Int, searchValue2: Int, range: Int, scantype: Int, physicalMemoryOnly: Boolean): LongArray {
            return searchMemoryInt(pid, searchValue, searchValue2, range, scantype, physicalMemoryOnly)
        }

        override fun isearchMemoryLong(pid: Long, searchValue: Long, searchValue2: Long, range: Int, scantype: Int, physicalMemoryOnly: Boolean): LongArray {
            return searchMemoryLong(pid, searchValue, searchValue2, range, scantype, physicalMemoryOnly)
        }

        override fun isearchMemoryFloat(pid: Long, searchValue: Float, searchValue2: Float, range: Int, scantype: Int, physicalMemoryOnly: Boolean): LongArray {
            return searchMemoryFloat(pid, searchValue, searchValue2, range, scantype, physicalMemoryOnly)
        }

        override fun isearchMemoryDouble(pid: Long, searchValue: Double, searchValue2: Double, range: Int, scantype: Int, physicalMemoryOnly: Boolean): LongArray {
            return searchMemoryDouble(pid, searchValue, searchValue2, range, scantype, physicalMemoryOnly)
        }

        override fun ifilterMemoryInt(pid: Long, addressArray: LongArray, filterValue: Int, filterValue2: Int): LongArray {
            return filterMemoryInt(pid, addressArray, filterValue, filterValue2)
        }

        override fun ifilterMemoryLong(pid: Long, addressArray: LongArray, filterValue: Long, filterValue2: Long): LongArray {
            return filterMemoryLong(pid, addressArray, filterValue, filterValue2)
        }

        override fun ifilterMemoryFloat(pid: Long, addressArray: LongArray, filterValue: Float, filterValue2: Float): LongArray {
            return filterMemoryFloat(pid, addressArray, filterValue, filterValue2)
        }

        override fun ifilterMemoryDouble(pid: Long, addressArray: LongArray, filterValue: Double, filterValue2: Double): LongArray {
            return filterMemoryDouble(pid, addressArray, filterValue, filterValue2)
        }
    }

    external fun checkrootjni(): Boolean

    external fun readMultipleInt(address: LongArray, pid: Long): IntArray

    external fun readMultipleLong(address: LongArray, pid: Long): LongArray

    external fun readMultipleFloat(address: LongArray, pid: Long): FloatArray

    external fun readMultipleDouble(address: LongArray, pid: Long): DoubleArray

    external fun writeMultipleInt(pid: Long, address: LongArray, value: Int)

    external fun writeMultipleLong(pid: Long, address: LongArray, value: Long)

    external fun writeMultipleFloat(pid: Long, address: LongArray, value: Float)

    external fun writeMultipleDouble(pid: Long, address: LongArray, value: Double)

    external fun searchMemoryInt(pid: Long, searchValue: Int, searchValue2: Int, range: Int, scantype: Int, physicalMemoryOnly: Boolean): LongArray

    external fun searchMemoryLong(pid: Long, searchValue: Long, searchValue2: Long, range: Int, scantype: Int, physicalMemoryOnly: Boolean): LongArray

    external fun searchMemoryFloat(pid: Long, searchValue: Float, searchValue2: Float, range: Int, scantype: Int, physicalMemoryOnly: Boolean): LongArray

    external fun searchMemoryDouble(pid: Long, searchValue: Double, searchValue2: Double, range: Int, scantype: Int, physicalMemoryOnly: Boolean): LongArray

    external fun filterMemoryInt(pid: Long, addressArray: LongArray, filterValue: Int, filterValue2: Int): LongArray

    external fun filterMemoryLong(pid: Long, addressArray: LongArray, filterValue: Long, filterValue2: Long): LongArray

    external fun filterMemoryFloat(pid: Long, addressArray: LongArray, filterValue: Float, filterValue2: Float): LongArray

    external fun filterMemoryDouble(pid: Long, addressArray: LongArray, filterValue: Double, filterValue2: Double): LongArray

    companion object {
        init {
            if (Process.myUid() == 0) {
                System.loadLibrary("hunt")
            }
        }
    }
}
