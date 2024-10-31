package com.yervant.huntgames.backend

import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.yervant.huntgames.ui.menu.AddressInfo
import com.yervant.huntgames.ui.menu.isattached
import com.yervant.huntgames.ui.menu.valtypeselected

class Hunt {

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
        val hunt = HuntingMemory()
        if(valtype == "int") {
            hunt.writemem(pid, addrs, "int", value, overlayContext)
        } else if (valtype == "long") {
            hunt.writemem(pid, addrs, "long", value, overlayContext)
        } else if (valtype == "float") {
            hunt.writemem(pid, addrs, "float", value, overlayContext)
        } else if (valtype == "double") {
            hunt.writemem(pid, addrs, "double", value, overlayContext)
        }
    }
}