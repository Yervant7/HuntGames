package com.yervant.huntgames.ui.menu

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.yervant.huntgames.backend.Assemble
import com.yervant.huntgames.backend.AssemblyInfo
import com.yervant.huntgames.ui.util.CreateTable
import com.yervant.huntgames.ui.util.TextInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val currentAssemblyList = mutableStateListOf<AssemblyInfo>()

@Composable
fun DisassembleMenu(overlayContext: OverlayContext?, addr: String) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(addr) {
        updateAssemblyList(addr)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { contentPadding ->
            DisassembleMenuContent(
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                overlayContext = overlayContext,
                contentPadding = contentPadding,
                addr = addr
            )
        }
    )
}

@Composable
fun DisassembleMenuContent(
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    overlayContext: OverlayContext?,
    contentPadding: PaddingValues,
    addr: String
) {
    val selectedAssemblyInfo = remember { mutableStateOf<AssemblyInfo?>(null) }
    val is64BitGlobal = remember { mutableStateOf(false) }
    val editedAssemblyList = remember { mutableStateMapOf<AssemblyInfo, String>() }
    val listState = rememberLazyListState()

    // Scroll to the correct item when the list is updated
    LaunchedEffect(currentAssemblyList, addr) {
        coroutineScope.launch {
            delay(500)
            val scrollToIndex = currentAssemblyList.indexOfFirst { it.address == addr }
            if (scrollToIndex != -1) {
                listState.scrollToItem(scrollToIndex)
            }
        }
    }

    val content: @Composable (assemblyTableModifier: Modifier) -> Unit = { assemblyTableModifier ->
        DisassembleTable(
            modifier = assemblyTableModifier,
            list = currentAssemblyList,
            is64BitGlobal = is64BitGlobal.value,
            editedAssemblyList = editedAssemblyList,
            onCodeClicked = { assemblyInfo: AssemblyInfo ->
                selectedAssemblyInfo.value = assemblyInfo
            },
            onCodeChanged = { assemblyInfo, newCode ->
                editedAssemblyList[assemblyInfo] = newCode
            },
            onGlobalCheckedChange = { isChecked ->
                is64BitGlobal.value = isChecked
            },
            listState = listState
        )
    }

    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            content(
                assemblyTableModifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            )
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            content(
                assemblyTableModifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun DisassembleTable(
    modifier: Modifier = Modifier,
    list: List<AssemblyInfo>,
    is64BitGlobal: Boolean,
    editedAssemblyList: MutableMap<AssemblyInfo, String>,
    onCodeClicked: (assemblyInfo: AssemblyInfo) -> Unit,
    onCodeChanged: (assemblyInfo: AssemblyInfo, newCode: String) -> Unit,
    onGlobalCheckedChange: (Boolean) -> Unit,
    listState: LazyListState
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        // Global Checkbox
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(1.dp)) {
            Checkbox(
                checked = is64BitGlobal,
                onCheckedChange = onGlobalCheckedChange
            )
            Text(text = if (is64BitGlobal) "64-bit" else "32-bit")
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    editedAssemblyList.forEach { (assemblyInfo, newCode) ->
                        val assemble = Assemble()
                        assemble.writeAssembly(assemblyInfo.address, newCode, is64BitGlobal)
                    }
                    editedAssemblyList.clear()
                },
            ) {
                Text("Save All")
            }
        }
        CreateTable(
            colNames = listOf("Address", "Assembly Code"),
            colWeights = listOf(0.3f, 0.7f),
            itemCount = list.size,
            minEmptyItemCount = 10,
            onRowClicked = { rowIndex: Int ->
                onCodeClicked(list[rowIndex])
            },
            drawCell = { rowIndex: Int, colIndex: Int ->
                when (colIndex) {
                    0 -> Text(text = list[rowIndex].address)
                    1 -> TextInput(
                        textValue = editedAssemblyList[list[rowIndex]] ?: list[rowIndex].code,
                        onTextChange = { newCode ->
                            onCodeChanged(list[rowIndex], newCode)
                        },
                    )
                }
            },
            rowMinHeight = 20.dp,
            rowMaxHeight = 20.dp,
            lazyColumnState = listState
        )
    }
}

fun updateAssemblyList(addr: String) {
    val assemble = Assemble()
    currentAssemblyList.clear()
    currentAssemblyList.addAll(assemble.readListFile(addr))
}

@Composable
@Preview
fun DisassembleMenuPreview() {
    DisassembleMenu(null, "")
}
