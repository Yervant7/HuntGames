package com.yervant.huntgames.backend

import com.topjohnwu.superuser.Shell

class Process {

    data class ProcessInfo(val pid: String, val packageName: String, val memory: String)

    private fun getPackageNameByPid(pid: String): String {
        val commandOutput = Shell.cmd("cat /proc/$pid/cmdline").exec().out.firstOrNull() ?: return ""
        val packageName = commandOutput.split('\u0000')[0]
        return if ("/" in packageName) "" else packageName
    }

    fun getRunningProcesses(): List<ProcessInfo> {
        val processes = mutableListOf<ProcessInfo>()
        val commandOutput = Shell.cmd("ps -e -o pid,rss,uid").exec().out.drop(1)

        commandOutput.mapNotNull { line ->
            val tokens = line.split(Regex("\\s+"))
            if (tokens.size >= 3) {
                val (pid, memory, uid) = tokens.take(3)
                val uidInt = uid.toIntOrNull()
                if (uidInt != null && uidInt > 1000) {
                    val packageName = getPackageNameByPid(pid)
                    if (packageName.isNotEmpty()) ProcessInfo(pid, packageName, memory) else null
                } else null
            } else null
        }.toCollection(processes)

        return processes
    }
}