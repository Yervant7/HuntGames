package com.yervant.huntgames.backend

import android.util.Log
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.yervant.huntgames.ui.menu.MatchInfo
import com.yervant.huntgames.ui.menu.isattached
import com.yervant.huntgames.ui.menu.valtypeselected
import com.yervant.huntgames.ui.menu.ResetMatches
import com.yervant.huntgames.ui.menu.UpdateMatches
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

class Memory {

    enum class NumType {
        _int, _long, _float;


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
            "int" -> hunt.readMemInt(pid.toInt(), addr).toString()
            "long" -> hunt.readMemLong(pid.toInt(), addr).toString()
            "float" -> hunt.readMemFloat(pid.toInt(), addr).toString()
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
                if (parts.size >= 2) {
                    val addr = parts[0]
                    val value = parts[1]
                    matches.add(MatchInfo(addr, value))
                }
            }
        }
        return matches
    }

    fun scanAgainstValue(numValStr: String, currentMatches: List<MatchInfo>) {
        try {
            val isAttached = isattached()
            val pid = isAttached.savepid()
            val hunt = HuntingMemory()
            val results: MutableList<Pair<String, String>> = mutableListOf()

            if (currentMatches.isEmpty()) {
                val addresses = when (valtypeselected) {
                    "int" -> hunt.searchInt(pid, numValStr.toInt())
                    "long" -> hunt.searchLong(pid, numValStr.toLong())
                    "float" -> hunt.searchFloat(pid, numValStr.toFloat())
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
                            pw.println("$address $value")
                        }
                    } else {
                        Log.d("Memory", "No results to write")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MemoryScan", "An error occurred: ${e.message}")
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
        //
        val defaultNumType: String = "int";
    }
}