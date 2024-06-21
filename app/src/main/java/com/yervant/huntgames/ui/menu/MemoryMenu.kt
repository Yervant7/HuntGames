package com.yervant.huntgames.ui.menu

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yervant.huntgames.ui.util.CreateTable
import com.yervant.huntgames.ui.util.NumberInputField
import com.yervant.huntgames.ui.util.OverlayDropDown
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.kuhakupixel.libuberalles.overlay.service.dialog.OverlayChoicesDialog
import com.kuhakupixel.libuberalles.overlay.service.dialog.OverlayInfoDialog
import com.yervant.huntgames.backend.Memory.NumType
import com.yervant.huntgames.backend.Memory.Operator
import com.yervant.huntgames.backend.HuntSettings
import com.yervant.huntgames.backend.Memory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.ColumnScrollbar
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds


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

// ================================================================
private val fileName = "/data/data/com.yervant.huntgames/files/matches.txt"
private val filenamepath = "/data/data/com.yervant.huntgames/files/filteroutput"
// ================================================================

data class MatchInfo(val address: String, val prevValue: String)

fun getCurrentScanOption(): ScanOptions {

    return ScanOptions(
        inputVal = scanInputVal.value,
        numType = NumType.values()[valueTypeSelectedOptionIdx.value],
        scanType = Operator.values()[scanTypeSelectedOptionIdx.value],
        initialScanDone = initialScanDone.value,
    )
}

@Composable
fun MemoryMenu(overlayContext: OverlayContext?) {
    val mem = Memory()
    val currentaddressList = mem.readMatchesFile()
    LaunchedEffect(Unit) {
        if (currentaddressList.isEmpty()) {
            println("No need to refresh")
        } else {
            while (true) {
                refreshvalue()
                delay(50.seconds)
            }
        }
    }
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, content = ({
        _MemoryMenu(
            snackbarHostState = snackbarHostState,
            coroutineScope = coroutineScope,
            overlayContext = overlayContext,
        )
    }))
}

fun refreshvalue() {
    val mem = Memory()
    val currentaddressList: List<MatchInfo> = mem.readMatchesFile()
    val fileOutputStream = FileOutputStream(fileName)
    val printWriter = PrintWriter(fileOutputStream)
    var i = 0
    while (i < currentaddressList.size) {
        val value = mem.getvalue(currentaddressList[i].address)
        val line = "${currentaddressList[i].address} $value"
        printWriter.println(line)
        i++
    }
    printWriter.close()
    UpdateMatches()
}

var valtypeselected: String = "int"
var valuestype: List<String> = listOf("int", "long", "float")

