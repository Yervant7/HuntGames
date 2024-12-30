package com.yervant.huntgames.backend

import android.content.Context
import com.yervant.huntgames.ui.menu.AddressInfo
import com.yervant.huntgames.ui.menu.isattached
import com.yervant.huntgames.ui.menu.valtypeselected

class Hunt {

    fun writeall(addrs: MutableList<AddressInfo>, value: String, context: Context) {
        val pid = isattached().savepid()
        val addresses = LongArray(addrs.size)
        var i = 0
        while (i < addresses.size) {
            addresses[i] = addrs[i].matchInfo.address
            i++
        }
        writeValueAtAddress(pid, addresses, value, valtypeselected, context)

    }

    fun unfreezeall(context: Context) {
        val rwmem = rwMem()
        rwmem.stopFreeze(context)
    }

    fun freezeall(addrs: MutableList<AddressInfo>, value: String, context: Context) {
        val pid = isattached().savepid()
        val rwmem = rwMem()
        var i = 0
        val addresses = LongArray(addrs.size)

        while (i < addrs.size) {
            addresses[i] = addrs[i].matchInfo.address
            i++
        }
        rwmem.stopFreeze(context)
        rwmem.freeze(pid, addresses, valtypeselected, value, context)
    }

    fun writeValueAtAddress(pid: Long, addrs: LongArray, value: String, valtype: String, context: Context) {
        val hunt = HuntingMemory()
        if(valtype == "int") {
            hunt.writemem(pid, addrs, "int", value, context)
        } else if (valtype == "long") {
            hunt.writemem(pid, addrs, "long", value, context)
        } else if (valtype == "float") {
            hunt.writemem(pid, addrs, "float", value, context)
        } else if (valtype == "double") {
            hunt.writemem(pid, addrs, "double", value, context)
        }
    }
}