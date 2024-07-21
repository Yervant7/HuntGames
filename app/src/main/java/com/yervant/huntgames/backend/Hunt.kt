package com.yervant.huntgames.backend

import com.yervant.huntgames.ui.menu.AddressInfo
import com.yervant.huntgames.ui.menu.isattached
import com.yervant.huntgames.ui.menu.valtypeselected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Hunt {

    companion object {
        var bool = false
    }

    fun setbool(b: Boolean) {
        bool = b
    }

    fun writeall(addrs: MutableList<AddressInfo>, value: String) {
        val pid = isattached().savepid()
        var i = 0
        while (i < addrs.size) {
            writeValueAtAddress(pid, addrs[i].matchInfo.address, value, valtypeselected)
            i++
        }
    }

    fun freezeall(addrs: MutableList<AddressInfo>, value: String) {
        val pid = isattached().savepid()
        var i = 0
        val freezelist: MutableList<FreezeInfo> = mutableListOf()
        while (i < addrs.size) {
            freezelist.add(FreezeInfo(addrs[i].matchInfo.address, value, valtypeselected))
            i++
        }
        freezeValuesAtAddresses(pid, freezelist)
    }

    fun writeValueAtAddress(pid: Long, addr: String, value: String, valtype: String) {
        if(valtype == "int") {
            writeValueAtAddressInt(pid, addr, value.toInt())
        } else if (valtype == "long") {
            writeValueAtAddressLong(pid, addr, value.toLong())
        } else if (valtype == "float") {
            writeValueAtAddressFloat(pid, addr, value.toFloat())
        } else if (valtype == "double") {
            writeValueAtAddressDouble(pid, addr, value.toDouble())
        }
    }

    private fun writeValueAtAddressInt(pid: Long, addr: String, value: Int) {
        val hunt = HuntingMemory()
        hunt.writeMemInt(pid, addr, value)
    }

    private fun writeValueAtAddressLong(pid: Long, addr: String, value: Long) {
        val hunt = HuntingMemory()
        hunt.writeMemLong(pid, addr, value)
    }

    private fun writeValueAtAddressFloat(pid: Long, addr: String, value: Float) {
        val hunt = HuntingMemory()
        hunt.writeMemFloat(pid, addr, value)
    }

    private fun writeValueAtAddressDouble(pid: Long, addr: String, value: Double) {
        val hunt = HuntingMemory()
        hunt.writeMemDouble(pid, addr, value)
    }

    data class FreezeInfo(val addr: String, val value: String, val valtype: String)

    fun freezeValuesAtAddresses(pid: Long, freezeList: List<FreezeInfo>) {
        setbool(true)
        CoroutineScope(Dispatchers.IO).launch {
            val hunt = HuntingMemory()
            while (bool) {
                for (freezeInfo in freezeList) {
                    val (addr, value, valtype) = freezeInfo
                    if (valtype == "int") {
                        val writeValue = if (value.isEmpty()) readValueAtAddressInt(pid, addr) else value.toInt()
                        hunt.writeMemInt(pid, addr, writeValue)
                    } else if (valtype == "long") {
                        val writeValue = if (value.isEmpty()) readValueAtAddressLong(pid, addr) else value.toLong()
                        hunt.writeMemLong(pid, addr, writeValue)
                    } else if (valtype == "float") {
                        val writeValue = if (value.isEmpty()) readValueAtAddressFloat(pid, addr) else value.toFloat()
                        hunt.writeMemFloat(pid, addr, writeValue)
                    } else if (valtype == "double") {
                        val writeValue = if (value.isEmpty()) readValueAtAddressDouble(pid, addr) else value.toDouble()
                        hunt.writeMemDouble(pid, addr, writeValue)
                    }
                }
                delay(150)
            }
        }
    }

    private fun readValueAtAddressInt(pid: Long, addr: String): Int {
        val hunt = HuntingMemory()
        return hunt.readMemInt(pid, addr)
    }

    private fun readValueAtAddressLong(pid: Long, addr: String): Long {
        val hunt = HuntingMemory()
        return hunt.readMemLong(pid, addr)
    }

    private fun readValueAtAddressFloat(pid: Long, addr: String): Float {
        val hunt = HuntingMemory()
        return hunt.readMemFloat(pid, addr)
    }

    private fun readValueAtAddressDouble(pid: Long, addr: String): Double {
        val hunt = HuntingMemory()
        return hunt.readMemDouble(pid, addr)
    }
}