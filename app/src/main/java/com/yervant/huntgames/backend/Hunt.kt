package com.yervant.huntgames.backend

import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.yervant.huntgames.ui.menu.AddressInfo
import com.yervant.huntgames.ui.menu.isattached
import com.yervant.huntgames.ui.menu.valtypeselected

class Hunt {

    companion object {
        var bool = false
    }

    fun setbool(b: Boolean) {
        bool = b
    }

    fun writeall(addrs: MutableList<AddressInfo>, value: String, overlayContext: OverlayContext) {
        val pid = isattached().savepid()
        val addresses = LongArray(addrs.size)
        var i = 0
        while (i < addresses.size) {
            addresses[i] = addrs[i].matchInfo.address
            i++
        }
        writeValueAtAddress(pid, addresses, value, valtypeselected, overlayContext)

    }

    fun writeValueAtAddress(pid: Long, addrs: LongArray, value: String, valtype: String, overlayContext: OverlayContext) {
        if(valtype == "int") {
            writeValueAtAddressInt(pid, addrs, value.toInt(), overlayContext)
        } else if (valtype == "long") {
            writeValueAtAddressLong(pid, addrs, value.toLong(), overlayContext)
        } else if (valtype == "float") {
            writeValueAtAddressFloat(pid, addrs, value.toFloat(), overlayContext)
        } else if (valtype == "double") {
            writeValueAtAddressDouble(pid, addrs, value.toDouble(), overlayContext)
        }
    }

    private fun writeValueAtAddressInt(pid: Long, addr: LongArray, value: Int, overlayContext: OverlayContext) {
        val hunt = HuntingMemory()
        hunt.writeMemInt(pid, addr, value, overlayContext)
    }

    private fun writeValueAtAddressLong(pid: Long, addr: LongArray, value: Long, overlayContext: OverlayContext) {
        val hunt = HuntingMemory()
        hunt.writeMemLong(pid, addr, value, overlayContext)
    }

    private fun writeValueAtAddressFloat(pid: Long, addr: LongArray, value: Float, overlayContext: OverlayContext) {
        val hunt = HuntingMemory()
        hunt.writeMemFloat(pid, addr, value, overlayContext)
    }

    private fun writeValueAtAddressDouble(pid: Long, addr: LongArray, value: Double, overlayContext: OverlayContext) {
        val hunt = HuntingMemory()
        hunt.writeMemDouble(pid, addr, value, overlayContext)
    }
}