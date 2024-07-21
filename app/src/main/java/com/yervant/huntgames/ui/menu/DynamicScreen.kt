package com.yervant.huntgames.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

sealed class MenuComponent {
    data class Checkbox(val label: String, val checked: MutableState<Boolean>, var onCheck: ((Boolean) -> Unit)?) : MenuComponent()
    data class Slider(val value: MutableState<Float>, var onSlide: ((Float) -> Unit)?) : MenuComponent()
    data class TextField(val text: MutableState<String>, var onTextChange: ((String) -> Unit)?) : MenuComponent()
    data class Button(val label: String, val onClick: (() -> Unit)?) : MenuComponent()
    data class Text(val content: MutableState<String>) : MenuComponent()
}

class DynamicScreen {

    @Composable
    fun MenuTemplate(components: List<MenuComponent>, backgroundColor: State<Color>) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor.value)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            components.forEach { component ->
                when (component) {
                    is MenuComponent.Text -> TextComponent(component)
                    is MenuComponent.Checkbox -> CheckboxComponent(component)
                    is MenuComponent.Slider -> SliderComponent(component)
                    is MenuComponent.TextField -> TextFieldComponent(component)
                    is MenuComponent.Button -> ButtonComponent(component)
                }
            }
        }
    }

    @Composable
    fun CheckboxComponent(component: MenuComponent.Checkbox) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Checkbox(
                checked = component.checked.value,
                onCheckedChange = {
                    component.checked.value = it
                    component.onCheck?.invoke(it)
                }
            )
            Text(
                text = component.label,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    @Composable
    fun SliderComponent(component: MenuComponent.Slider) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Slider Value: ${component.value.value.toInt()}", modifier = Modifier.padding(bottom = 8.dp))
            Slider(
                value = component.value.value,
                onValueChange = {
                    component.value.value = it
                    component.onSlide?.invoke(it)
                },
                valueRange = 0f..100f
            )
        }
    }

    @Composable
    fun TextFieldComponent(component: MenuComponent.TextField) {
        TextField(
            value = component.text.value,
            onValueChange = {
                component.text.value = it
                component.onTextChange?.invoke(it)
            },
            label = { Text("Enter text") },
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }

    @Composable
    fun ButtonComponent(component: MenuComponent.Button) {
        Button(
            onClick = {
                component.onClick?.invoke()
            },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(component.label)
        }
    }

    @Composable
    fun TextComponent(component: MenuComponent.Text) {
        Text(
            text = component.content.value,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

object MenuManager {

    private val components = mutableStateListOf<MenuComponent>()
    private val backgroundColor = mutableStateOf(Color.Black)
    private var luaGlobals: Globals? = null

    fun getComponents() = components
    fun getBackgroundColor() = backgroundColor

    fun clearComponents() {
        components.clear()
    }

    fun setBackgroundColor(color: Color) {
        backgroundColor.value = color
    }

    fun addCheckboxComponent(label: String, checked: Boolean, onCheck: ((Boolean) -> Unit)?) {
        components.add(MenuComponent.Checkbox(label, mutableStateOf(checked), onCheck))
    }

    fun addSliderComponent(value: Float, onSlide: ((Float) -> Unit)?) {
        components.add(MenuComponent.Slider(mutableStateOf(value), onSlide))
    }

    fun addTextFieldComponent(text: String, onTextChange: ((String) -> Unit)?) {
        components.add(MenuComponent.TextField(mutableStateOf(text), onTextChange))
    }

    fun addButtonComponent(label: String, onClick: (() -> Unit)?) {
        components.add(MenuComponent.Button(label, onClick))
    }

    fun addTextComponent(content: String) {
        components.add(MenuComponent.Text(mutableStateOf(content)))
    }

    fun exportToLua(globals: Globals) {
        luaGlobals = globals

        globals["MenuManager_addCheckbox"] = object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val label = arg1.checkjstring()
                val checked = arg2.checkboolean()
                addCheckboxComponent(label, checked, null)
                return LuaValue.NIL
            }
        }

        globals["MenuManager_addSlider"] = object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val value = arg.optdouble(0.0).toFloat()
                addSliderComponent(value, null)
                return LuaValue.NIL
            }
        }

        globals["MenuManager_addTextField"] = object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val text = arg.optjstring(null)
                if (text != null) {
                    addTextFieldComponent(text, null)
                } else {
                    error("addTextFieldComponent requires text (string) as the argument.")
                }
                return LuaValue.NIL
            }
        }

        globals["MenuManager_addButton"] = object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val label = arg1.optjstring(null)
                val luaFunc = arg2.optfunction(null)
                if (label != null && luaFunc != null) {
                    addButtonComponent(label) {
                        luaFunc.call()
                    }
                } else {
                    error("addButtonComponent requires a label (string) and a callback function.")
                }
                return LuaValue.NIL
            }
        }

        globals["MenuManager_addText"] = object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val content = arg.optjstring(null)
                if (content != null) {
                    addTextComponent(content)
                } else {
                    error("addTextComponent requires content (string) as the argument.")
                }
                return LuaValue.NIL
            }
        }

        globals["MenuManager_setBackgroundColor"] = object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val color = arg.checkint()
                setBackgroundColor(Color(color))
                return LuaValue.NIL
            }
        }

        globals["MenuManager_setCheckboxCallback"] = object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val index = arg1.checkint() - 1
                val luaFunc = arg2.checkfunction()
                val component = components.getOrNull(index) as? MenuComponent.Checkbox
                component?.onCheck = { checked -> luaFunc.call(LuaValue.valueOf(checked)) }
                return LuaValue.NIL
            }
        }

        globals["MenuManager_setSliderCallback"] = object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val index = arg1.checkint() - 1
                val luaFunc = arg2.checkfunction()
                val component = components.getOrNull(index) as? MenuComponent.Slider
                component?.onSlide = { value -> luaFunc.call(LuaValue.valueOf(value.toDouble())) }
                return LuaValue.NIL
            }
        }

        globals["MenuManager_setTextFieldCallback"] = object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val index = arg1.checkint() - 1
                val luaFunc = arg2.checkfunction()
                val component = components.getOrNull(index) as? MenuComponent.TextField
                component?.onTextChange = { text -> luaFunc.call(LuaValue.valueOf(text)) }
                return LuaValue.NIL
            }
        }
    }
}