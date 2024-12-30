package com.yervant.huntgames.ui.menu

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.yervant.huntgames.backend.Memory.NumType
import com.yervant.huntgames.backend.HuntSettings
import com.yervant.huntgames.backend.Memory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds
import com.yervant.huntgames.backend.Memory.Companion.matches
import com.yervant.huntgames.backend.Process
import com.yervant.huntgames.ui.DialogCallback
import kotlinx.coroutines.isActive

// ======================= drop down options =================
private var defaultValueInitialized: Boolean = false

// ==================== selected scan options ==============================
private var scanInputVal: MutableState<String> = mutableStateOf("")

private val scanTypeSelectedOptionIdx = mutableStateOf(0)
private val valueTypeSelectedOptionIdx = mutableStateOf(0)

// ================================================================
private val initialScanDone: MutableState<Boolean> = mutableStateOf(false)
private val isScanOnGoing: MutableState<Boolean> = mutableStateOf(false)

private val valueTypeEnabled: MutableState<Boolean> = mutableStateOf(false)
private val scanTypeEnabled: MutableState<Boolean> = mutableStateOf(false)

// ===================================== current matches data =========================
private var currentMatchesList: MutableState<List<MatchInfo>> = mutableStateOf(mutableListOf())
private var matchesStatusText: MutableState<String> = mutableStateOf("0 matches")

data class MatchInfo(val address: Long, val prevValue: String, val valuetype: String)

fun getCurrentScanOption(): ScanOptions {

    return ScanOptions(
        inputVal = scanInputVal.value,
        numType = NumType.values()[valueTypeSelectedOptionIdx.value],
        initialScanDone = initialScanDone.value,
    )
}

@Composable
fun InitialMemoryMenu(context: Context?, dialogCallback: DialogCallback) {
    val currentaddressList = matches

    LaunchedEffect(Unit) {
        if (currentaddressList.isEmpty()) {
            println("No need to refresh")
        } else {
            while (isActive) {
                if (currentaddressList.isEmpty()) {
                    println("No need to refresh")
                } else {
                    refreshValues(context!!, dialogCallback)
                    delay(50.seconds)
                }
                delay(10.seconds)
            }
        }
    }
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, content = ({
        MemoryMenu(
            snackbarHostState = snackbarHostState,
            coroutineScope = coroutineScope,
            context!!,
            dialogCallback
        )
    }))
}

suspend fun refreshValues(context: Context, dialogCallback: DialogCallback) {
    val mem = Memory()
    val firstThousand = synchronized(matches) {
        if (matches.isEmpty()) return
        matches.take(1000)
    }

    val pid = isattached().savepid()

    if (pid < 0) {
        dialogCallback.showInfoDialog(
            title = "Hunt Games",
            message = "No Process Attached",
            onConfirm = {},
            onDismiss = {}
        )
        return
    }

    if (!Process().processIsRunning(pid.toString())) {
        dialogCallback.showInfoDialog(
            title = "Hunt Games",
            message = "Process Not Exist Anymore",
            onConfirm = {},
            onDismiss = {}
        )
        isattached().reset()
        return
    }

    val addresses = firstThousand.map { it.address }.toLongArray()
    val values = mem.getValues(addresses, context)

    val updatedMatches = addresses.zip(values).mapIndexed { index, (address, value) ->
        MatchInfo(
            address = address,
            prevValue = value.toString(),
            valuetype = firstThousand[index].valuetype
        )
    }

    synchronized(matches) {
        for (i in updatedMatches.indices) {
            if (i < matches.size) {
                matches[i] = updatedMatches[i]
            }
        }
    }

    updateMatches()
}

var valtypeselected: String = "int"
var valuestype: List<String> = listOf("int", "long", "float", "double")

fun getscantype(): Int {
    return scanTypeSelectedOptionIdx.value
}

