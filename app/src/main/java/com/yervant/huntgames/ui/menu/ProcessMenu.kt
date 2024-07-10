package com.yervant.huntgames.ui.menu

import com.kuhakupixel.libuberalles.overlay.OverlayContext
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import com.yervant.huntgames.R
import com.kuhakupixel.libuberalles.overlay.service.dialog.OverlayInfoDialog
import com.yervant.huntgames.backend.Process
import com.yervant.huntgames.backend.Process.ProcessInfo
import androidx.compose.runtime.LaunchedEffect
import kotlin.time.Duration.Companion.seconds
import androidx.compose.runtime.*
import com.yervant.huntgames.ui.util.CreateTable

private var attachedStatusString: MutableState<String> = mutableStateOf("None")
private var success: Boolean = false
private var svpid: Long = -1

class isattached {
    fun alert(): Boolean {
        return success
    }
    fun savepid(): Long {
        return if (svpid != -1L) svpid else -1
    }
}

fun AttachToProcess(
    pid: Long,
    onProcessNoExistAnymore: () -> Unit,
    onAttachSuccess: () -> Unit,
    onAttachFailure: (msg: String) -> Unit,
) {
    if (pid > 1000) {
        success = true
        onAttachSuccess()
    } else {
        onAttachFailure("Unexpected Error, cannot attach to $pid")
    }
}

@Composable
fun ProcessMenu(overlayContext: OverlayContext?) {
    val currentProcList = remember { mutableStateListOf<ProcessInfo>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            while (isActive) {
                refreshProcList(currentProcList)
                delay(60.seconds)
            }
        }
    }

    _ProcessMenu(
        runningProcState = currentProcList,
        onAttach = { pid: Long, procName: String, memory: String ->
            OverlayInfoDialog(overlayContext!!).show(
                title = "Attach to $pid - $procName?",
                text = "",
                onConfirm = {
                    AttachToProcess(
                        pid = pid,
                        onAttachSuccess = {
                            OverlayInfoDialog(overlayContext).show(
                                title = "Attaching to $procName is successful",
                                onConfirm = {},
                                text = "",
                            )
                            attachedStatusString.value = "$pid - $procName"
                            svpid = pid
                        },
                        onProcessNoExistAnymore = {
                            OverlayInfoDialog(overlayContext).show(
                                title = "Process $procName is not running anymore, Can't attach",
                                onConfirm = {},
                                text = "",
                            )
                        },
                        onAttachFailure = { msg: String ->
                            OverlayInfoDialog(overlayContext).show(
                                title = msg,
                                onConfirm = {},
                                text = "",
                            )
                        },
                    )
                },
            )
        },
        onRefreshClicked = { refreshProcList(currentProcList) },
    )
}

fun refreshProcList(processList: MutableList<Process.ProcessInfo>) {
    CoroutineScope(Dispatchers.IO).launch {
        val process = Process()
        val newProcesses = process.getRunningProcesses()

        withContext(Dispatchers.Main) {
            val existingPids = processList.map { it.pid }.toSet()
            val newPids = newProcesses.map { it.pid }.toSet()

            // Remove processes that are no longer running
            processList.removeAll { it.pid !in newPids }

            // Add new processes that are not already in the list
            newProcesses.filter { it.pid !in existingPids }.forEach {
                processList.add(it)
            }
        }
    }
}

@Composable
fun ProcessTable(
    processList: List<ProcessInfo>,
    onProcessSelected: (pid: Long, procName: String, memory: String) -> Unit,
) {
    CreateTable(
        modifier = Modifier.padding(16.dp),
        colNames = listOf("Pid", "Name", "Memory"),
        colWeights = listOf(0.2f, 0.5f, 0.3f),
        itemCount = processList.size,
        minEmptyItemCount = 50,
        onRowClicked = { rowIndex: Int ->
            onProcessSelected(
                processList[rowIndex].pid.toLong(),
                processList[rowIndex].packageName,
                processList[rowIndex].memory
            )
        },
        drawCell = { rowIndex: Int, colIndex: Int ->
            when (colIndex) {
                0 -> Text(text = processList[rowIndex].pid)
                1 -> Text(text = processList[rowIndex].packageName)
                2 -> {
                    val memoryInMB = processList[rowIndex].memory.toLong() / 1024
                    Text(text = "$memoryInMB MB")
                }
            }
        }
    )
}

@Composable
private fun _ProcessMenuContent(
    runningProcState: MutableList<ProcessInfo>,
    onRefreshClicked: () -> Unit,
    onAttach: (pid: Long, procName: String, memory: String) -> Unit,
    buttonContainer: @Composable (content: @Composable () -> Unit) -> Unit
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Text("Selected process: ${attachedStatusString.value}")
    }
    buttonContainer {
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Text("Selected process: ${attachedStatusString.value}")
        }
        Button(onClick = onRefreshClicked, modifier = Modifier.padding(start = 10.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_refresh),
                contentDescription = "Refresh",
            )
        }
    }
    ProcessTable(
        processList = runningProcState,
        onProcessSelected = onAttach,
    )
}

@Composable
private fun _ProcessMenu(
    runningProcState: MutableList<ProcessInfo>,
    onRefreshClicked: () -> Unit,
    onAttach: (pid: Long, procName: String, memory: String) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Column(modifier = Modifier.fillMaxSize()) {
                _ProcessMenuContent(
                    runningProcState = runningProcState,
                    onRefreshClicked = onRefreshClicked,
                    onAttach = onAttach,
                    buttonContainer = { content ->
                        Row(content = { content() })
                    }
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                _ProcessMenuContent(
                    runningProcState = runningProcState,
                    onRefreshClicked = onRefreshClicked,
                    onAttach = onAttach,
                    buttonContainer = { content ->
                        Column(content = { content() })
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTable() {
    val process = Process()
    ProcessTable(
        process.getRunningProcesses(),
    ) { pid: Long, procName: String, memory: String ->
        // do nothing
    }
}

@Composable
@Preview
fun ProcessMenuPreview() {
    ProcessMenu(null)
}
