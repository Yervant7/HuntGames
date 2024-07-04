package com.yervant.huntgames.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yervant.huntgames.ui.AddressOverlayDialog
import com.yervant.huntgames.ui.EditAddressOverlayDialog
import com.yervant.huntgames.ui.util.CreateTable
import com.yervant.huntgames.backend.Hunt
import com.kuhakupixel.libuberalles.overlay.OverlayContext
import com.kuhakupixel.libuberalles.overlay.service.dialog.OverlayInfoDialog
import com.yervant.huntgames.backend.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import kotlin.time.Duration.Companion.seconds

class AddressInfo(
    val matchInfo: MatchInfo,
    val numType: String,
    val isFreezed: MutableState<Boolean> = mutableStateOf(false)
) {
}
val savedaddressesfile = "/data/data/com.yervant.huntgames/files/savedaddresses.txt"
private val savedAddresList = mutableStateListOf<AddressInfo>()
fun AddressTableAddAddress(matchInfo: MatchInfo) {
    savedAddresList.add(AddressInfo(matchInfo, valtypeselected))
}

@Composable
fun AddressTableMenu(overlayContext: OverlayContext?) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(0.2f)
        ) {
            Button(
                onClick = {
                    OverlayInfoDialog(overlayContext!!).show(
                        title = "Info Dialog",
                        text = "Delete all addresses?",
                        onConfirm = {
                            savedAddresList.clear()
                        },
                    )

                }) {

                Icon(Icons.Filled.Delete, "Delete All Matches")
            }
        }
        SavedAddressesTable(
            modifier = Modifier.weight(0.8f),
            savedAddressList = savedAddresList,
            onAddressClicked = { itemIndex: Int ->
                AddressOverlayDialog(
                    overlayContext = overlayContext!!,
                    onAddressDeleted = {
                        savedAddresList.removeAt(index = itemIndex)
                    }

                ).show(title = "Address ", onConfirm = {})

            },
            onValueClicked = { addressInfo: AddressInfo ->
                EditAddressOverlayDialog(overlayContext!!).show(
                    title = "Edit value of $addressInfo",
                    onConfirm = { newValue: String ->
                        val hunt = Hunt()
                        val isattc = isattached()
                        if (addressInfo.isFreezed.value) {
                            GlobalScope.launch(Dispatchers.IO) {
                                hunt.freezeValueAtAddress(
                                    isattc.savepid(),
                                    addressInfo.matchInfo.address,
                                    newValue
                                )
                                hunt.setbool(true)
                            }
                        } else {
                            hunt.writeValueAtAddress(
                                isattc.savepid(),
                                addressInfo.matchInfo.address,
                                newValue
                            )
                        }
                    }
                )
            }
        )
        val fileOutputStream = FileOutputStream(savedaddressesfile)
        val file = File(savedaddressesfile)
        var contentfile = ""
        if (file.exists()) {
            contentfile = file.readText()
        }
        val printWriter = PrintWriter(fileOutputStream)
        if (savedAddresList.isNotEmpty() && contentfile.isEmpty()) {
            var i = 0
            while (i < savedAddresList.size) {
                val addressInfo = savedAddresList[i]
                val s = addressInfo.matchInfo.address
                val numValStr = addressInfo.matchInfo.prevValue
                val line = "$s $numValStr"
                printWriter.println(line)
                i++
            }
            printWriter.close()
        }
        if (contentfile.isNotEmpty()) {
            LaunchedEffect(Unit) {
                while (
                    savedAddresList.isNotEmpty() &&
                    contentfile.isNotEmpty()
                ) {
                    refreshvaluetable()
                    delay(10.seconds)
                }
            }
        }
    }
}

fun refreshvaluetable() {
    val mem = Memory()
    val currentsavedaddressList = readSavedAddressesFile()
    val fileOutputStream = FileOutputStream(savedaddressesfile)
    val printWriter = PrintWriter(fileOutputStream)
    var i = 0
    while (i < currentsavedaddressList.size) {
        val value = mem.getvalue(currentsavedaddressList[i].address)
        val line = "${currentsavedaddressList[i].address} $value"
        printWriter.println(line)
        savedAddresList.add(AddressInfo(MatchInfo(currentsavedaddressList[i].address, value), valtypeselected))
    }
    printWriter.close()
}

fun readSavedAddressesFile(): List<MatchInfo> {
    val file = File(savedaddressesfile)
    val savedaddresses = mutableListOf<MatchInfo>()

    if (file.exists()) {
        for (s: String in file.readLines()) {
            val parts = s.split(" ")
            val addr = parts[0]
            val value = parts[1]
            savedaddresses.add(MatchInfo(addr, value))
        }
    }

    return savedaddresses
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesTable(
    modifier: Modifier = Modifier,
    savedAddressList: SnapshotStateList<AddressInfo>,
    onValueClicked: (addressInfo: AddressInfo) -> Unit,
    onAddressClicked: (itemIndex: Int) -> Unit
) {

    CreateTable(
        modifier = modifier,
        colNames = listOf("Freeze", "Address", "Type", "Value"),
        colWeights = listOf(0.2f, 0.3f, 0.2f, 0.3f),
        itemCount = savedAddressList.size,
        minEmptyItemCount = 50,
        onRowClicked = { rowIndex: Int ->
        },
        rowMinHeight = 25.dp,
        drawCell = { rowIndex: Int, colIndex: Int ->
            if (colIndex == 0) {
                // remove default padding  in Checkbox
                // https://stackoverflow.com/questions/71609051/remove-default-padding-around-checkboxes-in-jetpack-compose-new-update
                // https://stackoverflow.com/questions/73620652/jetpack-compose-internal-padding
                CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                    Checkbox(
                        savedAddressList[rowIndex].isFreezed.value,
                        onCheckedChange = { checked: Boolean ->
                            savedAddressList[rowIndex].isFreezed.value = checked
                            val hunt = Hunt()
                            if (checked) {
                                val isattc = isattached()
                                val pid = isattc.savepid()
                                val value = ""
                                GlobalScope.launch(Dispatchers.IO) {
                                    hunt.freezeValueAtAddress(
                                        pid,
                                        savedAddressList[rowIndex].matchInfo.address,
                                        value
                                    )
                                    hunt.setbool(true)
                                }
                            } else {
                                hunt.setbool(false)
                            }
                        },
                    )
                }
            }
            // address
            if (colIndex == 1) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAddressClicked(rowIndex)
                        },
                ) {
                    Text(text = savedAddressList[rowIndex].matchInfo.address)
                }
            }

            // num type
            if (colIndex == 2) {
                val typeDesc: String = valtypeselected
                Text(text = typeDesc)
            }

            // value
            if (colIndex == 3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onValueClicked(
                                savedAddressList[rowIndex]
                            )

                        },
                ) {
                    Text(
                        text = savedAddressList[rowIndex].matchInfo.prevValue,
                    )
                }
            }
        }
    )
}

@Composable
@Preview
fun AddressTablePreview() {
    AddressTableMenu(null)
}