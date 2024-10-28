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

    fun searchMemoryInt(pid: Long, searchValue: Int, searchValue2: Int, scantype: Int, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    var range = regionsselected
                    if (range == 0) {
                        range = 1
                    }
                    val res: LongArray = it.isearchMemoryInt(pid, searchValue, searchValue2, range, scantype, false)
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

    fun searchMemoryLong(pid: Long, searchValue: Long, searchValue2: Long, scantype: Int, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    var range = regionsselected
                    if (range == 0) {
                        range = 1
                    }
                    val res: LongArray = it.isearchMemoryLong(pid, searchValue, searchValue2, range, scantype, false)
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

    fun searchMemoryFloat(pid: Long, searchValue: Float, searchValue2: Float, scantype: Int, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    var range = regionsselected
                    if (range == 0) {
                        range = 1
                    }
                    val res: LongArray = it.isearchMemoryFloat(pid, searchValue, searchValue2, range, scantype, false)
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

    fun searchMemoryDouble(pid: Long, searchValue: Double, searchValue2: Double, scantype: Int, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    var range = regionsselected
                    if (range == 0) {
                        range = 1
                    }
                    val res: LongArray = it.isearchMemoryDouble(pid, searchValue, searchValue2, range, scantype, false)
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

    fun filterMemoryInt(pid: Long, addressArray: LongArray, filterValue: Int, filterValue2: Int, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res: LongArray = it.ifilterMemoryInt(pid, addressArray, filterValue, filterValue2)
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

    fun filterMemoryLong(pid: Long, addressArray: LongArray, filterValue: Long, filterValue2: Long, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res: LongArray = it.ifilterMemoryLong(pid, addressArray, filterValue, filterValue2)
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

    fun filterMemoryFloat(pid: Long, addressArray: LongArray, filterValue: Float, filterValue2: Float, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res: LongArray = it.ifilterMemoryFloat(pid, addressArray, filterValue, filterValue2)
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

    fun filterMemoryDouble(pid: Long, addressArray: LongArray, filterValue: Double, filterValue2: Double, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res: LongArray = it.ifilterMemoryDouble(pid, addressArray, filterValue, filterValue2)
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

    fun readMultipleInt(addresses: LongArray, pid: Long, overlayContext: OverlayContext, onResult: (IntArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res = it.ireadMultipleInt(addresses, pid)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error reading int", e)
                    onResult(intArrayOf())
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(intArrayOf())
            }
        }
    }

    fun readMultipleLong(addresses: LongArray, pid: Long, overlayContext: OverlayContext, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res = it.ireadMultipleLong(addresses, pid)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error reading int", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun readMultipleFloat(addresses: LongArray, pid: Long, overlayContext: OverlayContext, onResult: (FloatArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res = it.ireadMultipleFloat(addresses, pid)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error reading int", e)
                    onResult(floatArrayOf())
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(floatArrayOf())
            }
        }
    }

    fun readMultipleDouble(addresses: LongArray, pid: Long, overlayContext: OverlayContext, onResult: (DoubleArray) -> Unit) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    val res = it.ireadMultipleDouble(addresses, pid)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwProcMem", "Error reading int", e)
                    onResult(doubleArrayOf())
                }
            } ?: run {
                Log.e("rwProcMem", "HuntService is not connected")
                onResult(doubleArrayOf())
            }
        }
    }

    fun writeMultipleInt(pid: Long, addresses: LongArray, value: Int, overlayContext: OverlayContext, onComplete: () -> Unit = {}) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    it.iwriteMultipleInt(pid, addresses, value)
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

    fun writeMultipleLong(pid: Long, addresses: LongArray, value: Long, overlayContext: OverlayContext, onComplete: () -> Unit = {}) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    it.iwriteMultipleLong(pid, addresses, value)
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

    fun writeMultipleFloat(pid: Long, addresses: LongArray, value: Float, overlayContext: OverlayContext, onComplete: () -> Unit = {}) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    it.iwriteMultipleFloat(pid, addresses, value)
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

    fun writeMultipleDouble(pid: Long, addresses: LongArray, value: Double, overlayContext: OverlayContext, onComplete: () -> Unit = {}) {
        ensureHuntServiceConnected(overlayContext) {
            huntService?.let {
                try {
                    it.iwriteMultipleDouble(pid, addresses, value)
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