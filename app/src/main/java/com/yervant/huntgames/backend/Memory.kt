package com.yervant.huntgames.backend

import android.util.Log
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.yervant.huntgames.ui.menu.MatchInfo
import com.yervant.huntgames.ui.menu.RegionSelected
import com.yervant.huntgames.ui.menu.isattached
import com.yervant.huntgames.ui.menu.valtypeselected
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

class Memory {

    enum class NumType {
        _int, _long, _float, _double, _all;


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

    private val fileName = "/data/data/com.yervant.huntgames/files/matches.txt"

    enum class Operator {
        greater, less, equal, greaterEqual, lessEqual, notEqual, unknown
    }

    fun listMatches(maxCount: Int): List<MatchInfo> {
        val match = readMatchesFile()
        return if (maxCount > 0 && match.size > maxCount) {
            match.take(maxCount)
        } else {
            match
        }
    }

    fun getvalue(addr: String): String {
        val isAttached = isattached()
        val pid = isAttached.savepid()
        val hunt = HuntingMemory()
        val result = when (valtypeselected) {
            "int" -> hunt.readMemInt(pid, addr).toString()
            "long" -> hunt.readMemLong(pid, addr).toString()
            "float" -> hunt.readMemFloat(pid, addr).toString()
            "double" -> hunt.readMemDouble(pid, addr).toString()
            else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
        }
        return result
    }

    fun readMatchesFile(): List<MatchInfo> {
        val file = File(fileName)
        val matches = mutableListOf<MatchInfo>()

        if (file.exists()) {
            file.forEachLine { line ->
                val parts = line.split(" ")
                if (parts.size >= 3) {
                    val addr = parts[0]
                    val value = parts[1]
                    val valtype = parts[2]
                    matches.add(MatchInfo(addr, value, valtype))
                }
            }
        }
        return matches
    }

    fun gotoAddress(address: String) {
        val isAttached = isattached()
        val pid = isAttached.savepid()
        val hunt = HuntingMemory()

        val valueint = hunt.readMemInt(pid, address)
        val valuelong = hunt.readMemLong(pid, address)
        val valuefloat = hunt.readMemFloat(pid, address)
        val valuedouble = hunt.readMemDouble(pid, address)
        val values: MutableList<String> = mutableListOf()
        if (valueint != 0) {
            values.add(valueint.toString())
        }
        if (valuelong != 0L) {
            values.add(valuelong.toString())
        }
        if (valuefloat != 0.0f) {
            values.add(valuefloat.toString())
        }
        if (valuedouble != 0.0) {
            values.add(valuedouble.toString())
        }
        val results: MutableList<Pair<String, String>> = mutableListOf()
        values.forEach { value ->
            results.add(address to value)
        }

        val file = File(fileName)
        if (file.exists()) {
            file.delete()
        }
        FileOutputStream(fileName).use { fos ->
            PrintWriter(fos).use { pw ->
                if (results.isNotEmpty()) {
                    results.forEach { (address, value) ->
                        pw.println("$address $value")
                    }
                } else {
                    Log.d("Memory", "No results to write")
                }
            }
        }
    }

    fun gotoAddressAndOffset(addr: String, offset: String, issub: Boolean) {
        val isAttached = isattached()
        val pid = isAttached.savepid()
        val hunt = HuntingMemory()

        val cleanedAddr = if (addr.startsWith("0x")) {
            addr.removePrefix("0x")
        } else {
            addr
        }
        val offset = if (offset.startsWith("0x")) {
            offset.removePrefix("0x")
        } else {
            offset
        }

        val address = if (issub) {
            cleanedAddr.toLong(16) - offset.toLong(16)
        } else {
            cleanedAddr.toLong(16) + offset.toLong(16)
        }

        val valueint = hunt.readMemInt(pid, address.toString(16))
        val valuelong = hunt.readMemLong(pid, address.toString(16))
        val valuefloat = hunt.readMemFloat(pid, address.toString(16))
        val valuedouble = hunt.readMemDouble(pid, address.toString(16))

        val values: MutableList<String> = mutableListOf()
        if (valueint != 0) {
            values.add(valueint.toString())
        }
        if (valuelong != 0L) {
            values.add(valuelong.toString())
        }
        if (valuefloat != 0.0f) {
            values.add(valuefloat.toString())
        }
        if (valuedouble != 0.0) {
            values.add(valuedouble.toString())
        }
        val results: MutableList<Pair<String, String>> = mutableListOf()
        values.forEach { value ->
            results.add(address.toString(16) to value)
        }

        val file = File(fileName)
        if (file.exists()) {
            file.delete()
        }
        FileOutputStream(fileName).use { fos ->
            PrintWriter(fos).use { pw ->
                if (results.isNotEmpty()) {
                    results.forEach { (address, value) ->
                        pw.println("$address $value")
                    }
                } else {
                    Log.d("Memory", "No results to write")
                }
            }
        }
    }

