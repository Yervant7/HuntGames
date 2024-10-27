package com.yervant.huntgames.backend

import android.util.Log
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.yervant.huntgames.ui.menu.MatchInfo
import com.yervant.huntgames.ui.menu.savepid
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

    enum class Operator {
        greater, less, equal, greaterEqual, lessEqual, notEqual, unknown
    }

    fun listMatches(maxCount: Int): List<MatchInfo> {
        return if (maxCount > 0 && matches.size > maxCount) {
            matches.take(maxCount)
        } else {
            matches
        }
    }

    suspend fun getvalue(addr: Long, overlayContext: OverlayContext): String {
        val pid = savepid()
        val hunt = HuntingMemory()
        val result = when (valtypeselected) {
            "int" -> hunt.readMemInt(pid, addr, overlayContext).toString()
            "long" -> hunt.readMemLong(pid, addr, overlayContext).toString()
            "float" -> hunt.readMemFloat(pid, addr, overlayContext).toString()
            "double" -> hunt.readMemDouble(pid, addr, overlayContext).toString()
            else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
        }
        return result
    }

    suspend fun gotoAddress(address: String, overlayContext: OverlayContext) {
        val pid = savepid()
        val hunt = HuntingMemory()

        val cleanedAddr = if (address.startsWith("0x")) {
            address.removePrefix("0x")
        } else {
            address
        }

        val valueint = hunt.readMemInt(pid, cleanedAddr.toLong(16), overlayContext)
        val valuelong = hunt.readMemLong(pid, cleanedAddr.toLong(16), overlayContext)
        val valuefloat = hunt.readMemFloat(pid, cleanedAddr.toLong(16), overlayContext)
        val valuedouble = hunt.readMemDouble(pid, cleanedAddr.toLong(16), overlayContext)
        val values: MutableList<MatchInfo> = mutableListOf()
        if (valueint != 0) {
            values.add(MatchInfo(cleanedAddr.toLong(16), valueint.toString(), "int"))
        }
        if (valuelong != 0L) {
            values.add(MatchInfo(cleanedAddr.toLong(16), valuelong.toString(), "long"))
        }
        if (valuefloat != 0.0f) {
            values.add(MatchInfo(cleanedAddr.toLong(16), valuefloat.toString(), "float"))
        }
        if (valuedouble != 0.0) {
            values.add(MatchInfo(cleanedAddr.toLong(16), valuedouble.toString(), "double"))
        }
        matches.clear()
        if (values.isNotEmpty()) {
            matches.addAll(values)
        } else {
            Log.d("Memory", "No results to write")
        }
    }

    suspend fun gotoAddressAndOffset(addr: String, offset: String, issub: Boolean, overlayContext: OverlayContext) {
        val pid = savepid()
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

        val valueint = hunt.readMemInt(pid, address, overlayContext)
        val valuelong = hunt.readMemLong(pid, address, overlayContext)
        val valuefloat = hunt.readMemFloat(pid, address, overlayContext)
        val valuedouble = hunt.readMemDouble(pid, address, overlayContext)

        val values: MutableList<MatchInfo> = mutableListOf()
        if (valueint != 0) {
            values.add(MatchInfo(address, valueint.toString(), "int"))
        }
        if (valuelong != 0L) {
            values.add(MatchInfo(address, valuelong.toString(), "long"))
        }
        if (valuefloat != 0.0f) {
            values.add(MatchInfo(address, valuefloat.toString(), "float"))
        }
        if (valuedouble != 0.0) {
            values.add(MatchInfo(address, valuedouble.toString(), "double"))
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
            val pid = savepid()
            val hunt = HuntingMemory()
            val results: MutableList<MatchInfo> = mutableListOf()

            Log.d("Memory", "value type: $valtypeselected")
            if (matches.isEmpty()) {
                val addresses = when (valtypeselected) {
                    "int" -> hunt.searchInt(pid, numValStr.toInt(), overlayContext)
                    "long" -> hunt.searchLong(pid, numValStr.toLong(), overlayContext)
                    "float" -> hunt.searchFloat(pid, numValStr.toFloat(), overlayContext)
                    "double" -> hunt.searchDouble(pid, numValStr.toDouble(), overlayContext)
                    else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
                }
                for (address in addresses) {
                    results.add(MatchInfo(address, numValStr, valtypeselected))
                }
            } else {
                val addresses = when (valtypeselected) {
                    "int" -> hunt.filterMemInt(pid, numValStr.toInt(), overlayContext)
                    "long" -> hunt.filterMemLong(pid, numValStr.toLong(), overlayContext)
                    "float" -> hunt.filterMemFloat(pid, numValStr.toFloat(), overlayContext)
                    "double" -> hunt.filterMemDouble(pid, numValStr.toDouble(), overlayContext)
                    else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
                }
                for (address in addresses) {
                    results.add(MatchInfo(address, numValStr, valtypeselected))
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

    // todo
    companion object {

        // https://stackoverflow.com/a/507658/14073678
        val operatorEnumToSymbolBiMap: BiMap<Operator, String> = HashBiMap.create()

        var matches: MutableList<MatchInfo> = mutableListOf()

        init {
            // operatorEnumToSymbolBiMap.put(Operator.greater, ">")
            //operatorEnumToSymbolBiMap.put(Operator.less, "<")
            operatorEnumToSymbolBiMap.put(Operator.equal, "=")
            // operatorEnumToSymbolBiMap.put(Operator.greaterEqual, ">=")
            // operatorEnumToSymbolBiMap.put(Operator.lessEqual, "<=")
            // operatorEnumToSymbolBiMap.put(Operator.notEqual, "!=")
        }
    }
}

class HuntSettings {
    companion object {
        val maxShownMatchesCount: Int = 1000
        // default to exact scan
        val defaultScanType: Memory.Operator = Memory.Operator.equal;
    }
}