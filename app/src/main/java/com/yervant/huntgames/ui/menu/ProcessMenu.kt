package com.yervant.huntgames.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yervant.huntgames.R
import com.yervant.huntgames.backend.Process
import com.yervant.huntgames.backend.Process.ProcessInfo
import com.yervant.huntgames.ui.DialogCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

private var attachedStatusString: MutableState<String> = mutableStateOf("None")
private var success: Boolean = false
private var svpid: Long = -1L

class isattached {
    fun alert(): Boolean {
        return success
    }
    fun savepid(): Long {
        return if (svpid != -1L) svpid else -1
    }
    fun reset() {
        svpid = -1L
        success = false
    }
}

fun AttachToProcess(
    pid: Long,
    onProcessNoExistAnymore: () -> Unit,
    onAttachSuccess: () -> Unit,
    onAttachFailure: (msg: String) -> Unit,
) {
    if (Process().processIsRunning(pid.toString())) {
        success = true
        onAttachSuccess()
    } else {
        onProcessNoExistAnymore()
    }
}

@Composable
fun ProcessMenu(dialogCallback: DialogCallback) {
    val currentProcList = remember { mutableStateListOf<ProcessInfo>() }
    val searchQuery = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    fun refreshProcList() {
        coroutineScope.launch(Dispatchers.IO) {
            val newProcesses = Process().getRunningProcesses()
            withContext(Dispatchers.Main) {
                currentProcList.clear()
                currentProcList.addAll(newProcesses)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            refreshProcList()
            delay(60.seconds)
        }
    }

    val filteredProcList = remember(searchQuery.value, currentProcList) {
        if (searchQuery.value.isBlank()) currentProcList
        else currentProcList.filter { it.packageName.contains(searchQuery.value, ignoreCase = true) }
    }

    _ProcessMenu(
        runningProcState = filteredProcList,
        searchQuery = searchQuery,
        onAttach = { pid, procName, memory ->
            dialogCallback.showInfoDialog(
                title = "Hunt Games",
                message = "Attach to $pid - $procName?",
                onConfirm = {
                    AttachToProcess(
                        pid = pid.toLong(),
                        onAttachSuccess = {
                            dialogCallback.showInfoDialog(
                                title = "Hunt Games",
                                message = "Attaching to $procName is successful",
                                onConfirm = {},
                                onDismiss = {}
                            )
                            attachedStatusString.value = "$pid - $procName"
                            svpid = pid.toLong()
                        },
                        onProcessNoExistAnymore = {
                            dialogCallback.showInfoDialog(
                                title = "Hunt Games",
                                message = "Process $procName is not running anymore, Can't attach",
                                onConfirm = {},
                                onDismiss = {}
                            )
                        },
                        onAttachFailure = { msg ->
                            dialogCallback.showInfoDialog(
                                title = "Hunt Games",
                                message = msg,
                                onConfirm = {},
                                onDismiss = {}
                            )
                        }
                    )
                },
                onDismiss = {}
            )
        },
        onRefreshClicked = { refreshProcList() },
    )
}

@Composable
private fun _ProcessMenu(
    runningProcState: List<ProcessInfo>,
    searchQuery: MutableState<String>,
    onRefreshClicked: () -> Unit,
    onAttach: (pid: String, procName: String, memory: String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = searchQuery.value,
                onValueChange = { searchQuery.value = it },
                label = { Text("Search by package name", color = Color.White) },
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )

            Button(onClick = onRefreshClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = "Refresh"
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            itemsIndexed(runningProcState) { index, process ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {onAttach(process.pid, process.packageName, process.memory)},
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = process.pid, modifier = Modifier.weight(0.2f), color = Color.White)
                    Text(text = process.packageName, modifier = Modifier.weight(0.5f), color = Color.White)
                    Text(text = "${process.memory.toLong() / 1024} MB", modifier = Modifier.weight(0.3f), color = Color.White)
                }
            }
        }
    }
}