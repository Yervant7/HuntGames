package com.yervant.huntgames.backend

import android.util.Log
import java.io.File
import java.lang.Process
import java.nio.file.Files
import java.nio.file.Path

class HuntingMemory {

    private val binDirPath = "/data/data/com.yervant.huntgames/files"
    private val filterdirPath = "$binDirPath/filteroutput"
    private val searchOutputPath = "$binDirPath/searchoutput.txt"
    private val filterOutputPath = "$filterdirPath/filteroutput_${System.currentTimeMillis()}.txt"
    private val readOutputPath = "$binDirPath/readoutput.txt"
    private val writeOutputPath = "$binDirPath/writeoutput.txt"

    private var currentFilteredAddresses = mutableListOf<String>()

    private fun executeRootCommand(command: String): Process? {
        return Runtime.getRuntime().exec(arrayOf("su", "-c", command))
    }

    private fun executeRootCommandWithOutput(command: String, outputPath: String): List<String> {
        val output = mutableListOf<String>()
        try {
            val process = executeRootCommand(command)
            process?.waitFor()
            val file = File(outputPath)
            if (file.exists()) {
                output.addAll(file.readLines())
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
            val file = File(out)
            if (file.exists()) {
                output.addAll(file.readLines())
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

    fun searchInt(pid: Int, targetValue: Int, packageName: String): List<String> {
        ensureFileDeleted(searchOutputPath)
        val command = "cd $binDirPath && chmod +x ./RWMem && ./RWMem search-int $pid $targetValue $packageName $searchOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput(command, searchOutputPath).toMutableList()
        return currentFilteredAddresses
    }

    fun searchLong(pid: Int, targetValue: Long, packageName: String): List<String> {
        ensureFileDeleted(searchOutputPath)
        val command = "cd $binDirPath && chmod +x ./RWMem && ./RWMem search-long $pid $targetValue $packageName $searchOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput(command, searchOutputPath).toMutableList()
        return currentFilteredAddresses
    }

    fun searchFloat(pid: Int, targetValue: Float, packageName: String): List<String> {
        ensureFileDeleted(searchOutputPath)
        val command = "cd $binDirPath && chmod +x ./RWMem && ./RWMem search-float $pid $targetValue $packageName $searchOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput(command, searchOutputPath).toMutableList()
        return currentFilteredAddresses
    }

    fun readMemInt(pid: Int, addr: String): Int {
        val command = "cd $binDirPath && chmod +x ./RWMem && ./RWMem read $pid $addr 4 int > $readOutputPath"
        val output = executeRootCommand2(command, readOutputPath)
        return extractValue(output).toIntOrNull() ?: 0
    }

    fun readMemLong(pid: Int, addr: String): Long {
        val command = "cd $binDirPath && chmod +x ./RWMem && ./RWMem read $pid $addr 8 long > $readOutputPath"
        val output = executeRootCommand2(command, readOutputPath)
        return extractValue(output).toLongOrNull() ?: 0
    }

    fun readMemFloat(pid: Int, addr: String): Float {
        val command = "cd $binDirPath && chmod +x ./RWMem && ./RWMem read $pid $addr 4 float> $readOutputPath"
        val output = executeRootCommand2(command, readOutputPath)
        return extractValue(output).toFloatOrNull() ?: 0.0f
    }

    fun writeMemInt(pid: Int, addr: String, value: Int) {
        val command = "cd $binDirPath && chmod +x ./RWMem && ./RWMem write $pid $addr $value > $writeOutputPath"
        val output = executeRootCommand2(command, writeOutputPath)
        Log.d("HuntingMemory", "Write output: $output")
    }

    fun writeMemLong(pid: Int, addr: String, value: Long) {
        val command = "cd $binDirPath && chmod +x ./RWMem && ./RWMem write $pid $addr $value > $writeOutputPath"
        val output = executeRootCommand2(command, writeOutputPath)
        Log.d("HuntingMemory", "Write output: $output")
    }

    fun writeMemFloat(pid: Int, addr: String, value: Float) {
        val command = "cd $binDirPath && chmod +x ./RWMem && ./RWMem write $pid $addr $value > $writeOutputPath"
        val output = executeRootCommand2(command, writeOutputPath)
        Log.d("HuntingMemory", "Write output: $output")
    }

    fun getLatestFile(directory: String): String {
        var latestFile = ""
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "ls -t $directory | head -1"))
        process.waitFor()
        process.inputStream.bufferedReader().use { reader ->
            reader.forEachLine { line ->
                latestFile = line
            }
        }
        Log.d("HuntingMemory", "Latest file: $latestFile")
        return latestFile
    }

    fun filterMemInt(pid: Long, expectedValue: String): List<String> {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $binDirPath/filteroutput"))
        val filteroutpath = getLatestFile(filterdirPath).trim()
        val filePath = if (currentFilteredAddresses.isNotEmpty()) {
            filteroutpath
        } else {
            searchOutputPath
        }
        val command = "cd $binDirPath && chmod +x ./RWMem && ./RWMem filter-int $pid $expectedValue $filePath $filterOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput2(command).toMutableList()
        return currentFilteredAddresses
    }

    fun filterMemLong(pid: Long, expectedValue: String): List<String> {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $binDirPath/filteroutput"))
        val filteroutpath = getLatestFile(filterdirPath).trim()
        val filePath = if (currentFilteredAddresses.isNotEmpty()) {
            filteroutpath
        } else {
            searchOutputPath
        }
        val command = "cd $binDirPath && chmod +x ./RWMem && ./RWMem filter-long $pid $expectedValue $filePath $filterOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput2(command).toMutableList()
        return currentFilteredAddresses
    }

    fun filterMemFloat(pid: Long, expectedValue: String): List<String> {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $binDirPath/filteroutput"))
        val filteroutpath = getLatestFile(filterdirPath).trim()
        val filePath = if (currentFilteredAddresses.isNotEmpty()) {
            filteroutpath
        } else {
            searchOutputPath
        }
        val command = "cd $binDirPath && chmod +x ./RWMem && ./RWMem filter-float $pid $expectedValue $filePath $filterOutputPath"
        currentFilteredAddresses = executeRootCommandWithOutput2(command).toMutableList()
        return currentFilteredAddresses
    }
}
