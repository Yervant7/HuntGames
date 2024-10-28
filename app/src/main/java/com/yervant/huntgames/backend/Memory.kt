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
        val valuesArray = when (valtypeselected) {
            "int" -> hunt.readMultiInt(pid, addresses, overlayContext).asList()
            "long" -> hunt.readMultiLong(pid, addresses, overlayContext).asList()
            "float" -> hunt.readMultiFloat(pid, addresses, overlayContext).asList()
            "double" -> hunt.readMultiDouble(pid, addresses, overlayContext).asList()
            else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
        }
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

        val valueint = hunt.readMultiInt(pid, addrs, overlayContext)
        val valuelong = hunt.readMultiLong(pid, addrs, overlayContext)
        val valuefloat = hunt.readMultiFloat(pid, addrs, overlayContext)
        val valuedouble = hunt.readMultiDouble(pid, addrs, overlayContext)
        val values: MutableList<MatchInfo> = mutableListOf()
        if (valueint[0] != 0) {
            values.add(MatchInfo(cleanedAddr.toLong(16), valueint[0].toString(), "int"))
        }
        if (valuelong[0] != 0L) {
            values.add(MatchInfo(cleanedAddr.toLong(16), valuelong[0].toString(), "long"))
        }
        if (valuefloat[0] != 0.0f) {
            values.add(MatchInfo(cleanedAddr.toLong(16), valuefloat[0].toString(), "float"))
        }
        if (valuedouble[0] != 0.0) {
            values.add(MatchInfo(cleanedAddr.toLong(16), valuedouble[0].toString(), "double"))
        }
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

        val valueint = hunt.readMultiInt(pid, addrs, overlayContext)
        val valuelong = hunt.readMultiLong(pid, addrs, overlayContext)
        val valuefloat = hunt.readMultiFloat(pid, addrs, overlayContext)
        val valuedouble = hunt.readMultiDouble(pid, addrs, overlayContext)

        val values: MutableList<MatchInfo> = mutableListOf()
        if (valueint[0] != 0) {
            values.add(MatchInfo(address, valueint[0].toString(), "int"))
        }
        if (valuelong[0] != 0L) {
            values.add(MatchInfo(address, valuelong[0].toString(), "long"))
        }
        if (valuefloat[0] != 0.0f) {
            values.add(MatchInfo(address, valuefloat[0].toString(), "float"))
        }
        if (valuedouble[0] != 0.0) {
            values.add(MatchInfo(address, valuedouble[0].toString(), "double"))
        }
        matches.clear()
        if (values.isNotEmpty()) {
            matches.addAll(values)
        } else {
            Log.d("Memory", "No results to write")
        }

    }

    suspend fun scanAgainstValue(numValStr: String, overlayContext: OverlayContext) {
        try {
            val pid = isattached().savepid()
            val hunt = HuntingMemory()
            val results: MutableList<MatchInfo> = mutableListOf()
            val scantype = getscantype()
            val value = if (numValStr.contains("..")) {
                numValStr.split("..")
            } else {
                listOf(numValStr, "0")
            }

            Log.d("Memory", "value type: $valtypeselected")
            if (matches.isEmpty()) {
                val addresses = when (valtypeselected) {
                    "int" -> hunt.searchInt(pid, value[0].toInt(), value[1].toInt(), scantype, overlayContext)
                    "long" -> hunt.searchLong(pid, value[0].toLong(), value[1].toLong(), scantype, overlayContext)
                    "float" -> hunt.searchFloat(pid, value[0].toFloat(), value[1].toFloat(), scantype, overlayContext)
                    "double" -> hunt.searchDouble(pid, value[0].toDouble(), value[1].toDouble(), scantype, overlayContext)
                    else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
                }
                val valuesArray = when (valtypeselected) {
                    "int" -> hunt.readMultiInt(pid, addresses, overlayContext).asList()
                    "long" -> hunt.readMultiLong(pid, addresses, overlayContext).asList()
                    "float" -> hunt.readMultiFloat(pid, addresses, overlayContext).asList()
                    "double" -> hunt.readMultiDouble(pid, addresses, overlayContext).asList()
                    else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
                }
                var i = 0
                while (i < addresses.size && i < valuesArray.size) {
                    results.add(MatchInfo(addresses[i], valuesArray[i].toString(), valtypeselected))
                    i++
                }
            } else {
                val addresses = when (valtypeselected) {
                    "int" -> hunt.filterMemInt(pid, value[0].toInt(), value[1].toInt(), overlayContext)
                    "long" -> hunt.filterMemLong(pid, value[0].toLong(), value[1].toLong(), overlayContext)
                    "float" -> hunt.filterMemFloat(pid, value[0].toFloat(), value[1].toFloat(), overlayContext)
                    "double" -> hunt.filterMemDouble(pid, value[0].toDouble(), value[1].toDouble(), overlayContext)
                    else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
                }
                val valuesArray = when (valtypeselected) {
                    "int" -> hunt.readMultiInt(pid, addresses, overlayContext).asList()
                    "long" -> hunt.readMultiLong(pid, addresses, overlayContext).asList()
                    "float" -> hunt.readMultiFloat(pid, addresses, overlayContext).asList()
                    "double" -> hunt.readMultiDouble(pid, addresses, overlayContext).asList()
                    else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
                }
                var i = 0
                while (i < addresses.size && i < valuesArray.size) {
                    results.add(MatchInfo(addresses[i], valuesArray[i].toString(), valtypeselected))
                    i++
                }
            }
            matches.clear()
            if (results.isNotEmpty()) {
                matches.addAll(results)
            } else {
                Log.d("Memory", "No results to write")
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