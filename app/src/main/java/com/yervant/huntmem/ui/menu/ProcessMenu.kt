package com.yervant.huntmem.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yervant.huntmem.R
import com.yervant.huntmem.backend.Process
import com.yervant.huntmem.backend.Process.ProcessInfo
import com.yervant.huntmem.ui.DialogCallback
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
        attachedStatusString.value = "None"
    }
    fun resetLast() {
        lastpid = -1
    }
    fun lastPid(): Int {
        return lastpid
    }
}

fun attachToProcess(
    pid: Int,
    onProcessNoExistAnymore: () -> Unit,
    onAttachSuccess: () -> Unit,
) {
    if (Process().processIsRunning(pid.toString())) {
        if (svpid != -1 && svpid != pid) {
            lastpid = svpid
        }

        svpid = pid
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
        refreshProcList()
        while (isActive) {
            delay(60.seconds)
            refreshProcList()
        }
    }

    val filteredProcList = remember(searchQuery.value, currentProcList) {
        if (searchQuery.value.isBlank()) currentProcList
        else currentProcList.filter { it.packageName.contains(searchQuery.value, ignoreCase = true) }
    }

    ProcessMenuContent(
        runningProcState = filteredProcList,
        searchQuery = searchQuery,
        attachedStatus = attachedStatusString.value,
        currentPid = svpid,
        onAttach = { pid, procName, memory ->
            dialogCallback.showInfoDialog(
                title = "Hunt Games",
                message = "Attach to $pid - $procName?",
                onConfirm = {
                    attachToProcess(
                        pid = pid.toInt(),
                        onAttachSuccess = {
                            dialogCallback.showInfoDialog(
                                title = "Hunt Games",
                                message = "Attaching to $procName is successful",
                                onConfirm = {},
                                onDismiss = {}
                            )
                            attachedStatusString.value = "$pid - $procName"
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
    attachedStatus: String,
    currentPid: Int,
    onRefreshClicked: () -> Unit,
    onAttach: (pid: String, procName: String, memory: String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            AttachedProcessIndicator(attachedStatus)

            Spacer(modifier = Modifier.height(8.dp))

            SearchAndRefreshRow(searchQuery, onRefreshClicked)

            if (runningProcState.isEmpty()) {
                EmptyStateMessage()
            } else {
                ProcessList(
                    processes = runningProcState,
                    currentPid = currentPid,
                    onAttach = onAttach
                )
            }
        }
    }
}

@Composable
private fun AttachedProcessIndicator(attachedStatus: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Attached Process",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = attachedStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SearchAndRefreshRow(
    searchQuery: MutableState<String>,
    onRefreshClicked: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            value = searchQuery.value,
            onValueChange = { searchQuery.value = it },
            label = { Text("Search processes") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search"
                )
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
    currentPid: Int,
    onAttach: (String, String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(processes) { index, process ->
            val isAttached = currentPid == process.pid.toInt()

            ProcessListItem(
                process = process,
                isAttached = isAttached,
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

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ProcessListItem(
    process: ProcessInfo,
    isAttached: Boolean,
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
        color = if (isAttached)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(0.7f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = process.packageName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, false)
                    )

                    if (isAttached) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Attached",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

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
                modifier = Modifier.weight(0.3f, false)
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