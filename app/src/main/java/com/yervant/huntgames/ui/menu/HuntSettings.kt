package com.yervant.huntgames.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.yervant.huntgames.backend.LuaExecute
import java.io.File

private var regionsselected = ""

fun RegionSelected(): String {
    if (regionsselected == "") {
        regionsselected = "C_ALLOC,C_BSS,C_DATA,C_HEAP,JAVA_HEAP,A_ANONYMOUS,STACK,ASHMEM"
        return regionsselected
    } else {
        return regionsselected
    }
}

fun setRegions(regions: String) {
    regionsselected = regions
}

@Composable
fun SettingsMenu(overlayContext: OverlayContext?) {
    val words = listOf("C_ALLOC", "C_BSS", "C_DATA", "C_HEAP", "JAVA_HEAP", "A_ANONYMOUS", "STACK", "CODE_SYSTEM", "ASHMEM")
    val selectedWords = remember { mutableStateListOf<String>() }
    val customRegion = remember { mutableStateOf("") }
    val files = LuaExecute().getLuaFiles()
    val selectedFile = remember { mutableStateOf("") }
    val executeKts = remember { mutableStateOf(false) }
    val showDynamicInterface = remember { mutableStateOf(false) }

    files.forEach { file ->
        if (file.name == selectedFile.value) {
            val res = file.readText()
            if (res.contains("MenuManager"))
                showDynamicInterface.value = true
        }
    }

    if (showDynamicInterface.value && executeKts.value && selectedFile.value.isNotEmpty()) {
        val file = File("/data/data/com.yervant.huntgames/files/${selectedFile.value}")
        LuaExecute().ExecuteLuaAndMenu(file)
    } else if (executeKts.value && selectedFile.value.isNotEmpty()) {
        val file = File("/data/data/com.yervant.huntgames/files/${selectedFile.value}")
        LuaExecute().executelua(file)
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                items(words) { word ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedWords.contains(word),
                            onCheckedChange = {
                                if (it) {
                                    selectedWords.add(word)
                                } else {
                                    selectedWords.remove(word)
                                }
                            }
                        )
                        Text(text = word)
                    }
                }

                item {
                    TextField(
                        value = customRegion.value,
                        onValueChange = { customRegion.value = it },
                        label = { Text("Custom Region") },
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(vertical = 8.dp)
                    )
                }
                item {
                    Button(
                        onClick = {
                            val selectedWordsString = selectedWords.joinToString(separator = ",")
                            if (customRegion.value.isNotBlank()) {
                                regionsselected = customRegion.value
                            } else {
                                regionsselected = selectedWordsString
                            }
                        },
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text("Save Regions")
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(16.dp)
            ) {
                items(files) { file ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedFile.value == file.name,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    selectedFile.value = file.name
                                } else if (selectedFile.value == file.name) {
                                    selectedFile.value = ""
                                }
                            }
                        )
                        Text(text = file.name)
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Button(
                    onClick = {
                        executeKts.value = true
                    },
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text("Execute Lua")
                }
            }
        }
    }
}

@Composable
@Preview
fun SettingsMenuPreview() {
    SettingsMenu(null)
}