package com.yervant.huntgames.backend

import android.content.Context
import android.widget.Toast
import com.yervant.huntgames.backend.Memory.Companion.matches
import com.yervant.huntgames.ui.menu.MatchInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class HuntingMemory {

    suspend fun searchmem(pid: Long, datatype: String, targetValue: String, targetValue2: String, scantype: Int, context: Context): LongArray {
        val rwmem = rwMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.searchvalues(pid, datatype, targetValue, targetValue2, scantype, context) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(
                        context,
                        "Success in Search",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun searchmemgroup(pid: Long, datatype: String, targetValues: Array<String>, proxi: Long, context: Context): LongArray {
        val rwmem = rwMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.searchgroupvalues(pid, datatype, targetValues, proxi, context) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(
                        context,
                        "Success in Search",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun readmem(pid: Long, addr: LongArray, datatype: String, context: Context): Array<String> {
        val rwmem = rwMem()
        return suspendCancellableCoroutine { continuation ->
            rwmem.readmultiple(addr, pid, datatype, context) { onResult ->
                continuation.resume(onResult)
            }
        }
    }

    fun writemem(pid: Long, addr: LongArray, datatype: String, value: String, context: Context) {
        val rwmem = rwMem()
        rwmem.writemultiple(pid, addr, datatype, value, context)
    }

    suspend fun filtermem(pid: Long, datatype: String, expectedValue: String, expectedValue2: String, scantype: Int, tMatches: List<MatchInfo>, context: Context): LongArray {
        val rwmem = rwMem()
        val targetlist: MutableList<Long> = mutableListOf()
        var i = 0
        while (i < tMatches.size) {
            targetlist.add(tMatches[i].address)
            i++
        }
        return suspendCancellableCoroutine { continuation ->
            rwmem.filtervalues(pid, datatype, targetlist.toLongArray(), expectedValue, expectedValue2, scantype, context) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(context, "Success in Filter", Toast.LENGTH_SHORT).show()
                }
                continuation.resume(onResult)
            }
        }
    }

    suspend fun filtermemgroup(pid: Long, datatype: String, expectedValues: Array<String>, tMatches: List<MatchInfo>, context: Context): LongArray {
        val rwmem = rwMem()
        val targetlist: MutableList<Long> = mutableListOf()
        var i = 0
        while (i < tMatches.size) {
            targetlist.add(tMatches[i].address)
            i++
        }
        return suspendCancellableCoroutine { continuation ->
            rwmem.filtergroupvalues(pid, datatype, targetlist.toLongArray(), expectedValues, context) { onResult ->
                if (onResult.isNotEmpty()) {
                    Toast.makeText(context, "Success in Filter", Toast.LENGTH_SHORT).show()
                }
                continuation.resume(onResult)
            }
        }
    }
}
