package com.yervant.huntgames.backend

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import com.yervant.huntgames.IHuntService
import com.yervant.huntgames.ui.menu.RegionSelected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class rwMem {

    private var huntService: IHuntService? = null

    private var onHuntServiceConnected: (() -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            huntService = IHuntService.Stub.asInterface(service)
            Log.d("rwMem", "HuntService connected")

            onHuntServiceConnected?.invoke()
            onHuntServiceConnected = null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            huntService = null
            Log.d("rwMem", "HuntService disconnected")
        }
    }

    private fun ensureHuntServiceConnected(context: Context, onConnected: () -> Unit) {
        if (huntService != null) {
            onConnected()
        } else {
            Log.d("rwMem", "HuntService is not connected. trying reconnect...")
            reconnectToHuntService(context)
            onHuntServiceConnected = onConnected
        }
    }

    private fun reconnectToHuntService(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent(context, HuntService::class.java)
            RootService.bind(intent, serviceConnection)
        }
    }

    fun searchvalues(pid: Long, datatype: String, searchValue: String, searchValue2: String, scantype: Int, context: Context, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(context) {
            huntService?.let {
                try {
                    val regions = RegionSelected()
                    val res: LongArray = it.isearchvalues(pid, datatype, searchValue, searchValue2, scantype, regions)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwMem", "Error searching memory", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun filtervalues(pid: Long, datatype: String, addressArray: LongArray, filterValue: String, filterValue2: String, scantype: Int, context: Context, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(context) {
            huntService?.let {
                try {
                    val res: LongArray = it.ifiltervalues(pid, datatype, filterValue, filterValue2, scantype, addressArray)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwMem", "Error in filter memory", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun searchgroupvalues(pid: Long, datatype: String, searchValues: Array<String>, proxi: Long, context: Context, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(context) {
            huntService?.let {
                try {
                    val regions = RegionSelected()
                    val res: LongArray = it.isearchgroupvalues(pid, datatype, searchValues, proxi, regions)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwMem", "Error searching memory", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun filtergroupvalues(pid: Long, datatype: String, addressArray: LongArray, filterValues: Array<String>, context: Context, onResult: (LongArray) -> Unit) {
        ensureHuntServiceConnected(context) {
            huntService?.let {
                try {
                    val res: LongArray = it.ifiltergroupvalues(pid, datatype, filterValues, addressArray)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwMem", "Error in filter memory", e)
                    onResult(longArrayOf())
                }
            } ?: run {
                Log.e("rwMem", "HuntService is not connected")
                onResult(longArrayOf())
            }
        }
    }

    fun readmultiple(addresses: LongArray, pid: Long, datatype: String, context: Context, onResult: (Array<String>) -> Unit) {
        ensureHuntServiceConnected(context) {
            huntService?.let {
                try {
                    val res = it.ireadmultiple(addresses, pid, datatype)
                    onResult(res)
                } catch (e: Exception) {
                    Log.e("rwMem", "Error reading", e)
                    onResult(arrayOf())
                }
            } ?: run {
                Log.e("rwMem", "HuntService is not connected")
                onResult(arrayOf())
            }
        }
    }

    fun writemultiple(pid: Long, addresses: LongArray, datatype: String, value: String, context: Context, onComplete: () -> Unit = {}) {
        ensureHuntServiceConnected(context) {
            huntService?.let {
                try {
                    it.iwritemultiple(addresses, pid, datatype, value)
                    onComplete()
                } catch (e: Exception) {
                    Log.e("rwMem", "Error writing", e)
                    onComplete()
                }
            } ?: run {
                Log.e("rwMem", "HuntService is not connected")
                onComplete()
            }
        }
    }

    fun freeze(pid: Long, addresses: LongArray, datatype: String, value: String, context: Context, onComplete: () -> Unit = {}) {
        ensureHuntServiceConnected(context) {
            huntService?.let {
                try {
                    it.istartFreezeExecution(addresses, pid, datatype, value)
                    onComplete()
                } catch (e: Exception) {
                    Log.e("rwMem", "Error freezing", e)
                    onComplete()
                }
            } ?: run {
                Log.e("rwMem", "HuntService is not connected")
                onComplete()
            }
        }
    }

    fun stopFreeze(context: Context) {
        ensureHuntServiceConnected(context) {
            huntService?.let {
                try {
                    it.istopFreezeExecution()
                } catch (e: Exception) {
                    Log.e("rwMem", "Error stopping freeze", e)
                }
            } ?: run {
                Log.e("rwMem", "HuntService is not connected")
            }
        }
    }
}