package com.yervant.huntgames.backend

import com.yervant.huntgames.ui.menu.isattached
import java.io.File

private val fileName = "/data/data/com.yervant.huntgames/files/bin/disassembleoutput.txt"

data class AssemblyInfo(val address: String, var code: String)

class Assemble {

    fun readListFile(address: String): List<AssemblyInfo> {
        val hunt = HuntingMemory()
        val pid = isattached().savepid()
        if (address.isEmpty()) {
            throw Exception("Address is empty")
        }
        val result = hunt.disassemble(pid, address)
        if (!result) {
            throw Exception("Failed to disassemble")
        }
        val file = File(fileName)
        val list = mutableListOf<AssemblyInfo>()

        if (file.exists()) {
            file.forEachLine { line ->
                val parts = line.split(": ")
                if (parts.size >= 2) {
                    val addr = parts[0]
                    val code = parts[1]
                    list.add(AssemblyInfo(addr, code))
                }
            }
        }
        return list
    }

    fun writeAssembly(addr: String, assemblycode: String, is64bits: Boolean) {

        val pid = isattached().savepid()
        val hunt = HuntingMemory()
        hunt.writeassemble(pid, addr, assemblycode, is64bits)
    }

}