package com.yervant.huntgames.backend

import android.widget.Toast
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.yervant.huntgames.backend.Memory.Companion.matches
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class HuntingMemory {

    suspend fun searchInt(pid: Long, targetValue: Int, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.searchMemoryInt(pid, targetValue, overlayContext) { onResult ->
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

    suspend fun searchLong(pid: Long, targetValue: Long, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.searchMemoryLong(pid, targetValue, overlayContext) { onResult ->
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

    suspend fun searchFloat(pid: Long, targetValue: Float, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.searchMemoryFloat(pid, targetValue, overlayContext) { onResult ->
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

    suspend fun searchDouble(pid: Long, targetValue: Double, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.searchMemoryDouble(pid, targetValue, overlayContext) { onResult ->
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

    suspend fun readMemInt(pid: Long, addr: Long, overlayContext: OverlayContext): Int {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.readMemoryInt(addr, pid, overlayContext) { onResult ->
                continuation.resume(onResult)
            }
        }
    }

    suspend fun readMemLong(pid: Long, addr: Long, overlayContext: OverlayContext): Long {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.readMemoryLong(addr, pid, overlayContext) { onResult ->
                continuation.resume(onResult)
            }
        }
    }

    suspend fun readMemFloat(pid: Long, addr: Long, overlayContext: OverlayContext): Float {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.readMemoryFloat(addr, pid, overlayContext) { onResult ->
                continuation.resume(onResult)
            }
        }
    }

    suspend fun readMemDouble(pid: Long, addr: Long, overlayContext: OverlayContext): Double {
        val rwmem = rwProcMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.readMemoryDouble(addr, pid, overlayContext) { onResult ->
                continuation.resume(onResult)
            }
        }
    }

    fun writeMemInt(pid: Long, addr: Long, value: Int, overlayContext: OverlayContext) {
        val rwmem = rwProcMem()
        rwmem.writeMemoryInt(pid, addr, value, overlayContext)
    }

    fun writeMemLong(pid: Long, addr: Long, value: Long, overlayContext: OverlayContext) {
        val rwmem = rwProcMem()
        rwmem.writeMemoryLong(pid, addr, value, overlayContext)
    }

    fun writeMemFloat(pid: Long, addr: Long, value: Float, overlayContext: OverlayContext) {
        val rwmem = rwProcMem()
        rwmem.writeMemoryFloat(pid, addr, value, overlayContext)
    }

    fun writeMemDouble(pid: Long, addr: Long, value: Double, overlayContext: OverlayContext) {
        val rwmem = rwProcMem()
        rwmem.writeMemoryDouble(pid, addr, value, overlayContext)
    }

    suspend fun filterMemInt(pid: Long, expectedValue: Int, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        val targetlist: MutableList<Long> = mutableListOf()
        var i = 0
        while (i < matches.size) {
            targetlist.add(matches[i].address)
            i++
        }
        return suspendCancellableCoroutine { continuation ->
            rwmem.filterMemoryInt(pid, targetlist.toLongArray(), expectedValue, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(overlayContext.service, "Success in Filter int", Toast.LENGTH_SHORT).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun filterMemLong(pid: Long, expectedValue: Long, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        val targetlist: MutableList<Long> = mutableListOf()
        var i = 0
        while (i < matches.size) {
            targetlist.add(matches[i].address)
            i++
        }
        return suspendCancellableCoroutine { continuation ->
            rwmem.filterMemoryLong(pid, targetlist.toLongArray(), expectedValue, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(overlayContext.service, "Success in Filter int", Toast.LENGTH_SHORT).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun filterMemFloat(pid: Long, expectedValue: Float, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        val targetlist: MutableList<Long> = mutableListOf()
        var i = 0
        while (i < matches.size) {
            targetlist.add(matches[i].address)
            i++
        }
        return suspendCancellableCoroutine { continuation ->
            rwmem.filterMemoryFloat(pid, targetlist.toLongArray(), expectedValue, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(overlayContext.service, "Success in Filter int", Toast.LENGTH_SHORT).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun filterMemDouble(pid: Long, expectedValue: Double, overlayContext: OverlayContext): LongArray {
        val rwmem = rwProcMem()
        val targetlist: MutableList<Long> = mutableListOf()
        var i = 0
        while (i < matches.size) {
            targetlist.add(matches[i].address)
            i++
        }
        return suspendCancellableCoroutine { continuation ->
            rwmem.filterMemoryDouble(pid, targetlist.toLongArray(), expectedValue, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(overlayContext.service, "Success in Filter int", Toast.LENGTH_SHORT).show()
                }
                continuation.resume(onResult)
            }
        }
    }
}
