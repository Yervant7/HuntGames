package com.yervant.huntgames.ui.menu

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.yervant.huntgames.backend.Assemble
import com.yervant.huntgames.backend.AssemblyInfo
import com.yervant.huntgames.ui.util.CreateTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val currentAssemblyList: MutableState<List<AssemblyInfo>> = mutableStateOf(mutableListOf())

@Composable
fun DisassembleMenu(overlayContext: OverlayContext?, addr: String) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(addr) {
        UpdateAssemblyList(addr)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { contentPadding ->
            _DisassembleMenu(
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                overlayContext = overlayContext,
                contentPadding = contentPadding
            )
        }
    )
}

@Composable
fun _DisassembleMenu(
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    overlayContext: OverlayContext?,
    contentPadding: PaddingValues
) {
    val showDialog = remember { mutableStateOf(false) }
    val selectedAssemblyInfo = remember { mutableStateOf<AssemblyInfo?>(null) }
    val newCode = remember { mutableStateOf("") }
    val is64Bit = remember { mutableStateOf(false) }

    val content: @Composable (assemblyTableModifier: Modifier) -> Unit =
        { assemblyTableModifier ->
            DisassembleTable(
                modifier = assemblyTableModifier,
                list = currentAssemblyList.value,
                onCodeClicked = { assemblyInfo: AssemblyInfo ->
                    selectedAssemblyInfo.value = assemblyInfo
                    newCode.value = assemblyInfo.code
                    is64Bit.value = false
                    showDialog.value = true
                },
            )
        }

    if (showDialog.value && selectedAssemblyInfo.value != null) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Edit Assembly Code") },
            text = {
                Column {
                    Text("Address: ${selectedAssemblyInfo.value!!.address}\nCheck the checkbox if the code is 64-bit")
                    TextField(
                        value = newCode.value,
                        onValueChange = { newCode.value = it },
                        label = { Text("Assembly Code") }
                    )
                    Checkbox(
                        checked = is64Bit.value,
                        onCheckedChange = { isChecked ->
                            is64Bit.value = isChecked
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedAssemblyInfo.value?.let {
                            val assemble = Assemble()
                            assemble.writeAssembly(selectedAssemblyInfo.value!!.address, newCode.value, is64Bit.value)
                            UpdateAssemblyList(selectedAssemblyInfo.value!!.address)
                        }
                        showDialog.value = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Code updated successfully",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text("Cancel")
                }
            }
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

fun UpdateAssemblyList(addr: String) {
    val assemble = Assemble()
    currentAssemblyList.value = assemble.readListFile(addr)
}

@Composable
private fun DisassembleTable(
    modifier: Modifier = Modifier,
    list: List<AssemblyInfo>,
    onCodeClicked: (assemblyInfo: AssemblyInfo) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    1 -> Text(text = list[rowIndex].code)
                }
            }
        )
    }
}

@Composable
@Preview
fun DisassembleMenuPreview() {
    DisassembleMenu(null, "")
}

