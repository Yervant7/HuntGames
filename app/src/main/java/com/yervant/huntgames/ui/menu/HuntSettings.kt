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

private var regionsselected = ""

fun RegionSelected(): String {
    if (regionsselected == "") {
        regionsselected = "C_ALLOC,C_BSS,C_DATA,C_HEAP,JAVA_HEAP,A_ANONYMOUS,STACK,ASHMEM"
        return regionsselected
    } else {
        return regionsselected
    }
}

@Composable
fun SettingsMenu() {
    val words = listOf("C_ALLOC", "C_BSS", "C_DATA", "C_HEAP", "JAVA_HEAP", "A_ANONYMOUS", "STACK", "CODE_SYSTEM", "ASHMEM")
    val selectedWords = remember { mutableStateListOf<String>() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
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
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Button(
                onClick = {
                    val selectedWordsString = selectedWords.joinToString(separator = ",")
                    regionsselected = selectedWordsString
                },
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
@Preview
fun SettingsMenuPreview() {
    SettingsMenu()
}