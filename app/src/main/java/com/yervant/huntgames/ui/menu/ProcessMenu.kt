package com.yervant.huntgames.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
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
private var svpid: Int = -1
private var lastpid: Int = -1

class isattached {
    fun alert(): Boolean {
        return success
    }
    fun currentPid(): Int {
        return svpid
    }
    fun reset() {
        svpid = -1
        success = false
    }
    fun resetLast() {
        lastpid = -1
    }
    fun lastPid(): Int {
        return lastpid
    }
}

fun AttachToProcess(
    pid: Int,
    onProcessNoExistAnymore: () -> Unit,
    onAttachSuccess: () -> Unit,
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

    ProcessMenuContent(
        runningProcState = filteredProcList,
        searchQuery = searchQuery,
        onAttach = { pid, procName, memory ->
            dialogCallback.showInfoDialog(
                title = "Hunt Games",
                message = "Attach to $pid - $procName?",
                onConfirm = {
                    AttachToProcess(
                        pid = pid.toInt(),
                        onAttachSuccess = {
                            dialogCallback.showInfoDialog(
                                title = "Hunt Games",
                                message = "Attaching to $procName is successful",
                                onConfirm = {},
                                onDismiss = {}
                            )
                            attachedStatusString.value = "$pid - $procName"
                            if (svpid != -1) {
                                lastpid = svpid
                            }
                            svpid = pid.toInt()
                        },
                        onProcessNoExistAnymore = {
                            dialogCallback.showInfoDialog(
                                title = "Hunt Games",
                                message = "Process $procName is not running anymore, Can't attach",
                                onConfirm = {},
                                onDismiss = {}
                            )
                        },
                    )
                },
                onDismiss = {}
            )
        },
        onRefreshClicked = { refreshProcList() },
    )
}

@Composable
private fun ProcessMenuContent(
    runningProcState: List<ProcessInfo>,
    searchQuery: MutableState<String>,
    onRefreshClicked: () -> Unit,
    onAttach: (pid: String, procName: String, memory: String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SearchAndRefreshRow(searchQuery, onRefreshClicked)

            if (runningProcState.isEmpty()) {
                EmptyStateMessage()
            } else {
                ProcessList(runningProcState, onAttach)
            }
        }
    }
}

@Composable
private fun SearchAndRefreshRow(
    searchQuery: MutableState<String>,
    onRefreshClicked: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            value = searchQuery.value,
            onValueChange = { searchQuery.value = it },
            label = { Text("Search processes") },
            leadingIcon = {
                Icons.Filled.Search
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            )
        )

        IconButton(
            onClick = onRefreshClicked,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_refresh),
                contentDescription = "Refresh",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProcessList(
    processes: List<ProcessInfo>,
    onAttach: (String, String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(processes) { index, process ->
            ProcessListItem(
                process = process,
                onAttach = onAttach,
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
            )
            if (index < processes.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun ProcessListItem(
    process: ProcessInfo,
    onAttach: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedMemory = remember(process.memory) {
        "%.2f MB".format(process.memory.toLong() / 1024.0)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onAttach(process.pid, process.packageName, process.memory) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(0.4f)) {
                Text(
                    text = process.packageName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "PID: ${process.pid}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Text(
                text = formattedMemory,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(0.2f)
            )
        }
    }
}

@Composable
private fun EmptyStateMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No processes found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}