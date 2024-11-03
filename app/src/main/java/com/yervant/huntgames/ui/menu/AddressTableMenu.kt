package com.yervant.huntgames.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
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
import com.yervant.huntgames.ui.OverlayInputDialog
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import kotlin.time.Duration.Companion.seconds

class AddressInfo(
    val matchInfo: MatchInfo,
    val numType: String,
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
            modifier = Modifier.weight(0.2f),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween
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
                Button(
                    onClick = {
                        OverlayInputDialog(overlayContext!!).show(
                            title = "Edit All",
                            defaultValue = "999999999",
                            onConfirm = { input: String ->
                                Hunt().writeall(savedAddresList, input, overlayContext)
                            }
                        )
                    }) {

                    Icon(Icons.Filled.Edit, "Edit All Matches")
                }
                Button(
                    onClick = {
                        OverlayInputDialog(overlayContext!!).show(
                            title = "Freeze All",
                            defaultValue = "999999999",
                            onConfirm = { input: String ->
                                Hunt().freezeall(savedAddresList, input, overlayContext)
                            }
                        )
                    }) {

                    Icon(Icons.Filled.CheckCircle, "Freeze All Matches")
                }
                Button(
                    onClick = {
                        Hunt().unfreezeall(overlayContext!!)
                    }) {

                    Icon(Icons.Filled.Clear, "UnFreeze All Matches")
                }
            }
        }
        SavedAddressesTable(
            modifier = Modifier.weight(0.8f),
            overlayContext = overlayContext!!,
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
                        val addr = LongArray(1)
                        addr[0] = addressInfo.matchInfo.address
                        hunt.writeValueAtAddress(
                            isattached().savepid(),
                            addr,
                            newValue,
                            addressInfo.matchInfo.valuetype,
                            overlayContext
                        )
                    }
                )
            }
        )
        val fileOutputStream = FileOutputStream(savedaddressesfile)
        val file = File(savedaddressesfile)
        val contentfile: MutableList<String> = mutableListOf()
        if (file.exists()) {
            contentfile.addAll(file.readLines())
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
                    refreshvaluetable(overlayContext!!)
                    delay(10.seconds)
                }
            }
        }
    }
}

suspend fun refreshvaluetable(overlayContext: OverlayContext) {
    val mem = Memory()
    val currentsavedaddressList = readSavedAddressesFile()
    val fileOutputStream = FileOutputStream(savedaddressesfile)
    val printWriter = PrintWriter(fileOutputStream)

    val addresses = LongArray(currentsavedaddressList.size)
    var i = 0
    while (i < addresses.size) {
        addresses[i] = currentsavedaddressList[i].address
        i++
    }

    val values = mem.getvalues(addresses, overlayContext)
    for (value in values) {
        val line = "${currentsavedaddressList[i].address} $value"
        printWriter.println(line)
        savedAddresList.add(AddressInfo(MatchInfo(currentsavedaddressList[i].address, value.toString(), currentsavedaddressList[i].valuetype), valtypeselected))
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
            val valtype = parts[2]
            savedaddresses.add(MatchInfo(addr.toLong(16), value, valtype))
        }
    }

    return savedaddresses
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesTable(
    modifier: Modifier = Modifier,
    overlayContext: OverlayContext,
    savedAddressList: SnapshotStateList<AddressInfo>,
    onValueClicked: (addressInfo: AddressInfo) -> Unit,
    onAddressClicked: (itemIndex: Int) -> Unit
) {

    CreateTable(
        modifier = modifier,
        colNames = listOf("Address", "Type", "Value"),
        colWeights = listOf(0.4f, 0.2f, 0.4f),
        itemCount = savedAddressList.size,
        minEmptyItemCount = 50,
        onRowClicked = { rowIndex: Int ->
        },
        rowMinHeight = 25.dp,
        drawCell = { rowIndex: Int, colIndex: Int ->
            // address
            if (colIndex == 0) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAddressClicked(rowIndex)
                        },
                ) {
                    Text(text = savedAddressList[rowIndex].matchInfo.address.toString())
                }
            }

            // num type
            if (colIndex == 1) {
                Text(text = savedAddressList[rowIndex].matchInfo.valuetype)
            }

            // value
            if (colIndex == 2) {
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