@Composable
fun _MemoryMenu(
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    overlayContext: OverlayContext?
) {
    // ==================================
    // initialize default value for scanning options
    if (!defaultValueInitialized) {
        // init default
        scanTypeSelectedOptionIdx.value = Operator.values().indexOf(HuntSettings.defaultScanType)

        defaultValueInitialized = true
    }
    val isattc = isattached()
    val isAttached: Boolean = isattc.alert()
    valueTypeEnabled.value = isAttached && !initialScanDone.value
    scanTypeEnabled.value = isAttached

    // =================================
    // for showing scan error
    // have to use this hacky solution because OverlayInfoDiaog.show
    // cannot be called in another thread
    val showErrorDialog = remember { mutableStateOf(false) }
    val errorDialogMsg = remember { mutableStateOf("") }
    if (showErrorDialog.value) {
        OverlayInfoDialog(overlayContext!!).show(
            title = "Error",
            text = errorDialogMsg.value,
            onConfirm = {
            },
            onClose = {

                showErrorDialog.value = false
            }

        )
    }


//
    val content: @Composable (matchesTableModifier: Modifier, matchesSettingModifier: Modifier) -> Unit =
        { matchesTableModifier, matchesSettingModifier ->

            MatchesTable(
                modifier = matchesTableModifier,
                matches = currentMatchesList.value,
                matchesStatusText = matchesStatusText.value,
                onMatchClicked = { matchInfo: MatchInfo ->
                    //
                    val valueType: NumType = getCurrentScanOption().numType
                    AddressTableAddAddress(matchInfo = matchInfo)
                    //
                    coroutineScope.launch() {
                        snackbarHostState.showSnackbar(
                            message = "Added ${matchInfo.address} to Address Table",
                            duration = SnackbarDuration.Short,
                            actionLabel = "Ok"

                        )
                    }
                },
                onCopyAllMatchesToAddressTable = {
                    val valueType: NumType = getCurrentScanOption().numType
                    for (matchInfo in currentMatchesList.value)
                        AddressTableAddAddress(matchInfo = matchInfo)
                    coroutineScope.launch() {
                        snackbarHostState.showSnackbar(
                            message = "Added all matches to Address Table",
                            duration = SnackbarDuration.Short,
                            actionLabel = "Ok"

                        )
                    }

                }
            )
            MatchesSetting(
                modifier = matchesSettingModifier,
                //
                scanTypeEnabled = scanTypeEnabled,
                scanTypeSelectedOptionIdx = scanTypeSelectedOptionIdx,
                //
                scanInputVal = scanInputVal,
                // only allow to change Value type before any scan is done
                valueTypeEnabled = valueTypeEnabled,
                valueTypeSelectedOptionIdx = valueTypeSelectedOptionIdx,
                //
                nextScanEnabled = isAttached && !isScanOnGoing.value,
                nextScanClicked = fun() {
                    onNextScanClicked(
                        currentmatches = currentMatchesList.value,
                        scanOptions = getCurrentScanOption(),
                        onBeforeScanStart = {
                            // disable next and new scan
                            isScanOnGoing.value = true
                        },
                        onScanDone = {
                            isScanOnGoing.value = false
                            // set initial scan to true
                            initialScanDone.value = true
                            // update matches table
                            UpdateMatches()
                        },
                        onScanError = { e: Exception ->
                            showErrorDialog.value = true
                            errorDialogMsg.value = e.stackTraceToString()
                        }
                    )
                },

                //
                newScanEnabled = isAttached && initialScanDone.value && !isScanOnGoing.value,
                newScanClicked = {
                    ResetMatches()
                    UpdateMatches()
                    initialScanDone.value = false
                },
                overlayContext = overlayContext!!,
            )

        }


// switch to column when portrait and row when landscape
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

fun ResetMatches() {
    currentMatchesList.value = emptyList()
    matchesStatusText.value = "0 matches"
    val file = File(fileName)
    if (file.exists()) {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "rm $fileName"))
        process.waitFor()
    }
    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "rm $filenamepath*"))
    process.waitFor()
}

@Composable
private fun MatchesTable(
    modifier: Modifier = Modifier,
    matches: List<MatchInfo>,
    matchesStatusText: String,
    onMatchClicked: (matchInfo: MatchInfo) -> Unit,
    onCopyAllMatchesToAddressTable: () -> Unit,
) {

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween
        ) {
            Text(matchesStatusText)

            Button(onClick = onCopyAllMatchesToAddressTable) {
                Icon(
                    Icons.Filled.ArrowForward,
                    "Copy all matches to address table",
                )
            }
        }
        CreateTable(colNames = listOf("Address", "Previous Value"),
            colWeights = listOf(0.4f, 0.6f),
            itemCount = matches.size,
            minEmptyItemCount = 50,
            onRowClicked = { rowIndex: Int ->
                onMatchClicked(matches[rowIndex])
            },
            drawCell = { rowIndex: Int, colIndex: Int ->
                if (colIndex == 0) {
                    Text(text = matches[rowIndex].address)
                }
                if (colIndex == 1) {
                    Text(text = matches[rowIndex].prevValue)
                }
            })
    }
}

