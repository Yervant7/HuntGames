package com.yervant.huntgames.backend

import com.topjohnwu.superuser.Shell

class Process {

    data class ProcessInfo(val pid: String, val packageName: String, val memory: String)

    private fun getPackageNameByPid(pid: String): String {
        val commandOutput = Shell.cmd("cat /proc/$pid/cmdline").exec().out
        if (commandOutput.isNotEmpty()) {
            val packageName = commandOutput[0].split('\u0000')[0]
            return packageName
        } else { return "" }
    }

    fun getRunningProcesses(): List<ProcessInfo> {
        val processes = mutableListOf<ProcessInfo>()
        val commandOutput = Shell.cmd("ps -e -o pid,rss").exec().out.drop(1)

        commandOutput.mapNotNull { line ->
            val tokens = line.split(Regex("\\s+"))
            if (tokens.size >= 2) {
                val (pid, memory) = tokens.take(2)
                val packageName = getPackageNameByPid(pid)
                if (packageName.isNotEmpty() && !packageName.contains("/")) {
                    ProcessInfo(pid, packageName, memory)
                } else null
            } else null
        }.toCollection(processes)

        return processes
    }
}