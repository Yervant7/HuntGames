package com.yervant.huntgames.backend

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import com.topjohnwu.superuser.Shell
import com.yervant.huntgames.backend.Memory.Companion.matches
import com.yervant.huntgames.ui.menu.DynamicScreen
import com.yervant.huntgames.ui.menu.MatchInfo
import com.yervant.huntgames.ui.menu.MenuManager
import com.yervant.huntgames.ui.menu.isattached
import com.yervant.huntgames.ui.menu.setRegions
import com.yervant.huntgames.ui.menu.valtypeselected
import kotlinx.coroutines.runBlocking
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File


class LuaExecute {

    fun executelua(script: File, context: Context) {
        val globals = JsePlatform.standardGlobals()
        HG.exportToLua(globals, context)
        val chunk = globals.loadfile(script.absolutePath)
        chunk.call()
    }

    @Composable
    fun ExecuteLuaAndMenu(scriptFile: File, context: Context) {
        MenuManager.clearComponents()
        val globals = JsePlatform.standardGlobals()
        MenuManager.exportToLua(globals)
        HG.exportToLua(globals, context)
        val chunk = globals.loadfile(scriptFile.absolutePath)
        chunk.call()
        DynamicScreen().MenuTemplate()
    }

    fun getLuaFiles(): List<File> {
        val scriptFiles = mutableListOf<File>()
        val result = Shell.cmd("ls /data/data/com.yervant.huntgames/files").exec().out
        for (line in result) {
            line.split(" ")
                .map { it.trim().replace(Regex("\\p{C}|\\s+"), " ") }
                .filter { it.endsWith(".lua") }
                .forEach { file ->
                    val scriptFile = File("/data/data/com.yervant.huntgames/files/$file")
                    scriptFiles.add(scriptFile)
                    Log.i("LuaExecute", "Found Lua file: $file")
                }
        }
        return scriptFiles
    }
}

object HG {

    private var luaGlobals: Globals? = null

    fun exportToLua(globals: Globals, context: Context) {
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
                runBlocking {
                    Memory().gotoAddress(address, context)
                }
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
                runBlocking {
                    Memory().gotoAddressAndOffset(addr, offset, issub, context)
                }
                return LuaValue.NIL
            }
        }

        globals["HG_search_value"] = object : OneArgFunction() {
            override fun call(arg1: LuaValue): LuaValue {
                val value = arg1.checkjstring()
                val emptylist: List<MatchInfo> = mutableListOf()
                runBlocking {
                    Memory().scanAgainstValue(value, emptylist, context)
                }
                return LuaValue.NIL
            }
        }

        globals["HG_filter_value"] = object : OneArgFunction() {
            override fun call(arg1: LuaValue): LuaValue {
                val value = arg1.checkjstring()
                val list: List<MatchInfo> = matches
                runBlocking {
                    Memory().scanAgainstValue(value, list, context)
                }
                return LuaValue.NIL
            }
        }

        globals["HG_read_value"] = object : OneArgFunction() {
            override fun call(arg1: LuaValue): LuaValue {
                val addr = arg1.checkjstring()
                val pid = isattached().savepid()
                val cleaned_addr = addr.removePrefix("0x")
                val address = longArrayOf(cleaned_addr.toLong(16))
                val result = runBlocking {
                    HuntingMemory().readmem(pid, address, valtypeselected, context)
                }
                return LuaValue.valueOf(result[0])
            }
        }

        globals["HG_write_value"] = object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val value = arg1.checkjstring()
                val addr = arg2.checkjstring()
                val pid = isattached().savepid()
                val cleaned_addr = addr.removePrefix("0x")
                if (addr != null && value != null) {
                    val address = longArrayOf(cleaned_addr.toLong(16))
                    Hunt().writeValueAtAddress(pid, address, value, valtypeselected, context)
                }
                return LuaValue.NIL
            }
        }

        globals["HG_freeze_value"] = object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val value = arg1.checkjstring()
                val addr = arg2.checkjstring()
                val pid = isattached().savepid()
                val cleaned_addr = addr.removePrefix("0x")
                if (addr != null && value != null) {
                    val address = longArrayOf(cleaned_addr.toLong(16))
                    rwMem().freeze(pid, address, valtypeselected, value, context)
                }
                return LuaValue.NIL
            }
        }
    }
}
