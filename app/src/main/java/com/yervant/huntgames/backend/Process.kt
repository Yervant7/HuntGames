package com.yervant.huntgames.backend

import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.runtime.mutableStateListOf
import java.util.concurrent.TimeUnit

class Process {

    fun executeRootCommand(command: String): List<String> {
        val output = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.inputStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    output.add(line)
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return output
    }

    data class ProcessInfo(val pid: String, val packageName: String, val memory: String)

    fun getPackageNameByPid(pid: String): String {
        val commandOutput = executeRootCommand("cat /proc/$pid/cmdline")
        return if (commandOutput.isNotEmpty()) {
            val fullCommand = commandOutput[0]
            val packageName = fullCommand.split('\u0000')[0]
            if (packageName.contains("/")) {
                ""
            } else {
                packageName
            }
        } else {
            ""
        }
    }

    fun getRunningProcesses(): List<ProcessInfo> {
        val processes = mutableListOf<ProcessInfo>()
        val commandOutput = executeRootCommand("ps -e -o pid,rss,uid")

        for (line in commandOutput.drop(1)) {
            val tokens = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            val pid = tokens[0]
            val memory = tokens[1]
            val uid = tokens[2]
            val uidint = uid.toIntOrNull()
            if (uidint != null && uidint > 1000) {
                val packageName = getPackageNameByPid(pid)
                if (packageName.isNotEmpty()) {
                    processes.add(ProcessInfo(pid, packageName, memory))
                }
            }
        }
        return processes
    }
}
