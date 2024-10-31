package com.yervant.huntgames.backend

import android.util.Log
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.yervant.huntgames.ui.menu.MatchInfo
import com.yervant.huntgames.ui.menu.getscantype
import com.yervant.huntgames.ui.menu.isattached
import com.yervant.huntgames.ui.menu.valtypeselected

class Memory {

    enum class NumType {
        _int, _long, _float, _double;


        @Override
        override fun toString(): String {
            return this.name.replace("_", "")
        }

        companion object {
            fun fromString(s: String): NumType {
                var s = s
                if (s[0] != '_') s = "_$s"
                return valueOf(s)
            }
        }
    }

    fun listMatches(maxCount: Int): List<MatchInfo> {
        return if (maxCount > 0 && matches.size > maxCount) {
            matches.take(maxCount)
        } else {
            matches
        }
    }

    suspend fun getvalues(addresses: LongArray, overlayContext: OverlayContext): List<Any> {
        val pid = isattached().savepid()
        val hunt = HuntingMemory()
        val valuesArray = hunt.readmem(pid, addresses, valtypeselected, overlayContext).asList()
        return valuesArray
    }

    suspend fun gotoAddress(address: String, overlayContext: OverlayContext) {
        val pid = isattached().savepid()
        val hunt = HuntingMemory()

        val cleanedAddr = if (address.startsWith("0x")) {
            address.removePrefix("0x")
        } else {
            address
        }

        val addrs = longArrayOf(cleanedAddr.toLong(16))

        val value = hunt.readmem(pid, addrs, valtypeselected, overlayContext)

        val values: MutableList<MatchInfo> = mutableListOf()
        values.add(MatchInfo(cleanedAddr.toLong(16), value[0], valtypeselected))
        matches.clear()
        if (values.isNotEmpty()) {
            matches.addAll(values)
        } else {
            Log.d("Memory", "No results to write")
        }
    }

    suspend fun gotoAddressAndOffset(addr: String, offset: String, issub: Boolean, overlayContext: OverlayContext) {
        val pid = isattached().savepid()
        val hunt = HuntingMemory()

        val cleanedAddr = if (addr.startsWith("0x")) {
            addr.removePrefix("0x")
        } else {
            addr
        }
        val offset_f = if (offset.startsWith("0x")) {
            offset.removePrefix("0x")
        } else {
            offset
        }

        val address = if (issub) {
            cleanedAddr.toLong(16) - offset_f.toLong(16)
        } else {
            cleanedAddr.toLong(16) + offset_f.toLong(16)
        }

        val addrs = longArrayOf(address)

        val valuestr = hunt.readmem(pid, addrs, valtypeselected, overlayContext)

        val values: MutableList<MatchInfo> = mutableListOf()
        values.add(MatchInfo(address, valuestr[0], valtypeselected))
        matches.clear()
        if (values.isNotEmpty()) {
            matches.addAll(values)
        } else {
            Log.d("Memory", "No results to write")
        }

    }

