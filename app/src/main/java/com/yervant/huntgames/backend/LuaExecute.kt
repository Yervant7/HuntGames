package com.yervant.huntgames.backend

import android.util.Log
import androidx.compose.runtime.Composable
import com.yervant.huntgames.backend.Hunt.FreezeInfo
import com.yervant.huntgames.ui.menu.DynamicScreen
import com.yervant.huntgames.ui.menu.MatchInfo
import com.yervant.huntgames.ui.menu.MenuManager
import com.yervant.huntgames.ui.menu.savepid
import com.yervant.huntgames.ui.menu.valtypeselected
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File

/*
class LuaExecute {

    fun executelua(script: File) {
        val globals = JsePlatform.standardGlobals()
        HG.exportToLua(globals)
        val chunk = globals.loadfile(script.absolutePath)
        chunk.call()
    }

    @Composable
    fun ExecuteLuaAndMenu(scriptFile: File) {
        MenuManager.clearComponents()
        val globals = JsePlatform.standardGlobals()
        MenuManager.exportToLua(globals)
        HG.exportToLua(globals)
        val chunk = globals.loadfile(scriptFile.absolutePath)
        chunk.call()
        DynamicScreen().MenuTemplate(MenuManager.getComponents(), MenuManager.getBackgroundColor())
    }

    private fun executeRootCommand(command: String): List<String> {
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

    fun getLuaFiles(): List<File> {
        val ktsFiles = mutableListOf<File>()
        val result = executeRootCommand("ls /data/data/com.yervant.huntgames/files")
        for (file in result) {
            if (file.contains(".lua")) {
                val ktsfile = File("/data/data/com.yervant.huntgames/files/$file")
                ktsFiles.add(ktsfile)
                Log.d("=====LUA====", "file: $file")
            }
        }
        return ktsFiles
    }
}

object HG {

    private var luaGlobals: Globals? = null

    fun exportToLua(globals: Globals) {
        luaGlobals = globals

        globals["HG_set_regions"] = object : OneArgFunction() {
            override fun call(arg1: LuaValue): LuaValue {
                val regions = arg1.checkjstring()
                setRegions(regions)
                return LuaValue.NIL
            }
        }
        globals["HG_set_valuetype"] = object : OneArgFunction() {
            override fun call(arg1: LuaValue): LuaValue {
                val valtype = arg1.checkjstring()
                valtypeselected = valtype
                return LuaValue.NIL
            }
        }
        globals["HG_goto_address"] = object : OneArgFunction() {
            override fun call(arg1: LuaValue): LuaValue {
                val address = arg1.checkjstring()
                Memory().gotoAddress(address)
                return LuaValue.NIL
            }
        }
        globals["HG_goto_address_offset"] = object : OneArgFunction() {
            override fun call(arg1: LuaValue): LuaValue {
                val addressAndOffset = arg1.checkjstring()
                val values = if (addressAndOffset.contains("-")) {
                    addressAndOffset.split("-")
                } else {
                    addressAndOffset.split("+")
                }
                val issub = addressAndOffset.contains("-")
                val addr = values[0]
                val offset = values[1]
                Memory().gotoAddressAndOffset(addr, offset, issub)
                return LuaValue.NIL
            }
        }
        globals["HG_search_value"] = object : OneArgFunction() {
            override fun call(arg1: LuaValue): LuaValue {
                val value = arg1.checkjstring()
                val emptylist: List<MatchInfo> = mutableListOf()
                Memory().scanAgainstValue(value, emptylist)
                return LuaValue.NIL
            }
        }
        globals["HG_read_value"] = object : OneArgFunction() {
            override fun call(arg1: LuaValue): LuaValue {
                val addr = arg1.checkjstring()
                val pid = savepid()
                return when (valtypeselected) {
                    "int" -> LuaValue.valueOf(HuntingMemory().readMemInt(pid, addr))
                    "long" -> LuaValue.valueOf(HuntingMemory().readMemLong(pid, addr).toString())
                    "float" -> LuaValue.valueOf(HuntingMemory().readMemFloat(pid, addr).toDouble())
                    "double" -> LuaValue.valueOf(HuntingMemory().readMemDouble(pid, addr))
                    else -> LuaValue.NIL
                }
            }
        }
        globals["HG_write_value"] = object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val value = arg1.checkjstring()
                val addr = arg2.checkjstring()
                val pid = savepid()
                if (addr != null && value != null) {
                    Hunt().writeValueAtAddress(pid, addr, value, valtypeselected)
                }
                return LuaValue.NIL
            }
        }
        globals["HG_freeze_value"] = object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val value = arg1.checkjstring()
                val addr = arg2.checkjstring()
                val pid = savepid()
                if (addr != null && value != null) {
                    val freezelist: MutableList<FreezeInfo> = mutableListOf()
                    freezelist.add(FreezeInfo(addr, value, valtypeselected))
                    Hunt().freezeValuesAtAddresses(pid, freezelist)
                }
                return LuaValue.NIL
            }
        }
    }
}

*/