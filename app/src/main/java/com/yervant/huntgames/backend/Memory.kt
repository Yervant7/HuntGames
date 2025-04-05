package com.yervant.huntgames.backend

import android.content.Context
import android.util.Log
import com.yervant.huntgames.ui.menu.MatchInfo
import com.yervant.huntgames.ui.menu.getCurrentScanOption
import com.yervant.huntgames.ui.menu.getSelectedRegions
import com.yervant.huntgames.ui.menu.isattached
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Memory {

    fun listMatches(maxCount: Int): List<MatchInfo> {
        return synchronized(matches) {
            matches.take(maxCount).toList()
        }
    }

    suspend fun readMemory(pid: Int, addr: Long, datatype: String, context: Context): Number {
        return when (datatype.lowercase()) {
            "int" -> {
                val byteCount = 4
                val bytes = HGMem().readMem(pid, addr, byteCount.toLong(), context).getOrNull()
                if (bytes != null && bytes.size == byteCount) {
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
                } else {
                    Log.w(TAG, "Failed to read Int at $addr. Returning 0.")
                    0
                }
            }
            "long" -> {
                val byteCount = 8
                val bytes = HGMem().readMem(pid, addr, byteCount.toLong(), context).getOrNull()
                if (bytes != null && bytes.size == byteCount) {
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).long
                } else {
                    Log.w(TAG, "Failed to read Long at $addr. Returning 0L.")
                    0L
                }
            }
            "float" -> {
                val byteCount = 4
                val bytes = HGMem().readMem(pid, addr, byteCount.toLong(), context).getOrNull()
                if (bytes != null && bytes.size == byteCount) {
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float
                } else {
                    Log.w(TAG, "Failed to read Float at $addr. Returning 0.0f.")
                    0.0f
                }
            }
            "double" -> {
                val byteCount = 8
                val bytes = HGMem().readMem(pid, addr, byteCount.toLong(), context).getOrNull()
                if (bytes != null && bytes.size == byteCount) {
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).double
                } else {
                    Log.w(TAG, "Failed to read Double at $addr. Returning 0.0.")
                    0.0
                }
            }
            else -> {
                Log.w(TAG, "Unsupported data type: $datatype. Returning 0.")
                0
            }
        }
    }

    suspend fun getValues(matches: List<MatchInfo>, context: Context): List<MatchInfo> {
        val pid = isattached().currentPid()
        val newMatches = mutableListOf<MatchInfo>()

        matches.forEach { match ->
            val value = readMemory(pid, match.address, match.valuetype, context)
            if (value != 0 && value != 0.0) {
                newMatches.add(match.copy(prevValue = value))
            }
        }

        return newMatches
    }

    suspend fun scanValues(numValStr: String, context: Context) {
        try {
            val pid = isattached().currentPid()
            val results: MutableList<MatchInfo> = mutableListOf()
            val localMatches = synchronized(matches) { matches.toList() }
            val scanOptions = getCurrentScanOption()
            val regions = getSelectedRegions()
            if (numValStr.contains(";") && numValStr.contains(":")) {
                val res = MemoryScanner(pid).searchGroupValues(
                    groupQuery = numValStr,
                    dataType = scanOptions.valueType,
                    context = context,
                    regions = regions,
                    operator = scanOptions.operator
                )
                results.addAll(res)
            } else if (numValStr.contains("..")) {
                val values = numValStr.split("..")
                val res = when (scanOptions.valueType.lowercase()) {
                    "int" -> {
                        MemoryScanner(pid).searchRangeInt(
                            values[0].trim().toInt(),
                            values[1].trim().toInt(),
                            scanOptions.valueType,
                            context,
                            regions,
                        )
                    }

                    "long" -> {
                        MemoryScanner(pid).searchRangeLong(
                            values[0].trim().toLong(),
                            values[1].trim().toLong(),
                            scanOptions.valueType,
                            context,
                            regions,
                        )
                    }

                    "float" -> {
                        MemoryScanner(pid).searchRangeFloat(
                            values[0].trim().toFloat(),
                            values[1].trim().toFloat(),
                            scanOptions.valueType,
                            context,
                            regions,
                        )
                    }

                    "double" -> {
                        MemoryScanner(pid).searchRangeDouble(
                            values[0].trim().toDouble(),
                            values[1].trim().toDouble(),
                            scanOptions.valueType,
                            context,
                            regions,
                        )
                    }

                    else -> throw IllegalArgumentException("Unsupported data type: ${scanOptions.valueType}")
                }
                results.addAll(res)
            } else {
                if (localMatches.isEmpty()) {
                    val res = when (scanOptions.valueType.lowercase()) {
                        "int" -> {
                            MemoryScanner(pid).searchInt(
                                numValStr.toInt(),
                                scanOptions.valueType,
                                context,
                                regions,
                                operator = scanOptions.operator
                            )
                        }

                        "long" -> {
                            MemoryScanner(pid).searchLong(
                                numValStr.toLong(),
                                scanOptions.valueType,
                                context,
                                regions,
                                operator = scanOptions.operator
                            )
                        }

                        "float" -> {
                            MemoryScanner(pid).searchFloat(
                                numValStr.toFloat(),
                                scanOptions.valueType,
                                context,
                                regions,
                                operator = scanOptions.operator
                            )
                        }

                        "double" -> {
                            MemoryScanner(pid).searchDouble(
                                numValStr.toDouble(),
                                scanOptions.valueType,
                                context,
                                regions,
                                operator = scanOptions.operator
                            )
                        }

                        else -> throw IllegalArgumentException("Unsupported data type: ${scanOptions.valueType}")
                    }
                    results.addAll(res)
                } else {
                    val matchs =
                        MemoryScanner(pid).filterAddressesAuto(localMatches, numValStr, context)
                    results.addAll(matchs)
                }
            }
            if (results.isEmpty()) {
                Log.d("Memory", "No results to write")
            }
            synchronized(matches) {
                matches.clear()
                matches.addAll(results)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MemoryScan", "An error occurred: ${e.message}")
        }
    }

    companion object {
        const val TAG = "Memory"
        var matches: MutableList<MatchInfo> = mutableListOf()
    }
}

class HuntSettings {
    companion object {
        const val maxShownMatchesCount: Int = 1000
    }
}