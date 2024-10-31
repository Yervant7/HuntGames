package com.yervant.huntgames.ui.menu

import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.yervant.huntgames.backend.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScanOptions(
    val inputVal: String,
    val numType: Memory.NumType,
    val initialScanDone: Boolean,
) {
}

suspend fun onNextScanClicked(
    scanOptions: ScanOptions,
    currentmatcheslist: List<MatchInfo>,
    overlayContext: OverlayContext,
    onBeforeScanStart: () -> Unit,
    onScanDone: () -> Unit,
    onScanError: (e: Exception) -> Unit,
) {
    onBeforeScanStart()
    try {
        withContext(Dispatchers.IO) {
            val mem = Memory()
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
                mem.gotoAddressAndOffset(addr, offset, issub, overlayContext)
            } else if (scanOptions.inputVal.startsWith("0x")){
                mem.gotoAddress(scanOptions.inputVal, overlayContext)
            } else {
                mem.scanAgainstValue(
                    scanOptions.inputVal,
                    currentmatcheslist,
                    overlayContext
                )
            }
        }
    } catch (e: Exception) {
        onScanError(e as Exception)
        onScanDone()
    } finally {
        onScanDone()
    }
}