    fun scanAgainstValue(numValStr: String, currentMatches: List<MatchInfo>) {
        try {
            val isAttached = isattached()
            val pid = isAttached.savepid()
            val hunt = HuntingMemory()
            val results: MutableList<Pair<String, String>> = mutableListOf()

            Log.d("Memory", "value type: $valtypeselected")
            if (valtypeselected == "all") {
                searchAndFilterAll(pid, numValStr, currentMatches)
            } else {
                if (currentMatches.isEmpty()) {
                    val addresses = when (valtypeselected) {
                        "int" -> hunt.searchInt(pid, numValStr.toInt())
                        "long" -> hunt.searchLong(pid, numValStr.toLong())
                        "float" -> hunt.searchFloat(pid, numValStr.toFloat())
                        "double" -> hunt.searchDouble(pid, numValStr.toDouble())
                        else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
                    }
                    for (address in addresses) {
                        results.add(address to numValStr)
                    }
                } else {
                    val addresses = when (valtypeselected) {
                        "int" -> hunt.filterMemInt(pid, numValStr)
                        "long" -> hunt.filterMemLong(pid, numValStr)
                        "float" -> hunt.filterMemFloat(pid, numValStr)
                        "double" -> hunt.filterMemDouble(pid, numValStr)
                        else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
                    }
                    for (address in addresses) {
                        results.add(address to numValStr)
                    }
                }

                val file = File(fileName)
                if (file.exists()) {
                    file.delete()
                }
                FileOutputStream(fileName).use { fos ->
                    PrintWriter(fos).use { pw ->
                        if (results.isNotEmpty()) {
                            results.forEach { (address, value) ->
                                pw.println("$address $value $valtypeselected")
                            }
                        } else {
                            Log.d("Memory", "No results to write")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MemoryScan", "An error occurred: ${e.message}")
        }
    }

    fun searchAndFilterAll(pid: Long, value: String, currentMatches: List<MatchInfo>) {
        val hunt = HuntingMemory()
        val valueint = value.toIntOrNull()
        val valuelong = value.toLongOrNull()
        val valuefloat = value.toFloatOrNull()
        val valuedouble = value.toDoubleOrNull()
        val file = File(fileName)
        if (file.exists()) {
            file.delete()
        }
        if (currentMatches.isEmpty()) {
            if (valueint != null) {
                val addresses = hunt.searchInt(pid, value.toInt())
                FileOutputStream(fileName, true).use { fos ->
                    PrintWriter(fos).use { pw ->
                        if (addresses.isNotEmpty()) {
                            addresses.forEach { address ->
                                pw.println("$address $value int")
                            }
                        } else {
                            Log.d("Memory", "No results to write")
                        }
                    }
                }
            }
            if (valuelong != null) {
                val addresses = hunt.searchLong(pid, value.toLong())
                FileOutputStream(fileName, true).use { fos ->
                    PrintWriter(fos).use { pw ->
                        if (addresses.isNotEmpty()) {
                            addresses.forEach { address ->
                                pw.println("$address $value long")
                            }
                        } else {
                            Log.d("Memory", "No results to write")
                        }
                    }
                }
            }
            if (valuefloat != null) {
                val addresses = hunt.searchFloat(pid, value.toFloat())
                FileOutputStream(fileName, true).use { fos ->
                    PrintWriter(fos).use { pw ->
                        if (addresses.isNotEmpty()) {
                            addresses.forEach { address ->
                                pw.println("$address $value float")
                            }
                        } else {
                            Log.d("Memory", "No results to write")
                        }
                    }
                }
            }
            if (valuedouble != null) {
                val addresses = hunt.searchDouble(pid, value.toDouble())
                FileOutputStream(fileName, true).use { fos ->
                    PrintWriter(fos).use { pw ->
                        if (addresses.isNotEmpty()) {
                            addresses.forEach { address ->
                                pw.println("$address $value double")
                            }
                        } else {
                            Log.d("Memory", "No results to write")
                        }
                    }
                }
            }
        } else {
            if (valueint != null) {
                val addresses = hunt.filterMemInt(pid, value)
                FileOutputStream(fileName, true).use { fos ->
                    PrintWriter(fos).use { pw ->
                        if (addresses.isNotEmpty()) {
                            addresses.forEach { address ->
                                pw.println("$address $value int")
                            }
                        } else {
                            Log.d("Memory", "No results to write")
                        }
                    }
                }
            }
            if (valuelong != null) {
                val addresses = hunt.filterMemLong(pid, value)
                FileOutputStream(fileName, true).use { fos ->
                    PrintWriter(fos).use { pw ->
                        if (addresses.isNotEmpty()) {
                            addresses.forEach { address ->
                                pw.println("$address $value long")
                            }
                        } else {
                            Log.d("Memory", "No results to write")
                        }
                    }
                }
            }
            if (valuefloat != null) {
                val addresses = hunt.filterMemFloat(pid, value)
                FileOutputStream(fileName, true).use { fos ->
                    PrintWriter(fos).use { pw ->
                        if (addresses.isNotEmpty()) {
                            addresses.forEach { address ->
                                pw.println("$address $value float")
                            }
                        } else {
                            Log.d("Memory", "No results to write")
                        }
                    }
                }
            }
            if (valuedouble != null) {
                val addresses = hunt.filterMemDouble(pid, value)
                FileOutputStream(fileName, true).use { fos ->
                    PrintWriter(fos).use { pw ->
                        if (addresses.isNotEmpty()) {
                            addresses.forEach { address ->
                                pw.println("$address $value double")
                            }
                        } else {
                            Log.d("Memory", "No results to write")
                        }
                    }
                }
            }
        }
    }

    fun scanAgainstValueGroup(numValStr: String, currentMatches: List<MatchInfo>) {
        try {
            val isAttached = isattached()
            val pid = isAttached.savepid()
            val hunt = HuntingMemory()
            val results: MutableList<Pair<String, String>> = mutableListOf()

            Log.d("Memory", "value type: $valtypeselected")
            if (valtypeselected == "all") {
                searchAndFilterAllGroup(pid, numValStr, currentMatches)
            } else {
                if (currentMatches.isEmpty()) {
                    val result = when (valtypeselected) {
                        "int" -> hunt.searchGroupInt(pid, numValStr)
                        "long" -> hunt.searchGroupLong(pid, numValStr)
                        "float" -> hunt.searchGroupFloat(pid, numValStr)
                        "double" -> hunt.searchGroupDouble(pid, numValStr)
                        else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
                    }
                    results.addAll(result)
                } else {
                    val result = when (valtypeselected) {
                        "int" -> hunt.filterMemIntGroup(pid, numValStr)
                        "long" -> hunt.filterMemLongGroup(pid, numValStr)
                        "float" -> hunt.filterMemFloatGroup(pid, numValStr)
                        "double" -> hunt.filterMemDoubleGroup(pid, numValStr)
                        else -> throw IllegalArgumentException("Unsupported value type selected: $valtypeselected")
                    }
                    results.addAll(result)
                }

                val file = File(fileName)
                if (file.exists()) {
                    file.delete()
                }
                FileOutputStream(fileName).use { fos ->
                    PrintWriter(fos).use { pw ->
                        if (results.isNotEmpty()) {
                            results.forEach { (address, value) ->
                                pw.println("$address $value $valtypeselected")
                            }
                        } else {
                            Log.d("Memory", "No results to write")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MemoryScan", "An error occurred: ${e.message}")
        }
    }

    fun searchAndFilterAllGroup(pid: Long, values: String, currentMatches: List<MatchInfo>) {
        val hunt = HuntingMemory()
        val file = File(fileName)
        if (file.exists()) {
            file.delete()
        }
        if (currentMatches.isEmpty()) {
            val result1 = hunt.searchGroupInt(pid, values)
            FileOutputStream(fileName, true).use { fos ->
                PrintWriter(fos).use { pw ->
                    if (result1.isNotEmpty()) {
                        for ((address, value) in result1) {
                            pw.println("$address $value int")
                        }
                    } else {
                        Log.d("Memory", "No results to write")
                    }
                }
            }
            val result2 = hunt.searchGroupLong(pid, values)
            FileOutputStream(fileName, true).use { fos ->
                PrintWriter(fos).use { pw ->
                    if (result2.isNotEmpty()) {
                        for ((address, value) in result2) {
                            pw.println("$address $value long")
                        }
                    } else {
                            Log.d("Memory", "No results to write")
                    }
                }
            }
            val result3 = hunt.searchGroupFloat(pid, values)
            FileOutputStream(fileName, true).use { fos ->
                PrintWriter(fos).use { pw ->
                    if (result3.isNotEmpty()) {
                        for ((address, value) in result3) {
                                pw.println("$address $value float")
                        }
                    } else {
                        Log.d("Memory", "No results to write")
                    }
                }
            }
            val result4 = hunt.searchGroupDouble(pid, values)
            FileOutputStream(fileName, true).use { fos ->
                PrintWriter(fos).use { pw ->
                    if (result4.isNotEmpty()) {
                        for ((address, value) in result4) {
                            pw.println("$address $value double")
                        }
                    } else {
                        Log.d("Memory", "No results to write")
                    }
                }
            }
        } else {
            val result1 = hunt.filterMemIntGroup(pid, values)
            FileOutputStream(fileName, true).use { fos ->
                PrintWriter(fos).use { pw ->
                    if (result1.isNotEmpty()) {
                        for ((address, value) in result1)  {
                            pw.println("$address $value int")
                        }
                    } else {
                        Log.d("Memory", "No results to write")
                    }
                }
            }
            val result2 = hunt.filterMemLongGroup(pid, values)
            FileOutputStream(fileName, true).use { fos ->
                PrintWriter(fos).use { pw ->
                    if (result2.isNotEmpty()) {
                        for ((address, value) in result2)  {
                            pw.println("$address $value long")
                        }
                    } else {
                        Log.d("Memory", "No results to write")
                    }
                }
            }
            val result3 = hunt.filterMemFloatGroup(pid, values)
            FileOutputStream(fileName, true).use { fos ->
                PrintWriter(fos).use { pw ->
                    if (result3.isNotEmpty()) {
                        for ((address, value) in result3)  {
                            pw.println("$address $value float")
                        }
                    } else {
                        Log.d("Memory", "No results to write")
                    }
                }
            }
            val result4 = hunt.filterMemDoubleGroup(pid, values)
            FileOutputStream(fileName, true).use { fos ->
                PrintWriter(fos).use { pw ->
                    if (result4.isNotEmpty()) {
                        for ((address, value) in result4)  {
                            pw.println("$address $value double")
                        }
                    } else {
                        Log.d("Memory", "No results to write")
                    }
                }
            }
        }
    }

    fun getMatchCount(matches: List<MatchInfo>): Int {
        return matches.size
    }

    // todo
    companion object {

        // https://stackoverflow.com/a/507658/14073678
        val operatorEnumToSymbolBiMap: BiMap<Operator, String> = HashBiMap.create()

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