package com.yervant.huntgames.ui.menu

import android.content.Context
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayDisabled
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yervant.huntgames.backend.HGMem
import com.yervant.huntgames.backend.Hunt
import com.yervant.huntgames.backend.Memory
import com.yervant.huntgames.backend.Process
import com.yervant.huntgames.ui.DialogCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class AddressInfo(
    val matchInfo: MatchInfo,
    val numType: String,
)

private val savedAddresList = mutableStateListOf<AddressInfo>()

fun AddressTableAddAddress(matchInfo: MatchInfo) {
    savedAddresList.add(AddressInfo(matchInfo, matchInfo.valuetype))
}

@Composable
fun AddressTableMenu(context: Context?, dialogCallback: DialogCallback) {
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showFreezeDialog by remember { mutableStateOf(false) }
    var selectedAddressIndex by remember { mutableStateOf<Int?>(null) }
    var selectedAddressInfo by remember { mutableStateOf<AddressInfo?>(null) }

    LaunchedEffect(savedAddresList.isNotEmpty()) {
        while (isActive) {
            refreshValue(context!!, dialogCallback)
            delay(10.seconds)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {

        ControlButtonsRow(
            showDeleteDialog = { showDeleteDialog = true },
            showEditDialog = { showEditDialog = true },
            showFreezeDialog = { showFreezeDialog = true }
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                AddressTableHeader()
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(savedAddresList) { index, item ->
                        AddressTableRow(
                            item = item,
                            index = index,
                            onAddressClick = { selectedAddressIndex = index },
                            onValueClick = { selectedAddressInfo = item }
                        )
                        if (index < savedAddresList.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }

    DialogHandling(
        context = context,
        dialogCallback = dialogCallback,
        coroutineScope = coroutineScope,
        showDeleteDialog = showDeleteDialog,
        showEditDialog = showEditDialog,
        showFreezeDialog = showFreezeDialog,
        selectedAddressIndex = selectedAddressIndex,
        selectedAddressInfo = selectedAddressInfo,
        onDismissDelete = { showDeleteDialog = false },
        onDismissEdit = { showEditDialog = false },
        onDismissFreeze = { showFreezeDialog = false },
        onDismissAddress = { selectedAddressIndex = null },
        onDismissValue = { selectedAddressInfo = null }
    )
}

@Composable
private fun ControlButtonsRow(
    showDeleteDialog: () -> Unit,
    showEditDialog: () -> Unit,
    showFreezeDialog: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.screenHeightDp > configuration.screenWidthDp

    if (isPortrait) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ControlButton(
                    icon = Icons.Filled.Delete,
                    text = "Delete All",
                    containerColor = MaterialTheme.colorScheme.error,
                    onClick = showDeleteDialog,
                    modifier = Modifier.weight(1f)
                )
                ControlButton(
                    icon = Icons.Filled.Edit,
                    text = "Edit All",
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    onClick = showEditDialog,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ControlButton(
                    icon = Icons.Filled.CheckCircle,
                    text = "Freeze All",
                    containerColor = MaterialTheme.colorScheme.primary,
                    onClick = showFreezeDialog,
                    modifier = Modifier.weight(1f)
                )
                ControlButton(
                    icon = Icons.Filled.PlayDisabled,
                    text = "Unfreeze",
                    containerColor = MaterialTheme.colorScheme.secondary,
                    onClick = { Hunt().unfreezeall() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton(
                icon = Icons.Filled.Delete,
                text = "Delete All",
                containerColor = MaterialTheme.colorScheme.error,
                onClick = showDeleteDialog
            )
            ControlButton(
                icon = Icons.Filled.Edit,
                text = "Edit All",
                containerColor = MaterialTheme.colorScheme.tertiary,
                onClick = showEditDialog
            )
            ControlButton(
                icon = Icons.Filled.CheckCircle,
                text = "Freeze All",
                containerColor = MaterialTheme.colorScheme.primary,
                onClick = showFreezeDialog
            )
            ControlButton(
                icon = Icons.Filled.PlayDisabled,
                text = "Unfreeze",
                containerColor = MaterialTheme.colorScheme.secondary,
                onClick = { Hunt().unfreezeall() }
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = containerColor,
            contentColor = Color.White
        ),
        modifier = modifier.height(40.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AddressTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TableCell(
            text = "Address",
            weight = 0.4f,
            textStyle = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        TableCell(
            text = "Type",
            weight = 0.2f,
            textStyle = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        TableCell(
            text = "Value",
            weight = 0.4f,
            textStyle = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}

@Composable
private fun AddressTableRow(
    item: AddressInfo,
    index: Int,
    onAddressClick: (Int) -> Unit,
    onValueClick: (AddressInfo) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = { onAddressClick(index) }
            ),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(
                text = "0x${item.matchInfo.address.toString(16).uppercase(Locale.ROOT)}",
                weight = 0.4f,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            )
            TableCell(
                text = item.matchInfo.valuetype,
                weight = 0.2f,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            TableCell(
                text = item.matchInfo.prevValue,
                weight = 0.4f,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                ),
                onClick = { onValueClick(item) }
            )
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    weight: Float,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(weight)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DialogHandling(
    context: Context?,
    dialogCallback: DialogCallback,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    showDeleteDialog: Boolean,
    showEditDialog: Boolean,
    showFreezeDialog: Boolean,
    selectedAddressIndex: Int?,
    selectedAddressInfo: AddressInfo?,
    onDismissDelete: () -> Unit,
    onDismissEdit: () -> Unit,
    onDismissFreeze: () -> Unit,
    onDismissAddress: () -> Unit,
    onDismissValue: () -> Unit
) {
    if (showDeleteDialog) {
        dialogCallback.showInfoDialog(
            title = "Delete All Addresses",
            message = "Are you sure you want to delete all saved addresses?",
            onConfirm = {
                savedAddresList.clear()
                onDismissDelete()
            },
            onDismiss = onDismissDelete
        )
    }

    if (showEditDialog) {
        dialogCallback.showInputDialog(
            title = "Edit All Values",
            defaultValue = "999999999",
            onConfirm = { input ->
                context?.let {
                    coroutineScope.launch {
                        Hunt().writeall(savedAddresList, input, context)
                    }
                }
                onDismissEdit()
            },
            onDismiss = onDismissEdit
        )
    }

    if (showFreezeDialog) {
        dialogCallback.showInputDialog(
            title = "Freeze All Values",
            defaultValue = "999999999",
            onConfirm = { input ->
                context?.let {
                    coroutineScope.launch {
                        Hunt().freezeall(savedAddresList, input, context)
                    }
                }
                onDismissFreeze()
            },
            onDismiss = onDismissFreeze
        )
    }

    selectedAddressIndex?.let { index ->
        dialogCallback.showInfoDialog(
            title = "Delete Address",
            message = "Delete this address from the list?",
            onConfirm = {
                savedAddresList.removeAt(index)
                onDismissAddress()
            },
            onDismiss = onDismissAddress
        )
    }

    selectedAddressInfo?.let { info ->
        dialogCallback.showInputDialog(
            title = "Edit Value",
            defaultValue = info.matchInfo.prevValue,
            onConfirm = { newValue ->
                val hgmem = HGMem()
                context?.let {
                    coroutineScope.launch {
                        hgmem.writeMem(
                            isattached().currentPid(),
                            info.matchInfo.address,
                            info.matchInfo.valuetype,
                            newValue,
                            context
                        )
                        refreshValue(context, dialogCallback)
                    }
                }
                onDismissValue()
            },
            onDismiss = onDismissValue
        )
    }
}

private suspend fun refreshValue(context: Context, dialogCallback: DialogCallback) {
    val mem = Memory()
    val pid = isattached().currentPid()
    val lastPid = isattached().lastPid()

    if (pid < 0) {
        dialogCallback.showInfoDialog(
            title = "Error",
            message = "No process attached",
            onConfirm = {},
            onDismiss = {}
        )
        return
    }

    if (!Process().processIsRunning(pid.toString())) {
        dialogCallback.showInfoDialog(
            title = "Error",
            message = "Process not running",
            onConfirm = {},
            onDismiss = {}
        )
        savedAddresList.clear()
        isattached().reset()
        isattached().resetLast()
        return
    }

    if (lastPid != -1 && lastPid != pid) {
        dialogCallback.showInfoDialog(
            title = "Info",
            message = "Process changed cleaning...",
            onConfirm = {},
            onDismiss = {}
        )
        savedAddresList.clear()
        isattached().resetLast()
        return
    }

    withContext(Dispatchers.IO) {
        savedAddresList.forEachIndexed { index, addressInfo ->
            try {
                val currentValue = mem.readMemory(
                    pid,
                    addressInfo.matchInfo.address,
                    addressInfo.matchInfo.valuetype,
                    context
                )
                savedAddresList[index] = addressInfo.copy(
                    matchInfo = addressInfo.matchInfo.copy(prevValue = currentValue)
                )
            } catch (e: Exception) {
                savedAddresList.removeAt(index)
            }
        }
    }
}

private fun AddressInfo.copy(matchInfo: MatchInfo = this.matchInfo): AddressInfo {
    return AddressInfo(matchInfo, this.numType)
}