package com.yervant.huntgames.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    val words = listOf(
        "C_ALLOC",
        "C_BSS",
        "C_DATA",
        "C_HEAP",
        "JAVA_HEAP",
        "A_ANONYMOUS",
        "STACK",
        "CODE_SYSTEM",
        "ASHMEM",
    )
    val selectedWords = remember { mutableStateListOf<String>() }
    val files = LuaExecute().getLuaFiles()
    val selectedFile = remember { mutableStateOf("") }
    val executeLua = remember { mutableStateOf(false) }
    val showDynamicInterface = remember { mutableStateOf(false) }

    files.forEach { file ->
        if (file.name == selectedFile.value) {
            if (file.readText().contains("MenuManager")) {
                showDynamicInterface.value = true
            }
        }
    }

    if (showDynamicInterface.value && executeLua.value && selectedFile.value.isNotEmpty()) {
        val file = File("/data/data/com.yervant.huntgames/files/${selectedFile.value}")
        LuaExecute().ExecuteLuaAndMenu(file, overlayContext!!)
    } else if (executeLua.value && selectedFile.value.isNotEmpty()) {
        val file = File("/data/data/com.yervant.huntgames/files/${selectedFile.value}")
        LuaExecute().executelua(file, overlayContext!!)
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
                    Button(
                        onClick = {
                            val selectedWordsString = selectedWords.joinToString(separator = ",")
                            regionsselected = selectedWordsString
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
                        executeLua.value = true
                    },
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text("Execute Python")
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