package com.yervant.huntgames.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableIntStateOf
import com.kuhakupixel.libuberalles.overlay.OverlayContext
//import com.yervant.huntgames.backend.LuaExecute

var regionsselected = 0

@Composable
fun SettingsMenu(overlayContext: OverlayContext?) {
    val words = listOf(
        //"ALL",         //所有内存
        "RECOMMENDED",
        "B_BAD",       //B内存
        "C_ALLOC",     //Ca内存
        "C_BSS",       //Cb内存
        "C_DATA",      //Cd内存
        "C_HEAP",      //Ch内存
        "JAVA_HEAP",   //Jh内存
        "A_ANONMYOUS", //A内存
        "CODE_SYSTEM", //Xs内存 r-xp
        //CODE_APP     /data/ r-xp

        "STACK",       //S内存
        "ASHMEM",      //As内存
        "X",           //执行命令内存 r0xp
        "R0_0",        //可读非执行内存 r0_0
        "RW_0"
    )
    val selectedRegion = remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            itemsIndexed(words) { index, word ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedRegion.intValue == index,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                selectedRegion.intValue = index
                            } else if (selectedRegion.intValue == index) {
                                selectedRegion.intValue = 0
                            }
                        }
                    )
                    Text(text = "$index - $word")
                }
            }
            item {
                Button(
                    onClick = {
                        if (selectedRegion.intValue == 0) {
                            regionsselected = 1
                        } else {
                            regionsselected = selectedRegion.intValue + 1
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Save Region")
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