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

        override fun ireadMemoryInt(address: Long, pid: Long): Int {
            return readMemoryInt(address, pid)
        }

        override fun ireadMemoryLong(address: Long, pid: Long): Long {
            return readMemoryLong(address, pid)
        }

        override fun ireadMemoryFloat(address: Long, pid: Long): Float {
            return readMemoryFloat(address, pid)
        }

        override fun ireadMemoryDouble(address: Long, pid: Long): Double {
            return readMemoryDouble(address, pid)
        }

        override fun iwriteMemoryInt(pid: Long, address: Long, value: Int) {
            writeMemoryInt(pid, address, value)
        }

        override fun iwriteMemoryLong(pid: Long, address: Long, value: Long) {
            writeMemoryLong(pid, address, value)
        }

        override fun iwriteMemoryFloat(pid: Long, address: Long, value: Float) {
            writeMemoryFloat(pid, address, value)
        }

        override fun iwriteMemoryDouble(pid: Long, address: Long, value: Double) {
            writeMemoryDouble(pid, address, value)
        }

        override fun isearchMemoryInt(pid: Long, searchValue: Int, range: Int, physicalMemoryOnly: Boolean): LongArray {
            return searchMemoryInt(pid, searchValue, range, physicalMemoryOnly)
        }

        override fun isearchMemoryLong(pid: Long, searchValue: Long, range: Int, physicalMemoryOnly: Boolean): LongArray {
            return searchMemoryLong(pid, searchValue, range, physicalMemoryOnly)
        }

        override fun isearchMemoryFloat(pid: Long, searchValue: Float, range: Int, physicalMemoryOnly: Boolean): LongArray {
            return searchMemoryFloat(pid, searchValue, range, physicalMemoryOnly)
        }

        override fun isearchMemoryDouble(pid: Long, searchValue: Double, range: Int, physicalMemoryOnly: Boolean): LongArray {
            return searchMemoryDouble(pid, searchValue, range, physicalMemoryOnly)
        }

        override fun ifilterMemoryInt(pid: Long, addressArray: LongArray, filterValue: Int): LongArray {
            return filterMemoryInt(pid, addressArray, filterValue)
        }

        override fun ifilterMemoryLong(pid: Long, addressArray: LongArray, filterValue: Long): LongArray {
            return filterMemoryLong(pid, addressArray, filterValue)
        }

        override fun ifilterMemoryFloat(pid: Long, addressArray: LongArray, filterValue: Float): LongArray {
            return filterMemoryFloat(pid, addressArray, filterValue)
        }

        override fun ifilterMemoryDouble(pid: Long, addressArray: LongArray, filterValue: Double): LongArray {
            return filterMemoryDouble(pid, addressArray, filterValue)
        }

        override fun igetpidtarget(pkgname: String): Long {
            return getpidtarget(pkgname)
        }
    }

    external fun checkrootjni(): Boolean

    external fun getpidtarget(pkgname: String): Long

    external fun readMemoryInt(address: Long, pid: Long): Int

    external fun readMemoryLong(address: Long, pid: Long): Long

    external fun readMemoryFloat(address: Long, pid: Long): Float

    external fun readMemoryDouble(address: Long, pid: Long): Double

    external fun writeMemoryInt(pid: Long, address: Long, value: Int)

    external fun writeMemoryLong(pid: Long, address: Long, value: Long)

    external fun writeMemoryFloat(pid: Long, address: Long, value: Float)

    external fun writeMemoryDouble(pid: Long, address: Long, value: Double)

    external fun searchMemoryInt(pid: Long, searchValue: Int, range: Int, physicalMemoryOnly: Boolean): LongArray

    external fun searchMemoryLong(pid: Long, searchValue: Long, range: Int, physicalMemoryOnly: Boolean): LongArray

    external fun searchMemoryFloat(pid: Long, searchValue: Float, range: Int, physicalMemoryOnly: Boolean): LongArray

    external fun searchMemoryDouble(pid: Long, searchValue: Double, range: Int, physicalMemoryOnly: Boolean): LongArray

    external fun filterMemoryInt(pid: Long, addressArray: LongArray, filterValue: Int): LongArray

    external fun filterMemoryLong(pid: Long, addressArray: LongArray, filterValue: Long): LongArray

    external fun filterMemoryFloat(pid: Long, addressArray: LongArray, filterValue: Float): LongArray

    external fun filterMemoryDouble(pid: Long, addressArray: LongArray, filterValue: Double): LongArray

    companion object {
        init {
            if (Process.myUid() == 0) {
                System.loadLibrary("hunt")
            }
        }
    }
}
