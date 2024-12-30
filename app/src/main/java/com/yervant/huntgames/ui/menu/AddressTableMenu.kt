package com.yervant.huntgames.ui.menu

import android.content.Context
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yervant.huntgames.backend.Hunt
import com.yervant.huntgames.backend.Memory
import com.yervant.huntgames.backend.Process
import com.yervant.huntgames.ui.CreateTable
import com.yervant.huntgames.ui.DialogCallback
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
fun AddressTableMenu(context: Context?, dialogCallback: DialogCallback) {

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showFreezeDialog by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf<Int?>(null) }
    var showEditValueDialog by remember { mutableStateOf<AddressInfo?>(null) }

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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, "Delete All Matches")
                }
                Button(onClick = { showEditDialog = true }) {
                    Icon(Icons.Filled.Edit, "Edit All Matches")
                }
                Button(onClick = { showFreezeDialog = true }) {
                    Icon(Icons.Filled.CheckCircle, "Freeze All Matches")
                }
                Button(onClick = { Hunt().unfreezeall(context!!) }) {
                    Icon(Icons.Filled.Clear, "UnFreeze All Matches")
                }
            }
        }

        SavedAddressesTable(
            modifier = Modifier.weight(0.8f),
            savedAddressList = savedAddresList,
            onAddressClicked = { itemIndex ->
                showAddressDialog = itemIndex
            },
            onValueClicked = { addressInfo ->
                showEditValueDialog = addressInfo
            }
        )

        if (showDeleteDialog) {
            dialogCallback.showInfoDialog(
                title = "Hunt Games",
                message = "Delete all addresses?",
                onConfirm = {
                    savedAddresList.clear()
                },
                onDismiss = { showDeleteDialog = false }
            )
            showDeleteDialog = false
        }

        if (showEditDialog) {
            dialogCallback.showInputDialog(
                title = "Edit All",
                defaultValue = "999999999",
                onConfirm = { input ->
                    Hunt().writeall(savedAddresList, input, context!!)
                },
                onDismiss = { showEditDialog = false }
            )
            showEditDialog = false
        }

        if (showFreezeDialog) {
            dialogCallback.showInputDialog(
                title = "Freeze All",
                defaultValue = "999999999",
                onConfirm = { input ->
                    Hunt().freezeall(savedAddresList, input, context!!)
                },
                onDismiss = { showFreezeDialog = false }
            )
            showFreezeDialog = false
        }

        showAddressDialog?.let { index ->
            dialogCallback.showAddressDialog(
                title = "Address",
                onAddressDeleted = { savedAddresList.removeAt(index) },
                onDismiss = { showAddressDialog = null }
            )
            showAddressDialog = null
        }

        showEditValueDialog?.let { addressInfo ->
            dialogCallback.showInputDialog(
                title = "Edit value of $addressInfo",
                defaultValue = "999999999",
                onConfirm = { newValue ->
                    val hunt = Hunt()
                    val addr = LongArray(1)
                    addr[0] = addressInfo.matchInfo.address
                    hunt.writeValueAtAddress(
                        isattached().savepid(),
                        addr,
                        newValue,
                        addressInfo.matchInfo.valuetype,
                        context!!
                    )
                },
                onDismiss = { showEditValueDialog = null }
            )
            showEditValueDialog = null
        }

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
                    refreshValueTable(context!!, dialogCallback)
                    delay(10.seconds)
                }
            }
        }
    }
}

suspend fun refreshValueTable(context: Context, dialogCallback: DialogCallback) {
    val mem = Memory()
    val currentsavedaddressList = readSavedAddressesFile()
    val fileOutputStream = FileOutputStream(savedaddressesfile)
    val printWriter = PrintWriter(fileOutputStream)

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

    val addresses = LongArray(currentsavedaddressList.size)
    var i = 0
    while (i < addresses.size) {
        addresses[i] = currentsavedaddressList[i].address
        i++
    }

    val values = mem.getValues(addresses, context)
    values.forEachIndexed { index, value ->
        val line = "${currentsavedaddressList[index].address} $value"
        printWriter.println(line)
        savedAddresList.add(AddressInfo(
            MatchInfo(
                currentsavedaddressList[index].address,
                value.toString(),
                currentsavedaddressList[index].valuetype
            ),
            valtypeselected
        ))
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
                    Text(text = savedAddressList[rowIndex].matchInfo.address.toString(), color = Color.White)
                }
            }

            // num type
            if (colIndex == 1) {
                Text(text = savedAddressList[rowIndex].matchInfo.valuetype, color = Color.White)
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
                        text = savedAddressList[rowIndex].matchInfo.prevValue, color = Color.White
                    )
                }
            }
        }
    )
}