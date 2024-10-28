package com.yervant.huntgames.backend

import android.widget.Toast
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.yervant.huntgames.backend.Memory.Companion.matches
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class HuntingMemory {

    suspend fun searchInt(pid: Long, targetValue: Int, targetValue2: Int, scantype: Int, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.searchMemoryInt(pid, targetValue, targetValue2, scantype, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(
                        overlayContext.service,
                        "Success in Search for int",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun searchLong(pid: Long, targetValue: Long, targetValue2: Long, scantype: Int, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.searchMemoryLong(pid, targetValue, targetValue2, scantype, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(
                        overlayContext.service,
                        "Success in Search for int",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun searchFloat(pid: Long, targetValue: Float, targetValue2: Float, scantype: Int, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.searchMemoryFloat(pid, targetValue, targetValue2, scantype, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(
                        overlayContext.service,
                        "Success in Search for int",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun searchDouble(pid: Long, targetValue: Double, targetValue2: Double, scantype: Int, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.searchMemoryDouble(pid, targetValue, targetValue2, scantype,overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(
                        overlayContext.service,
                        "Success in Search for int",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun readMultiInt(pid: Long, addr: LongArray, overlayContext: OverlayContext): IntArray {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.readMultipleInt(addr, pid, overlayContext) { onResult ->
                continuation.resume(onResult)
            }
        }
    }

    suspend fun readMultiLong(pid: Long, addr: LongArray, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.readMultipleLong(addr, pid, overlayContext) { onResult ->
                continuation.resume(onResult)
            }
        }
    }

    suspend fun readMultiFloat(pid: Long, addr: LongArray, overlayContext: OverlayContext): FloatArray {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.readMultipleFloat(addr, pid, overlayContext) { onResult ->
                continuation.resume(onResult)
            }
        }
    }

    suspend fun readMultiDouble(pid: Long, addr: LongArray, overlayContext: OverlayContext): DoubleArray {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.readMultipleDouble(addr, pid, overlayContext) { onResult ->
                continuation.resume(onResult)
            }
        }
    }

    fun writeMemInt(pid: Long, addr: LongArray, value: Int, overlayContext: OverlayContext) {
        val rwmem = rwProcMem()
        rwmem.writeMultipleInt(pid, addr, value, overlayContext)
    }

    fun writeMemLong(pid: Long, addr: LongArray, value: Long, overlayContext: OverlayContext) {
        val rwmem = rwProcMem()
        rwmem.writeMultipleLong(pid, addr, value, overlayContext)
    }

    fun writeMemFloat(pid: Long, addr: LongArray, value: Float, overlayContext: OverlayContext) {
        val rwmem = rwProcMem()
        rwmem.writeMultipleFloat(pid, addr, value, overlayContext)
    }

    fun writeMemDouble(pid: Long, addr: LongArray, value: Double, overlayContext: OverlayContext) {
        val rwmem = rwProcMem()
        rwmem.writeMultipleDouble(pid, addr, value, overlayContext)
    }

    suspend fun filterMemInt(pid: Long, expectedValue: Int, expectedValue2: Int, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        val targetlist: MutableList<Long> = mutableListOf()
        var i = 0
        while (i < matches.size) {
            targetlist.add(matches[i].address)
            i++
        }
        return suspendCancellableCoroutine { continuation ->
            rwmem.filterMemoryInt(pid, targetlist.toLongArray(), expectedValue, expectedValue2, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(overlayContext.service, "Success in Filter int", Toast.LENGTH_SHORT).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun filterMemLong(pid: Long, expectedValue: Long, expectedValue2: Long, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        val targetlist: MutableList<Long> = mutableListOf()
        var i = 0
        while (i < matches.size) {
            targetlist.add(matches[i].address)
            i++
        }
        return suspendCancellableCoroutine { continuation ->
            rwmem.filterMemoryLong(pid, targetlist.toLongArray(), expectedValue, expectedValue2, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(overlayContext.service, "Success in Filter int", Toast.LENGTH_SHORT).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun filterMemFloat(pid: Long, expectedValue: Float, expectedValue2: Float, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        val targetlist: MutableList<Long> = mutableListOf()
        var i = 0
        while (i < matches.size) {
            targetlist.add(matches[i].address)
            i++
        }
        return suspendCancellableCoroutine { continuation ->
            rwmem.filterMemoryFloat(pid, targetlist.toLongArray(), expectedValue, expectedValue2, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(overlayContext.service, "Success in Filter int", Toast.LENGTH_SHORT).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun filterMemDouble(pid: Long, expectedValue: Double, expectedValue2: Double, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        val targetlist: MutableList<Long> = mutableListOf()
        var i = 0
        while (i < matches.size) {
            targetlist.add(matches[i].address)
            i++
        }
        return suspendCancellableCoroutine { continuation ->
            rwmem.filterMemoryDouble(pid, targetlist.toLongArray(), expectedValue, expectedValue2, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(overlayContext.service, "Success in Filter int", Toast.LENGTH_SHORT).show()
                }
                continuation.resume(onResult)
            }
        }
    }
}
