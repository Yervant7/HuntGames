package com.yervant.huntgames.backend

import android.content.Context
import android.util.Log
import com.yervant.huntgames.ui.menu.MatchInfo
import com.yervant.huntgames.ui.menu.getCurrentScanOption
import com.yervant.huntgames.ui.menu.getSelectedRegions
import com.yervant.huntgames.ui.menu.getscantype
import com.yervant.huntgames.ui.menu.isattached
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Memory {

    fun listMatches(maxCount: Int): List<MatchInfo> {
        return if (maxCount > 0 && matches.size > maxCount) {
            matches.take(maxCount)
        } else {
            matches
        }
    }

    suspend fun readMemory(pid: Int, addr: Long, datatype: String, context: Context): String {

        return when (datatype.lowercase()) {
            "int" -> {
                val readValue = HGMem().readMem(pid, addr, 4, context).getOrElse { null }
                if (readValue == null) {
                    ""
                } else {
                    ByteBuffer.wrap(readValue).order(ByteOrder.LITTLE_ENDIAN).int.toString()
                }
            }
            "long" -> {
                val readValue = HGMem().readMem(pid, addr, 8, context).getOrElse { null }
                if (readValue == null) {
                    ""
                } else {
                    ByteBuffer.wrap(readValue).order(ByteOrder.LITTLE_ENDIAN).long.toString()
                }
            }
            "float" -> {
                val readValue = HGMem().readMem(pid, addr, 4, context).getOrElse { null }
                if (readValue == null) {
                    ""
                } else {
                    ByteBuffer.wrap(readValue).order(ByteOrder.LITTLE_ENDIAN).float.toString()
                }
            }
            "double" -> {
                val readValue = HGMem().readMem(pid, addr, 8, context).getOrElse { null }
                if (readValue == null) {
                    ""
                } else {
                    ByteBuffer.wrap(readValue).order(ByteOrder.LITTLE_ENDIAN).double.toString()
                }
            }
            else -> throw IllegalArgumentException("Unsupported data type: $datatype")
        }
    }

    suspend fun getValues(matchs: List<MatchInfo>, context: Context): List<MatchInfo> {
        val pid = isattached().currentPid()
        val newMatchs: MutableList<MatchInfo> = mutableListOf()

        matchs.forEach { match ->
            val value = readMemory(pid, match.address, match.valuetype, context)
            newMatchs.add(MatchInfo(match.address, value, match.valuetype))
        }

        return newMatchs
    }

    suspend fun scanAgainstValue(numValStr: String, context: Context) {
        try {
            val pid = isattached().currentPid()
            val results: MutableList<MatchInfo> = mutableListOf()
            val scantype = getscantype()

            if (scantype == 1 || scantype == 2) {
                if (scantype == 1) {
                    matches.forEach { match ->
                        val valuestr =
                            readMemory(pid, match.address, match.valuetype, context)
                        when (match.valuetype) {
                            "int" -> {
                                if (match.prevValue.toInt() != valuestr.toInt()) {
                                    results.add(
                                        MatchInfo(
                                                match.address,
                                                valuestr,
                                                match.valuetype
                                        )
                                    )
                                }
                            }

                            "long" -> {
                                if (match.prevValue.toLong() != valuestr.toLong()) {
                                    results.add(
                                        MatchInfo(
                                            match.address,
                                            valuestr,
                                            match.valuetype
                                        )
                                    )
                                }
                            }

                            "float" -> {
                                if (match.prevValue.toFloat() != valuestr.toFloat()) {
                                    results.add(
                                        MatchInfo(
                                            match.address,
                                            valuestr,
                                            match.valuetype
                                        )
                                    )
                                }
                            }

                            "double" -> {
                                if (match.prevValue.toFloat() != valuestr.toFloat()) {
                                    results.add(
                                        MatchInfo(
                                            match.address,
                                            valuestr,
                                            match.valuetype
                                        )
                                    )
                                }
                            }
                        }
                    }
                    matches.clear()
                    if (results.isNotEmpty()) {
                        matches.addAll(results)
                    } else {
                        Log.d("Memory", "No results to write")
                    }
                } else {
                    matches.forEach { match ->
                        val valuestr =
                            readMemory(pid, match.address, match.valuetype, context)
                        when (match.valuetype) {
                            "int" -> {
                                if (match.prevValue.toInt() == valuestr.toInt()) {
                                    results.add(
                                        MatchInfo(
                                            match.address,
                                            valuestr,
                                            match.valuetype
                                        )
                                    )
                                }
                            }

                            "long" -> {
                                if (match.prevValue.toLong() == valuestr.toLong()) {
                                    results.add(
                                        MatchInfo(
                                            match.address,
                                            valuestr,
                                            match.valuetype
                                        )
                                    )
                                }
                            }

                            "float" -> {
                                if (match.prevValue.toFloat() == valuestr.toFloat()) {
                                    results.add(
                                        MatchInfo(
                                            match.address,
                                            valuestr,
                                            match.valuetype
                                        )
                                    )
                                }
                            }

                            "double" -> {
                                if (match.prevValue.toDouble() == valuestr.toDouble()) {
                                    results.add(
                                        MatchInfo(
                                            match.address,
                                            valuestr,
                                            match.valuetype
                                        )
                                    )
                                }
                            }
                        }
                    }
                    matches.clear()
                    if (results.isNotEmpty()) {
                        matches.addAll(results)
                    } else {
                        Log.d("Memory", "No results to write")
                    }
                }
            } else {
                if (matches.isEmpty()) {

                    val scanOptions = getCurrentScanOption()
                    val regions = getSelectedRegions()
                    val res = when (scanOptions.valueType.lowercase()) {
                        "int" -> {
                            MemoryScanner(pid).searchInt(
                                numValStr.toInt(),
                                scanOptions.valueType,
                                context,
                                regions,
                            )
                        }

                        "long" -> {
                            MemoryScanner(pid).searchLong(
                                numValStr.toLong(),
                                scanOptions.valueType,
                                context,
                                regions,
                            )
                        }

                        "float" -> {
                            MemoryScanner(pid).searchFloat(
                                numValStr.toFloat(),
                                scanOptions.valueType,
                                context,
                                regions,
                            )
                        }

                        "double" -> {
                            MemoryScanner(pid).searchDouble(
                                numValStr.toDouble(),
                                scanOptions.valueType,
                                context,
                                regions,
                            )
                        }
                        else -> throw IllegalArgumentException("Unsupported data type: ${scanOptions.valueType}")
                    }
                    results.addAll(res)
                } else {
                    val matchs = MemoryScanner(pid).filterAddressesAuto(matches, numValStr, context)
                    results.addAll(matchs)
                }
                matches.clear()
                if (results.isNotEmpty()) {
                    matches.addAll(results)
                } else {
                    Log.d("Memory", "No results to write")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MemoryScan", "An error occurred: ${e.message}")
        }
    }

    companion object {
        var matches: MutableList<MatchInfo> = mutableListOf()
    }
}

class HuntSettings {
    companion object {
        val maxShownMatchesCount: Int = 1000
    }
}