@Composable
fun MemoryMenu(
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    context: Context,
    dialogCallback: DialogCallback,
) {

    if (!defaultValueInitialized) {
        scanTypeSelectedOptionIdx.value = 0
        defaultValueInitialized = true
    }
    val isAttached: Boolean = isattached().alert()
    valueTypeEnabled.value = isAttached && !initialScanDone.value
    scanTypeEnabled.value = isAttached

    val showErrorDialog = remember { mutableStateOf(false) }
    val errorDialogMsg = remember { mutableStateOf("") }
    if (showErrorDialog.value) {
        dialogCallback.showInfoDialog(
            title = "Error",
            message = errorDialogMsg.value,
            onConfirm = { showErrorDialog.value = false },
            onDismiss = {}
        )
    }

    val content: @Composable (matchesTableModifier: Modifier, matchesSettingModifier: Modifier) -> Unit =
        { matchesTableModifier, matchesSettingModifier ->

            MatchesTable(
                modifier = matchesTableModifier,
                matches = currentMatchesList.value,
                matchesStatusText = matchesStatusText.value,
                onMatchClicked = { matchInfo: MatchInfo ->
                    AddressTableAddAddress(matchInfo = matchInfo)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Added ${matchInfo.address} to Address Table",
                            duration = SnackbarDuration.Short,
                            actionLabel = "Ok"
                        )
                    }
                },
                onCopyAllMatchesToAddressTable = {
                    for (matchInfo in currentMatchesList.value)
                        AddressTableAddAddress(matchInfo = matchInfo)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Added all matches to Address Table",
                            duration = SnackbarDuration.Short,
                            actionLabel = "Ok"
                        )
                    }
                },
            )
            MatchesSetting(
                modifier = matchesSettingModifier,
                scanTypeSelectedOptionIdx = scanTypeSelectedOptionIdx,
                scanInputVal = scanInputVal,
                nextScanEnabled = isAttached && !isScanOnGoing.value,
                nextScanClicked = {
                    coroutineScope.launch {
                        onNextScanClicked(
                            scanOptions = getCurrentScanOption(),
                            currentmatcheslist = currentMatchesList.value,
                            onBeforeScanStart = {
                                isScanOnGoing.value = true
                            },
                            onScanDone = {
                                isScanOnGoing.value = false
                                initialScanDone.value = true
                                updateMatches()
                            },
                            onScanError = { e: Exception ->
                                showErrorDialog.value = true
                                errorDialogMsg.value = e.stackTraceToString()
                            },
                            context = context
                        )
                    }
                },
                newScanEnabled = isAttached && initialScanDone.value && !isScanOnGoing.value,
                newScanClicked = {
                    resetMatches()
                    updateMatches()
                    initialScanDone.value = false
                },
            )
        }

    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize(),
        ) {
            content(
                matchesTableModifier = Modifier
                    .weight(0.6f)
                    .padding(16.dp),
                matchesSettingModifier = Modifier
                    .weight(0.4f)
                    .padding(10.dp),
            )
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize(),
        ) {
            content(
                matchesTableModifier = Modifier
                    .weight(0.6f)
                    .padding(16.dp),
                matchesSettingModifier = Modifier
                    .weight(0.4f)
                    .fillMaxSize()
            )
        }
    }
}

fun resetMatches() {
    currentMatchesList.value = emptyList()
    matchesStatusText.value = "0 matches"
    if (matches.isNotEmpty()) {
        matches.clear()
    }
}

@Composable
private fun MatchesTable(
    modifier: Modifier = Modifier,
    matches: List<MatchInfo>,
    matchesStatusText: String,
    onMatchClicked: (matchInfo: MatchInfo) -> Unit,
    onCopyAllMatchesToAddressTable: () -> Unit
) {

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween
        ) {
            Text(matchesStatusText, color = Color.White)

            Button(onClick = onCopyAllMatchesToAddressTable) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    "Copy all matches to address table",
                )
            }
        }
        LazyColumn {
            items(matches.size) { index ->
                val match = matches[index]
                Column(
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable { onMatchClicked(match) }
                ) {
                    Text(text = "Address: ${match.address}", color = Color.White)
                    Text(text = "PrevValue: ${match.prevValue}", color = Color.White)
                    Text(text = "Type: ${match.valuetype}", color = Color.White)
                }
            }
        }
    }
}

fun updateMatches() {
    val mem = Memory()
    val matchesCount: Int = matches.size
    val shownMatchesCount: Int = min(matchesCount, HuntSettings.maxShownMatchesCount)
    // update ui
    currentMatchesList.value = emptyList()
    currentMatchesList.value = mem.listMatches(HuntSettings.maxShownMatchesCount)
    matchesStatusText.value = "$matchesCount matches (showing ${shownMatchesCount})"
}

@Composable
private fun MatchesSetting(
    modifier: Modifier = Modifier,
    scanTypeSelectedOptionIdx: MutableState<Int>,
    scanInputVal: MutableState<String>,
    nextScanEnabled: Boolean,
    nextScanClicked: () -> Unit,
    newScanEnabled: Boolean,
    newScanClicked: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isScanTypeMenuExpanded = remember { mutableStateOf(false) }
    val isValueTypeMenuExpanded = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Campo de entrada
        OutlinedTextField(
            value = scanInputVal.value,
            onValueChange = { scanInputVal.value = it },
            label = { Text("Scan For", color = Color.White) },
            placeholder = { Text("Enter value...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { isScanTypeMenuExpanded.value = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Type: ${scanTypeOptions[scanTypeSelectedOptionIdx.value]}")
        }

        DropdownMenu(
            expanded = isScanTypeMenuExpanded.value,
            onDismissRequest = { isScanTypeMenuExpanded.value = false }
        ) {
            scanTypeOptions.forEachIndexed { index, label ->
                DropdownMenuItem(
                    onClick = {
                        scanTypeSelectedOptionIdx.value = index
                        isScanTypeMenuExpanded.value = false
                    },
                    text = { Text(label, color = Color.White) }
                )
            }
        }

        Button(
            onClick = { isValueTypeMenuExpanded.value = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Value Type: $valtypeselected")
        }

        DropdownMenu(
            expanded = isValueTypeMenuExpanded.value,
            onDismissRequest = { isValueTypeMenuExpanded.value = false }
        ) {
            valuestype.forEach { label ->
                DropdownMenuItem(
                    onClick = {
                        valtypeselected = label
                        isValueTypeMenuExpanded.value = false
                    },
                    text = { Text(label, color = Color.White) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = newScanClicked,
                enabled = newScanEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text("New Scan", color = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = nextScanClicked,
                enabled = nextScanEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text("Next Scan", color = Color.White)
            }
        }
    }
}

private val scanTypeOptions = listOf(
    "ACCURATE_VAL", "LARGER_THAN_VAL", "LESS_THAN_VAL",
    "BETWEEN_VAL", "CHANGED_VAL", "UNCHANGED_VAL"
)