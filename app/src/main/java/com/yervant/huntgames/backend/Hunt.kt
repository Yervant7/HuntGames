package com.yervant.huntgames.backend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.yervant.huntgames.ui.menu.refreshvalue
import com.yervant.huntgames.ui.menu.valtypeselected
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class Hunt {

    companion object {
        var bool = false
    }

    fun setbool(b: Boolean) {
        bool = b
    }

    fun writeValueAtAddress(pid: Long, addr: String, value: String) {
        if(valtypeselected == "int") {
            writeValueAtAddressInt(pid.toInt(), addr, value.toInt())
        } else if (valtypeselected == "long") {
            writeValueAtAddressLong(pid.toInt(), addr, value.toLong())
        } else if (valtypeselected == "float") {
            writeValueAtAddressFloat(pid.toInt(), addr, value.toFloat())
        }
    }

    private fun writeValueAtAddressInt(pid: Int, addr: String, value: Int) {
        val hunt = HuntingMemory()
        hunt.writeMemInt(pid, addr, value)
    }

    private fun writeValueAtAddressLong(pid: Int, addr: String, value: Long) {
        val hunt = HuntingMemory()
        hunt.writeMemLong(pid, addr, value)
    }

    private fun writeValueAtAddressFloat(pid: Int, addr: String, value: Float) {
        val hunt = HuntingMemory()
        hunt.writeMemFloat(pid, addr, value)
    }

    fun freezeValueAtAddress(pid: Long, addr: String, value: String) {
        val hunt = HuntingMemory()
        var currentValue: Int
        var currentValue2: Long
        var currentValue3: Float

        while (bool) {
            if(valtypeselected == "int") {
                if (value.isEmpty()) {
                    currentValue = readValueAtAddressInt(pid.toInt(), addr)
                    hunt.writeMemInt(pid.toInt(), addr, currentValue)
                    Thread.sleep(1000)
                } else {
                    val newValue = value.toInt()
                    hunt.writeMemInt(pid.toInt(), addr, newValue)
                    Thread.sleep(1000)
                }
                
            } else if (valtypeselected == "long") {
                if (value.isEmpty()) {
                    currentValue2 = readValueAtAddressLong(pid.toInt(), addr)
                    hunt.writeMemLong(pid.toInt(), addr, currentValue2)
                    Thread.sleep(1000)
                } else {
                    val newValue = value.toLong()
                    hunt.writeMemLong(pid.toInt(), addr, newValue)
                    Thread.sleep(1000)
                }
            } else if (valtypeselected == "float") {
                if (value.isEmpty()) {
                    currentValue3 = readValueAtAddressFloat(pid.toInt(), addr)
                    hunt.writeMemFloat(pid.toInt(), addr, currentValue3)
                    Thread.sleep(1000)
                } else {
                    val newValue = value.toFloat()
                    hunt.writeMemFloat(pid.toInt(), addr, newValue)
                    Thread.sleep(1000)
                }
            }
        }
    }

    private fun readValueAtAddressInt(pid: Int, addr: String): Int {
        val hunt = HuntingMemory()
        return hunt.readMemInt(pid, addr)
    }

    private fun readValueAtAddressLong(pid: Int, addr: String): Long {
        val hunt = HuntingMemory()
        return hunt.readMemLong(pid, addr)
    }

    private fun readValueAtAddressFloat(pid: Int, addr: String): Float {
        val hunt = HuntingMemory()
        return hunt.readMemFloat(pid, addr)
    }
}