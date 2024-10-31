package com.yervant.huntgames.backend

import android.widget.Toast
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.yervant.huntgames.backend.Memory.Companion.matches
import com.yervant.huntgames.ui.menu.MatchInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class HuntingMemory {

    suspend fun searchmem(pid: Long, datatype: String, targetValue: String, targetValue2: String, scantype: Int, overlayContext: OverlayContext): LongArray {
        val rwmem = rwMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.searchvalues(pid, datatype, targetValue, targetValue2, scantype, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(
                        overlayContext.service,
                        "Success in Search",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun searchmemgroup(pid: Long, datatype: String, targetValues: Array<String>, proxi: Long, overlayContext: OverlayContext): LongArray {
        val rwmem = rwMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.searchgroupvalues(pid, datatype, targetValues, proxi, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(
                        overlayContext.service,
                        "Success in Search",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun readmem(pid: Long, addr: LongArray, datatype: String, overlayContext: OverlayContext): Array<String> {
        val rwmem = rwMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.readmultiple(addr, pid, datatype, overlayContext) { onResult ->
                continuation.resume(onResult)
            }
        }
    }

    fun writemem(pid: Long, addr: LongArray, datatype: String, value: String, overlayContext: OverlayContext) {
        val rwmem = rwMem()
        rwmem.writemultiple(pid, addr, datatype, value, overlayContext)
    }

    suspend fun filtermem(pid: Long, datatype: String, expectedValue: String, expectedValue2: String, scantype: Int, tMatches: List<MatchInfo>, overlayContext: OverlayContext): LongArray {
        val rwmem = rwMem()
        val targetlist: MutableList<Long> = mutableListOf()
        var i = 0
        while (i < tMatches.size) {
            targetlist.add(tMatches[i].address)
            i++
        }
        return suspendCancellableCoroutine { continuation ->
            rwmem.filtervalues(pid, datatype, targetlist.toLongArray(), expectedValue, expectedValue2, scantype, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(overlayContext.service, "Success in Filter", Toast.LENGTH_SHORT).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun filtermemgroup(pid: Long, datatype: String, expectedValues: Array<String>, tMatches: List<MatchInfo>, overlayContext: OverlayContext): LongArray {
        val rwmem = rwMem()
        val targetlist: MutableList<Long> = mutableListOf()
        var i = 0
        while (i < tMatches.size) {
            targetlist.add(tMatches[i].address)
            i++
        }
        return suspendCancellableCoroutine { continuation ->
            rwmem.filtergroupvalues(pid, datatype, targetlist.toLongArray(), expectedValues, overlayContext) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(overlayContext.service, "Success in Filter", Toast.LENGTH_SHORT).show()
                }
                continuation.resume(onResult)
            }
        }
    }
}
