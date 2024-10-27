package com.yervant.huntgames.backend

import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.yervant.huntgames.ui.menu.AddressInfo
import com.yervant.huntgames.ui.menu.savepid
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

    fun writeall(addrs: MutableList<AddressInfo>, value: String, overlayContext: OverlayContext) {
        val pid = savepid()
        var i = 0
        while (i < addrs.size) {
            writeValueAtAddress(pid, addrs[i].matchInfo.address, value, addrs[i].matchInfo.valuetype, overlayContext)
            i++
        }
    }

    fun freezeall(addrs: MutableList<AddressInfo>, value: String, overlayContext: OverlayContext) {
        val pid = savepid()
        var i = 0
        val freezelist: MutableList<FreezeInfo> = mutableListOf()
        while (i < addrs.size) {
            freezelist.add(FreezeInfo(addrs[i].matchInfo.address, value, addrs[i].matchInfo.valuetype))
            i++
        }
        freezeValuesAtAddresses(pid, freezelist, overlayContext)
    }

    fun writeValueAtAddress(pid: Long, addr: Long, value: String, valtype: String, overlayContext: OverlayContext) {
        if(valtype == "int") {
            writeValueAtAddressInt(pid, addr, value.toInt(), overlayContext)
        } else if (valtype == "long") {
            writeValueAtAddressLong(pid, addr, value.toLong(), overlayContext)
        } else if (valtype == "float") {
            writeValueAtAddressFloat(pid, addr, value.toFloat(), overlayContext)
        } else if (valtype == "double") {
            writeValueAtAddressDouble(pid, addr, value.toDouble(), overlayContext)
        }
    }

    private fun writeValueAtAddressInt(pid: Long, addr: Long, value: Int, overlayContext: OverlayContext) {
        val hunt = HuntingMemory()
        hunt.writeMemInt(pid, addr, value, overlayContext)
    }

    private fun writeValueAtAddressLong(pid: Long, addr: Long, value: Long, overlayContext: OverlayContext) {
        val hunt = HuntingMemory()
        hunt.writeMemLong(pid, addr, value, overlayContext)
    }

    private fun writeValueAtAddressFloat(pid: Long, addr: Long, value: Float, overlayContext: OverlayContext) {
        val hunt = HuntingMemory()
        hunt.writeMemFloat(pid, addr, value, overlayContext)
    }

    private fun writeValueAtAddressDouble(pid: Long, addr: Long, value: Double, overlayContext: OverlayContext) {
        val hunt = HuntingMemory()
        hunt.writeMemDouble(pid, addr, value, overlayContext)
    }

    data class FreezeInfo(val addr: Long, val value: String, val valtype: String)

    fun freezeValuesAtAddresses(pid: Long, freezeList: List<FreezeInfo>, overlayContext: OverlayContext) {
        setbool(true)
        CoroutineScope(Dispatchers.IO).launch {
            val hunt = HuntingMemory()
            while (bool) {
                for (freezeInfo in freezeList) {
                    val (addr, value, valtype) = freezeInfo
                    if (valtype == "int") {
                        val writeValue = if (value.isEmpty()) readValueAtAddressInt(pid, addr, overlayContext) else value.toInt()
                        hunt.writeMemInt(pid, addr, writeValue, overlayContext)
                    } else if (valtype == "long") {
                        val writeValue = if (value.isEmpty()) readValueAtAddressLong(pid, addr, overlayContext) else value.toLong()
                        hunt.writeMemLong(pid, addr, writeValue, overlayContext)
                    } else if (valtype == "float") {
                        val writeValue = if (value.isEmpty()) readValueAtAddressFloat(pid, addr, overlayContext) else value.toFloat()
                        hunt.writeMemFloat(pid, addr, writeValue, overlayContext)
                    } else if (valtype == "double") {
                        val writeValue = if (value.isEmpty()) readValueAtAddressDouble(pid, addr, overlayContext) else value.toDouble()
                        hunt.writeMemDouble(pid, addr, writeValue, overlayContext)
                    }
                }
                delay(150)
            }
        }
    }

    private suspend fun readValueAtAddressInt(pid: Long, addr: Long, overlayContext: OverlayContext): Int {
        val hunt = HuntingMemory()
        return hunt.readMemInt(pid, addr, overlayContext)
    }

    private suspend fun readValueAtAddressLong(pid: Long, addr: Long, overlayContext: OverlayContext): Long {
        val hunt = HuntingMemory()
        return hunt.readMemLong(pid, addr, overlayContext)
    }

    private suspend fun readValueAtAddressFloat(pid: Long, addr: Long, overlayContext: OverlayContext): Float {
        val hunt = HuntingMemory()
        return hunt.readMemFloat(pid, addr, overlayContext)
    }

    private suspend fun readValueAtAddressDouble(pid: Long, addr: Long, overlayContext: OverlayContext): Double {
        val hunt = HuntingMemory()
        return hunt.readMemDouble(pid, addr, overlayContext)
    }
}