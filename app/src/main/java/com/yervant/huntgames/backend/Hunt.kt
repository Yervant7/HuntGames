package com.yervant.huntgames.backend

import android.content.Context
import com.yervant.huntgames.ui.menu.AddressInfo
import com.yervant.huntgames.ui.menu.isattached
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import android.util.Log

class Hunt {

    private var freezeJob: Job? = null

    private val isFreezing = AtomicBoolean(false)

    suspend fun writeall(addrs: MutableList<AddressInfo>, value: String, context: Context) {
        val pid = isattached().currentPid()
        val hgmem = HGMem()

        addrs.forEach { addressInfo ->
            var skip = false
            when (addressInfo.matchInfo.valuetype.lowercase()) {
                "int" -> value.toIntOrNull()
                    ?: {
                        skip = true
                    }
                "long" -> value.toLongOrNull()
                    ?: {
                        skip = true
                    }
                "float" -> value.toFloatOrNull()
                    ?: {
                        skip = true
                    }
                "double" -> value.toDoubleOrNull()
                    ?: {
                        skip = true
                    }
            }
            if (!skip) {
                hgmem.writeMem(
                    pid = pid,
                    address = addressInfo.matchInfo.address,
                    datatype = addressInfo.matchInfo.valuetype,
                    value = value,
                    context = context
                )
            }
        }
    }

    fun unfreezeall() {
        freezeJob?.cancel()
        isFreezing.set(false)
    }

    suspend fun freezeall(
        addrs: MutableList<AddressInfo>,
        value: String,
        context: Context,
        intervalMs: Long = 100
    ) {
        unfreezeall()

        val pid = isattached().currentPid()
        val hgmem = HGMem()

        isFreezing.set(true)

        freezeJob = CoroutineScope(Dispatchers.IO).launch {
            while (isFreezing.get() && isActive) {
                try {
                    addrs.forEach { addressInfo ->
                        var skip = false
                        when (addressInfo.matchInfo.valuetype.lowercase()) {
                            "int" -> value.toIntOrNull()
                                ?: {
                                    skip = true
                                }
                            "long" -> value.toLongOrNull()
                                ?: {
                                    skip = true
                                }
                            "float" -> value.toFloatOrNull()
                                ?: {
                                    skip = true
                                }
                            "double" -> value.toDoubleOrNull()
                                ?: {
                                    skip = true
                                }
                        }
                        if (!skip) {
                            hgmem.writeMem(
                                pid = pid,
                                address = addressInfo.matchInfo.address,
                                datatype = addressInfo.matchInfo.valuetype,
                                value = value,
                                context = context
                            )
                        }
                    }

                    delay(intervalMs)
                } catch (e: Exception) {
                    Log.e("FreezeAll", "Error in freeze loop", e)

                    if (e is CancellationException) {
                        break
                    }
                }
            }
        }
    }
}