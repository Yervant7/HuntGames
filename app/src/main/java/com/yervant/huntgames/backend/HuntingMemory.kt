package com.yervant.huntgames.backend

import com.yervant.huntgames.ui.menu.RegionSelected
import android.util.Log
import java.io.File
import java.lang.Process

class HuntingMemory {

    private val binDirPath = "/data/data/com.yervant.huntgames/files/bin"
    private val filterdirPath = "$binDirPath/filteroutput"
    private val searchOutputPath = "$binDirPath/searchoutput.txt"
    private val filterOutputPath = "$filterdirPath/filteroutput_${System.currentTimeMillis()}.txt"
    private val readOutputPath = "$binDirPath/readoutput.txt"
    private val writeOutputPath = "$binDirPath/writeoutput.txt"
    private val disassembleOutputPath = "$binDirPath/disassembleoutput.txt"
    private var currentFilteredAddresses = mutableListOf<String>()
    private var currentFilteredAddresses2 = mutableListOf<Pair<String, String>>()

    private fun executeRootCommand(command: String): Process? {
        return Runtime.getRuntime().exec(arrayOf("su", "-c", command))
    }

    private fun executeRootCommandWithOutput(command: String): List<String> {
        val output = mutableListOf<String>()
        try {
            val process = executeRootCommand(command)
            process?.waitFor()
            val file = File(searchOutputPath)
            if (file.exists()) {
                file.forEachLine { line ->
                    val va = line.split(" ")
                    output.add(va[0])
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return output
    }

    private fun executeRootCommandWithOutput2(command: String): List<String> {
        val output = mutableListOf<String>()
        try {
            val process = executeRootCommand(command)
            process?.waitFor()

            val out = getLatestFile(filterdirPath)
            val path = "$filterdirPath/$out"
            val file = File(path)
            if (file.exists()) {
                output.addAll(file.readLines())
            } else {
                Log.d("HuntingMemory", "file not exists: ${file.path}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return output
    }

    private fun executeRootCommandWithOutput3(command: String): List<Pair<String, String>> {
        val output = mutableListOf<Pair<String, String>>()
        try {
            val process = executeRootCommand(command)
            process?.waitFor()
            val file = File(searchOutputPath)
            if (file.exists()) {
                file.forEachLine { line ->
                    val va = line.split(" ")
                    output.add(va[0] to va[1])
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return output
    }

    private fun executeRootCommandWithOutput4(command: String): List<Pair<String, String>> {
        val output = mutableListOf<Pair<String, String>>()
        try {
            val process = executeRootCommand(command)
            process?.waitFor()

            val out = getLatestFile(filterdirPath)
            val path = "$filterdirPath/$out"
            val file = File(path)
            if (file.exists()) {
                file.forEachLine { line ->
                    val va = line.split(" ")
                    output.add(va[0] to va[1])
                }
            } else {
                Log.d("HuntingMemory", "file not exists: ${file.path}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return output
    }

    private fun executeRootCommand2(command: String, outputPath: String): String {
        var output = ""
        try {
            val process = executeRootCommand(command)
            process?.waitFor()
            val file = File(outputPath)
            if (file.exists()) {
                output = file.readText()
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return output
    }

    private fun ensureFileDeleted(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun extractValue(output: String): String {
        val start = "Value: "
        if (output.isEmpty()) {
            Log.d("HuntingMemory", "Output is empty")
            return ""
        }
        val startIndex = output.indexOf(start)
        if (startIndex == -1) {
            throw Exception("Start not found in $output")
        }

        val valueStartIndex = startIndex + start.length
        if (valueStartIndex >= output.length) {
            throw Exception("Value not found after $start in $output")
        }

        val value = output.substring(valueStartIndex).trim()
        if (value.isBlank()) {
            throw Exception("Value not found in $output")
        }

        return value
    }

    fun searchInt(pid: Long, targetValue: Int): List<String> {
        ensureFileDeleted(searchOutputPath)
        val regions = RegionSelected()
        Log.d("HuntingMemory", "Regions: $regions")
        val command = "LD_LIBRARY_PATH=$binDirPath nice -n -20 .$binDirPath/RWMem search-int $pid $targetValue $regions $searchOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput(command).toMutableList()
        return currentFilteredAddresses
    }

    fun searchLong(pid: Long, targetValue: Long): List<String> {
        ensureFileDeleted(searchOutputPath)
        val regions = RegionSelected()
        Log.d("HuntingMemory", "Regions: $regions")
        val command = "LD_LIBRARY_PATH=$binDirPath nice -n -20 .$binDirPath/RWMem search-long $pid $targetValue $regions $searchOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput(command).toMutableList()
        return currentFilteredAddresses
    }

    fun searchFloat(pid: Long, targetValue: Float): List<String> {
        ensureFileDeleted(searchOutputPath)
        val regions = RegionSelected()
        Log.d("HuntingMemory", "Regions: $regions")
        val command = "LD_LIBRARY_PATH=$binDirPath nice -n -20 .$binDirPath/RWMem search-float $pid $targetValue $regions $searchOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput(command).toMutableList()
        return currentFilteredAddresses
    }

    fun searchDouble(pid: Long, targetValue: Double): List<String> {
        ensureFileDeleted(searchOutputPath)
        val regions = RegionSelected()
        Log.d("HuntingMemory", "Regions: $regions")
        val command = "LD_LIBRARY_PATH=$binDirPath nice -n -20 .$binDirPath/RWMem search-double $pid $targetValue $regions $searchOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput(command).toMutableList()
        return currentFilteredAddresses
    }

    fun searchGroupInt(pid: Long, targetValues: String): List<Pair<String, String>> {
        ensureFileDeleted(searchOutputPath)
        val regions = RegionSelected()
        Log.d("HuntingMemory", "Regions: $regions")
        val command = "LD_LIBRARY_PATH=$binDirPath nice -n -20 .$binDirPath/RWMem search-group-int $pid '$targetValues' $regions $searchOutputPath"
        currentFilteredAddresses2 = executeRootCommandWithOutput3(command).toMutableList()
        return currentFilteredAddresses2
    }

    fun searchGroupLong(pid: Long, targetValues: String): List<Pair<String, String>> {
        ensureFileDeleted(searchOutputPath)
        val regions = RegionSelected()
        Log.d("HuntingMemory", "Regions: $regions")
        val command = "LD_LIBRARY_PATH=$binDirPath nice -n -20 .$binDirPath/RWMem search-group-long $pid '$targetValues' $regions $searchOutputPath"
        currentFilteredAddresses2 = executeRootCommandWithOutput3(command).toMutableList()
        return currentFilteredAddresses2
    }

    fun searchGroupFloat(pid: Long, targetValues: String): List<Pair<String, String>> {
        ensureFileDeleted(searchOutputPath)
        val regions = RegionSelected()
        Log.d("HuntingMemory", "Regions: $regions")
        val command = "LD_LIBRARY_PATH=$binDirPath nice -n -20 .$binDirPath/RWMem search-group-float $pid '$targetValues' $regions $searchOutputPath"
        currentFilteredAddresses2 = executeRootCommandWithOutput3(command).toMutableList()
        return currentFilteredAddresses2
    }

    fun searchGroupDouble(pid: Long, targetValues: String): List<Pair<String, String>> {
        ensureFileDeleted(searchOutputPath)
        val regions = RegionSelected()
        Log.d("HuntingMemory", "Regions: $regions")
        val command = "LD_LIBRARY_PATH=$binDirPath nice -n -20 .$binDirPath/RWMem search-group-double $pid '$targetValues' $regions $searchOutputPath"
        currentFilteredAddresses2 = executeRootCommandWithOutput3(command).toMutableList()
        return currentFilteredAddresses2
    }

    fun readMemInt(pid: Long, addr: String): Int {
        ensureFileDeleted(readOutputPath)
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem read $pid $addr 4 int > $readOutputPath"
        val output = executeRootCommand2(command, readOutputPath)
        return extractValue(output).toIntOrNull() ?: 0
    }

    fun readMemLong(pid: Long, addr: String): Long {
        ensureFileDeleted(readOutputPath)
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem read $pid $addr 8 long > $readOutputPath"
        val output = executeRootCommand2(command, readOutputPath)
        return extractValue(output).toLongOrNull() ?: 0
    }

    fun readMemFloat(pid: Long, addr: String): Float {
        ensureFileDeleted(readOutputPath)
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem read $pid $addr 4 float > $readOutputPath"
        val output = executeRootCommand2(command, readOutputPath)
        return extractValue(output).toFloatOrNull() ?: 0.0f
    }

    fun readMemDouble(pid: Long, addr: String): Double {
        ensureFileDeleted(readOutputPath)
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem read $pid $addr 8 Double > $readOutputPath"
        val output = executeRootCommand2(command, readOutputPath)
        return extractValue(output).toDoubleOrNull() ?: 0.0
    }

    fun writeMemInt(pid: Long, addr: String, value: Int) {
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem write-int $pid $addr $value > $writeOutputPath"
        val output = executeRootCommand2(command, writeOutputPath)
        Log.d("HuntingMemory", "Write output: $output")
    }

    fun writeMemLong(pid: Long, addr: String, value: Long) {
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem write-long $pid $addr $value > $writeOutputPath"
        val output = executeRootCommand2(command, writeOutputPath)
        Log.d("HuntingMemory", "Write output: $output")
    }

    fun writeMemFloat(pid: Long, addr: String, value: Float) {
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem write-float $pid $addr $value > $writeOutputPath"
        val output = executeRootCommand2(command, writeOutputPath)
        Log.d("HuntingMemory", "Write output: $output")
    }

    fun writeMemDouble(pid: Long, addr: String, value: Double) {
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem write-double $pid $addr $value > $writeOutputPath"
        val output = executeRootCommand2(command, writeOutputPath)
        Log.d("HuntingMemory", "Write output: $output")
    }

    private fun getLatestFile(directory: String): String {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "ls -t $directory | head -1"))
        process.waitFor()
        val latestFile = process.inputStream.bufferedReader().use { reader ->
            reader.readText()
        }
        Log.d("HuntingMemory", "Latest file: ${latestFile.trim()}")
        return latestFile.trim()
    }

    fun filterMemInt(pid: Long, expectedValue: String): List<String> {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $binDirPath/filteroutput"))
        val filteroutpath = getLatestFile(filterdirPath)
        val filePath = if (currentFilteredAddresses.isNotEmpty() || filteroutpath.isNotEmpty()) {
            filteroutpath
        } else {
            searchOutputPath
        }
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem filter-int $pid $expectedValue $filePath $filterOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput2(command).toMutableList()
        return currentFilteredAddresses
    }

    fun filterMemLong(pid: Long, expectedValue: String): List<String> {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $binDirPath/filteroutput"))
        val filteroutpath = getLatestFile(filterdirPath)
        val filePath = if (currentFilteredAddresses.isNotEmpty() || filteroutpath.isNotEmpty()) {
            filteroutpath
        } else {
            searchOutputPath
        }
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem filter-long $pid $expectedValue $filePath $filterOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput2(command).toMutableList()
        return currentFilteredAddresses
    }

    fun filterMemFloat(pid: Long, expectedValue: String): List<String> {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $binDirPath/filteroutput"))
        val filteroutpath = getLatestFile(filterdirPath)
        val filePath = if (currentFilteredAddresses.isNotEmpty() || filteroutpath.isNotEmpty()) {
            filteroutpath
        } else {
            searchOutputPath
        }
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem filter-float $pid $expectedValue $filePath $filterOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput2(command).toMutableList()
        return currentFilteredAddresses
    }

    fun filterMemDouble(pid: Long, expectedValue: String): List<String> {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $binDirPath/filteroutput"))
        val filteroutpath = getLatestFile(filterdirPath)
        val filePath = if (currentFilteredAddresses.isNotEmpty() || filteroutpath.isNotEmpty()) {
            filteroutpath
        } else {
            searchOutputPath
        }
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem filter-double $pid $expectedValue $filePath $filterOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput2(command).toMutableList()
        return currentFilteredAddresses
    }

    fun filterMemIntGroup(pid: Long, expectedValue: String): List<Pair<String, String>> {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $binDirPath/filteroutput"))
        val filteroutpath = getLatestFile(filterdirPath)
        val filePath = if (currentFilteredAddresses2.isNotEmpty() || filteroutpath.isNotEmpty()) {
            filteroutpath
        } else {
            searchOutputPath
        }
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem filter-group-int $pid '$expectedValue' $filePath $filterOutputPath"
        currentFilteredAddresses2 = executeRootCommandWithOutput4(command).toMutableList()
        return currentFilteredAddresses2
    }

    fun filterMemLongGroup(pid: Long, expectedValue: String): List<Pair<String, String>> {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $binDirPath/filteroutput"))
        val filteroutpath = getLatestFile(filterdirPath)
        val filePath = if (currentFilteredAddresses2.isNotEmpty() || filteroutpath.isNotEmpty()) {
            filteroutpath
        } else {
            searchOutputPath
        }
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem filter-group-long $pid '$expectedValue' $filePath $filterOutputPath"
        currentFilteredAddresses2 = executeRootCommandWithOutput4(command).toMutableList()
        return currentFilteredAddresses2
    }

    fun filterMemFloatGroup(pid: Long, expectedValue: String): List<Pair<String, String>> {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $binDirPath/filteroutput"))
        val filteroutpath = getLatestFile(filterdirPath)
        val filePath = if (currentFilteredAddresses2.isNotEmpty() || filteroutpath.isNotEmpty()) {
            filteroutpath
        } else {
            searchOutputPath
        }
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem filter-group-float $pid '$expectedValue' $filePath $filterOutputPath"
        currentFilteredAddresses2 = executeRootCommandWithOutput4(command).toMutableList()
        return currentFilteredAddresses2
    }

    fun filterMemDoubleGroup(pid: Long, expectedValue: String): List<Pair<String, String>> {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $binDirPath/filteroutput"))
        val filteroutpath = getLatestFile(filterdirPath)
        val filePath = if (currentFilteredAddresses2.isNotEmpty() || filteroutpath.isNotEmpty()) {
            filteroutpath
        } else {
            searchOutputPath
        }
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem filter-group-double $pid '$expectedValue' $filePath $filterOutputPath"
        currentFilteredAddresses2 = executeRootCommandWithOutput4(command).toMutableList()
        return currentFilteredAddresses2
    }

    fun disassemble(pid: Long, addr: String): Boolean {
        ensureFileDeleted(disassembleOutputPath)
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem disassemble $pid $addr $disassembleOutputPath"
        val process = executeRootCommand(command)
        process?.waitFor()
        val file = File(disassembleOutputPath)
        if (file.exists()) {
            return true
        } else {
            return false
        }
    }

    fun writeassemble(pid: Long, addr: String, assemblycode: String, is64bits: Boolean) {
        val arg = if (is64bits) {
            "64"
        } else {
            "32"
        }
        val command = "LD_LIBRARY_PATH=$binDirPath .$binDirPath/RWMem write-assembly $pid $addr $assemblycode $arg"
        val process = executeRootCommand(command)
        process?.waitFor()
    }
}
