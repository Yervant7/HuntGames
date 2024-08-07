package com.yervant.huntgames.ui.menu

import com.yervant.huntgames.backend.Memory
import java.util.concurrent.CompletableFuture

class ScanOptions(
    val inputVal: String,
    val numType: Memory.NumType,
    val scanType: Memory.Operator,
    val initialScanDone: Boolean,
) {
}

fun onNextScanClicked(
    currentmatches: List<MatchInfo>,
    scanOptions: ScanOptions,
    onBeforeScanStart: () -> Unit,
    onScanDone: () -> Unit,
    onScanError: (e: Exception) -> Unit,
) {
    onBeforeScanStart()
    val mem = Memory()
    CompletableFuture.supplyAsync<Unit> {
        // set the value type
        /**
         * scan against a value if input value
         * is not empty
         * and scan without value otherwise
         * (picking addresses whose value stayed the same, increased and etc)
         * */

        if (scanOptions.inputVal.isBlank()) {
            throw RuntimeException("Input value cannot be empty")
        } else if (scanOptions.inputVal.contains(" ")) {
            throw RuntimeException("Input value cannot contain spaces")
        } else if (scanOptions.inputVal.startsWith("0x") && scanOptions.inputVal.contains("+") || scanOptions.inputVal.startsWith("0x") && scanOptions.inputVal.contains("-")) {
            val value = if (scanOptions.inputVal.contains("-")) {
                scanOptions.inputVal.split("-")
            } else {
                scanOptions.inputVal.split("+")
            }
            val issub = scanOptions.inputVal.contains("-")
            val addr = value[0]
            val offset = value[1]
            mem.gotoAddressAndOffset(addr, offset, issub)
        } else if (scanOptions.inputVal.startsWith("0x")){
            mem.gotoAddress(scanOptions.inputVal)
        } else if (scanOptions.inputVal.contains(";")) {
            mem.scanAgainstValueGroup(
                scanOptions.inputVal,
                currentmatches
            )
        } else {
            mem.scanAgainstValue(
                scanOptions.inputVal,
                currentmatches
            )
        }

        onScanDone()
    }.exceptionally { e ->
        onScanError(e as Exception)
        onScanDone()
    }
}