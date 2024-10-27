package com.yervant.huntgames.backend

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import com.yervant.huntgames.IHuntService
import com.yervant.huntgames.ui.menu.regionsselected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class rwProcMem {

    private var huntService: IHuntService? = null

    private var onHuntServiceConnected: (() -> Unit)? = null

    companion object {
        init {
            Shell.enableVerboseLogging
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(10)
            )
        }
    }

    private fun rootCheck(): Boolean {
        val output = executeRootCommand("id")
        if (output.isEmpty()) {
            Log.d("rwProcMem", "APP Not Have Root Access")
        } else if (output.startsWith("uid=0")) {
            Log.d("rwProcMem", "APP Have Root Access")
            return true
        } else {
            Log.d("rwProcMem", "APP Not Have Root Access")
        }
        return false
    }

    private fun checkroot() {

        if (rootCheck() && checkRootJNI()) {
            Log.d("rwProcMem", "Have root Access")
        } else {
            Log.d("rwProcMem", "Failed to set root")
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            huntService = IHuntService.Stub.asInterface(service)
            Log.d("rwProcMem", "RootService connected")

            checkroot()
            onHuntServiceConnected?.invoke()
            onHuntServiceConnected = null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            huntService = null
            Log.d("rwProcMem", "RootService disconnected")
        }
    }

    private fun ensureHuntServiceConnected(overlayContext: OverlayContext, onConnected: () -> Unit) {
        if (huntService != null) {
            onConnected()
        } else {
            Log.d("rwProcMem", "HuntService is not connected. trying reconnect...")
            reconnectToHuntService(overlayContext)
            onHuntServiceConnected = onConnected
        }
    }

    private fun reconnectToHuntService(overlayContext: OverlayContext) {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent(overlayContext.service, HuntService::class.java)
            RootService.bind(intent, serviceConnection)
        }
    }

    private fun executeRootCommand(command: String): String {
        var res = ""
        huntService?.let {
            try {
                res = it.executeRootCommand(command)
                Log.d("rwProcMem", "Command send: $command")
            } catch (e: Exception) {
                Log.e("rwProcMem", "Error sending command to HuntService", e)
            }
        } ?: run {
            Log.e("rwProcMem", "HuntService is not connected")
        }
        return res;
    }

    private fun checkRootJNI(): Boolean {
        var res = false
        huntService?.let {
            try {
                res = it.icheckRootJNI()
            } catch (e: Exception) {
                Log.e("rwProcMem", "check root in jni failed", e)
            }
        } ?: run {
            Log.e("rwProcMem", "HuntService is not connected")
        }
        return res
    }

    fun getpid(pkgname: String, overlayContext: OverlayContext, onResult: (Long) -> Unit) {
        if (pkgname.isNotEmpty()) {
            ensureHuntServiceConnected(overlayContext) {
                huntService?.let {
                    try {
                        val res = it.igetpidtarget(pkgname)
                        onResult(res)
                    } catch (e: Exception) {
                        Log.e("rwProcMem", "get pid target failed", e)
                        onResult(-1L)
                    }
                } ?: run {
                    Log.e("rwProcMem", "HuntService is not connected")
                    onResult(-1L)
                }
            }
        }
    }

    fun searchMemoryInt(pid: Long, searchValue: Int, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    var range = regionsselected
                    if (range == 0) {
                        range = 1000
                    }
                    val res: LongArray = it.isearchMemoryInt(pid, searchValue, range, false)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error searching int", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun searchMemoryLong(pid: Long, searchValue: Long, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    var range = regionsselected
                    if (range == 0) {
                        range = 1000
                    }
                    val res: LongArray = it.isearchMemoryLong(pid, searchValue, range, false)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error searching int", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun searchMemoryFloat(pid: Long, searchValue: Float, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    var range = regionsselected
                    if (range == 0) {
                        range = 1000
                    }
                    val res: LongArray = it.isearchMemoryFloat(pid, searchValue, range, false)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error searching int", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun searchMemoryDouble(pid: Long, searchValue: Double, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    var range = regionsselected
                    if (range == 0) {
                        range = 1000
                    }
                    val res: LongArray = it.isearchMemoryDouble(pid, searchValue, range, false)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error searching int", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun filterMemoryInt(pid: Long, addressArray: LongArray, filterValue: Int, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res: LongArray = it.ifilterMemoryInt(pid, addressArray, filterValue)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error in filter memory", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun filterMemoryLong(pid: Long, addressArray: LongArray, filterValue: Long, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res: LongArray = it.ifilterMemoryLong(pid, addressArray, filterValue)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error in filter memory", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun filterMemoryFloat(pid: Long, addressArray: LongArray, filterValue: Float, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res: LongArray = it.ifilterMemoryFloat(pid, addressArray, filterValue)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error in filter memory", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun filterMemoryDouble(pid: Long, addressArray: LongArray, filterValue: Double, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res: LongArray = it.ifilterMemoryDouble(pid, addressArray, filterValue)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error in filter memory", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun readMemoryInt(address: Long, pid: Long, overlayContext: OverlayContext, onResult: (Int) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res = it.ireadMemoryInt(address, pid)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error reading int", e)
                    onResult(-1)
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(-1)
            }
        }
    }

    fun readMemoryLong(address: Long, pid: Long, overlayContext: OverlayContext, onResult: (Long) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res = it.ireadMemoryLong(address, pid)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error reading int", e)
                    onResult(-1L)
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(-1L)
            }
        }
    }

    fun readMemoryFloat(address: Long, pid: Long, overlayContext: OverlayContext, onResult: (Float) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res = it.ireadMemoryFloat(address, pid)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error reading int", e)
                    onResult(0.0f)
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(0.0f)
            }
        }
    }

    fun readMemoryDouble(address: Long, pid: Long, overlayContext: OverlayContext, onResult: (Double) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res = it.ireadMemoryDouble(address, pid)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error reading int", e)
                    onResult(0.0)
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(0.0)
            }
        }
    }

    fun writeMemoryInt(pid: Long, address: Long, value: Int, overlayContext: OverlayContext, onComplete: () -> Unit = {}) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    it.iwriteMemoryInt(pid, address, value)
                    onComplete()
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error writing int", e)
                    onComplete()
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onComplete()
            }
        }
    }

    fun writeMemoryLong(pid: Long, address: Long, value: Long, overlayContext: OverlayContext, onComplete: () -> Unit = {}) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    it.iwriteMemoryLong(pid, address, value)
                    onComplete()
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error writing int", e)
                    onComplete()
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onComplete()
            }
        }
    }

    fun writeMemoryFloat(pid: Long, address: Long, value: Float, overlayContext: OverlayContext, onComplete: () -> Unit = {}) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    it.iwriteMemoryFloat(pid, address, value)
                    onComplete()
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error writing int", e)
                    onComplete()
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onComplete()
            }
        }
    }

    fun writeMemoryDouble(pid: Long, address: Long, value: Double, overlayContext: OverlayContext, onComplete: () -> Unit = {}) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    it.iwriteMemoryDouble(pid, address, value)
                    onComplete()
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error writing int", e)
                    onComplete()
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onComplete()
            }
        }
    }
}