    suspend fun scanAgainstValue(numValStr: String, currentmatcheslist: List<MatchInfo>, overlayContext: OverlayContext) {
        try {
            val pid = isattached().savepid()
            val hunt = HuntingMemory()
            val results: MutableList<MatchInfo> = mutableListOf()
            val scantype = getscantype()

            if (scantype == 3 || scantype == 4) {
                if (scantype == 3) {
                    val targetlist: MutableList<Long> = mutableListOf()
                    var i = 0
                    while (i < currentmatcheslist.size) {
                        targetlist.add(currentmatcheslist[i].address)
                        i++
                    }
                    val valuesstr = hunt.readmem(pid, targetlist.toLongArray(), valtypeselected, overlayContext)
                    when (valtypeselected) {
                        "int" -> {
                            var j = 0
                            while (j < targetlist.size && j < valuesstr.size) {
                                if (currentmatcheslist[j].prevValue.toInt() != valuesstr[j].toInt()) {
                                    results.add(MatchInfo(targetlist[j], valuesstr[j], valtypeselected))
                                }
                                j++
                            }
                        }
                        "long" -> {
                            var j = 0
                            while (j < targetlist.size && j < valuesstr.size) {
                                if (currentmatcheslist[j].prevValue.toLong() != valuesstr[j].toLong()) {
                                    results.add(MatchInfo(targetlist[j], valuesstr[j], valtypeselected))
                                }
                                j++
                            }
                        }
                        "float" -> {
                            var j = 0
                            while (j < targetlist.size && j < valuesstr.size) {
                                if (currentmatcheslist[j].prevValue.toFloat() != valuesstr[j].toFloat()) {
                                    results.add(MatchInfo(targetlist[j], valuesstr[j], valtypeselected))
                                }
                                j++
                            }
                        }
                        "double" -> {
                            var j = 0
                            while (j < targetlist.size && j < valuesstr.size) {
                                if (currentmatcheslist[j].prevValue.toDouble() != valuesstr[j].toDouble()) {
                                    results.add(MatchInfo(targetlist[j], valuesstr[j], valtypeselected))
                                }
                                j++
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
                    val targetlist: MutableList<Long> = mutableListOf()
                    var i = 0
                    while (i < currentmatcheslist.size) {
                        targetlist.add(currentmatcheslist[i].address)
                        i++
                    }
                    val valuesstr = hunt.readmem(pid, targetlist.toLongArray(), valtypeselected, overlayContext)
                    when (valtypeselected) {
                        "int" -> {
                            var j = 0
                            while (j < targetlist.size && j < valuesstr.size) {
                                if (currentmatcheslist[j].prevValue.toInt() == valuesstr[j].toInt()) {
                                    results.add(MatchInfo(targetlist[j], valuesstr[j], valtypeselected))
                                }
                                j++
                            }
                        }
                        "long" -> {
                            var j = 0
                            while (j < targetlist.size && j < valuesstr.size) {
                                if (currentmatcheslist[j].prevValue.toLong() == valuesstr[j].toLong()) {
                                    results.add(MatchInfo(targetlist[j], valuesstr[j], valtypeselected))
                                }
                                j++
                            }
                        }
                        "float" -> {
                            var j = 0
                            while (j < targetlist.size && j < valuesstr.size) {
                                if (currentmatcheslist[j].prevValue.toFloat() == valuesstr[j].toFloat()) {
                                    results.add(MatchInfo(targetlist[j], valuesstr[j], valtypeselected))
                                }
                                j++
                            }
                        }
                        "double" -> {
                            var j = 0
                            while (j < targetlist.size && j < valuesstr.size) {
                                if (currentmatcheslist[j].prevValue.toDouble() == valuesstr[j].toDouble()) {
                                    results.add(MatchInfo(targetlist[j], valuesstr[j], valtypeselected))
                                }
                                j++
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

                val value = if (numValStr.contains("..")) {
                    numValStr.split("..")
                } else {
                    listOf(numValStr, "0")
                }

                if (numValStr.contains(";") && numValStr.contains(":")) {
                    val splited = numValStr.split(":")
                    val distance = splited[1].toLong()
                    val values = splited[0].split(";")
                    val valuesarray = values.toTypedArray()
                    if (matches.isEmpty()) {
                        val addresses = hunt.searchmemgroup(
                            pid,
                            valtypeselected,
                            valuesarray,
                            distance,
                            overlayContext
                        )
                        val vvalues = hunt.readmem(pid, addresses, valtypeselected, overlayContext)
                        var i = 0
                        while (i < addresses.size && i < values.size) {
                            results.add(MatchInfo(addresses[i], vvalues[i], valtypeselected))
                            i++
                        }
                    } else {
                        val addrs = hunt.filtermemgroup(
                            pid,
                            valtypeselected,
                            valuesarray,
                            currentmatcheslist,
                            overlayContext
                        )
                        val vvalues = hunt.readmem(pid, addrs, valtypeselected, overlayContext)
                        var i = 0
                        while (i < addrs.size && i < values.size) {
                            results.add(MatchInfo(addrs[i], vvalues[i], valtypeselected))
                            i++
                        }
                    }
                } else {
                    if (matches.isEmpty()) {
                        val addresses = hunt.searchmem(
                            pid,
                            valtypeselected,
                            value[0],
                            value[1],
                            scantype,
                            overlayContext
                        )
                        val valuesArray =
                            hunt.readmem(pid, addresses, valtypeselected, overlayContext)
                        var i = 0
                        while (i < addresses.size && i < valuesArray.size) {
                            results.add(
                                MatchInfo(
                                    addresses[i],
                                    valuesArray[i],
                                    valtypeselected
                                )
                            )
                            i++
                        }
                    } else {
                        val addresses = hunt.filtermem(
                            pid,
                            valtypeselected,
                            value[0],
                            value[1],
                            scantype,
                            currentmatcheslist,
                            overlayContext
                        )
                        val valuesArray =
                            hunt.readmem(pid, addresses, valtypeselected, overlayContext)
                        var i = 0
                        while (i < addresses.size && i < valuesArray.size) {
                            results.add(
                                MatchInfo(
                                    addresses[i],
                                    valuesArray[i],
                                    valtypeselected
                                )
                            )
                            i++
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