fun UpdateMatches() {
    val mem = Memory()
    val matchesCount: Int = mem.getMatchCount(mem.readMatchesFile())
    val shownMatchesCount: Int = min(matchesCount, HuntSettings.maxShownMatchesCount)
    // update ui
    currentMatchesList.value = mem.listMatches(HuntSettings.maxShownMatchesCount)
    matchesStatusText.value = "$matchesCount matches (showing ${shownMatchesCount})"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchesSetting(
    modifier: Modifier = Modifier,
    //
    scanTypeEnabled: MutableState<Boolean>,
    scanTypeSelectedOptionIdx: MutableState<Int>,
    //
    scanInputVal: MutableState<String>,
    //
    valueTypeEnabled: MutableState<Boolean>,
    valueTypeSelectedOptionIdx: MutableState<Int>,
    //
    nextScanEnabled: Boolean,
    nextScanClicked: () -> Unit,
    //
    newScanEnabled: Boolean,
    newScanClicked: () -> Unit,
    overlayContext: OverlayContext,
) {
    @Composable
    fun ScanInputField(scanValue: MutableState<String>) {
        NumberInputField(
            value = scanValue.value,
            onValueChange = { value ->
                scanValue.value = value
            },
            label = "Scan For",
            placeholder = "value ...",
        )
    }

    @Composable
    fun ScanButton(
        modifier: Modifier = Modifier,
        nextScanEnabled: Boolean,
        newScanEnabled: Boolean,
        onNextScan: () -> Unit,
        onNewScan: () -> Unit
    ) {
        Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(enabled = newScanEnabled, onClick = onNewScan) {
                Text("New Scan")
            }
            Button(enabled = nextScanEnabled, onClick = onNextScan) {
                Text("Next Scan")
            }
        }

    }

    @Composable
    fun ValueTypeDropDown(
        selectedOptionIndex: MutableState<Int>,
        enabled: MutableState<Boolean>,
        overlayContext: OverlayContext,
    ) {
        val expanded = remember { mutableStateOf(false) }
        OverlayDropDown(
            enabled = enabled,
            label = "Value Type",
            expanded = expanded,
            options = valuestype,
            selectedOptionIndex = selectedOptionIndex.value,
            onShowOptions = fun(options: List<String>) {
                OverlayChoicesDialog(overlayContext!!).show(
                    title = "Value: ",
                    choices = options,
                    onConfirm = { index: Int, value: String ->
                        selectedOptionIndex.value = index
                        valtypeselected = value
                    },
                    onClose = {
                        // after choice dialog is closed
                        // we should also set expanded to false
                        // so drop down will look closed
                        expanded.value = false

                    },
                    chosenIndex = selectedOptionIndex.value
                )
            }
        )
    }

    Column(modifier = modifier) {

        Box(
            modifier = Modifier
                .padding(vertical = 5.dp)
                .weight(0.8f)
        ) {
            val columnState = rememberScrollState()
            ColumnScrollbar(state = columnState, alwaysShowScrollBar = true) {
                Column(
                    modifier.verticalScroll(columnState),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ScanInputField(scanValue = scanInputVal)
                    /*ScanTypeDropDown(
                        scanTypeSelectedOptionIdx,
                        enabled = scanTypeEnabled,
                        overlayContext = overlayContext,
                    )*/
                    ValueTypeDropDown(
                        valueTypeSelectedOptionIdx,
                        // only allow to change type during initial scan
                        enabled = valueTypeEnabled,
                        overlayContext = overlayContext,
                    )
                }

            }

        }
        ScanButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f),
            nextScanEnabled = nextScanEnabled,
            // new scan can only be done if we have done at least one scan
            newScanEnabled = newScanEnabled,
            //
            onNextScan = nextScanClicked,
            onNewScan = newScanClicked,
        )

    }
}

@Composable
@Preview
fun MemoryMenuPreview() {
    MemoryMenu(null)
}