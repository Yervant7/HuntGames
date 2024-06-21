package com.yervant.huntgames.ui.util

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * [onClose]: Called when dialog is dismissed or confirmed
 * [onConfirm]: Called when confirmed
 * */
@Composable
fun ShowDialog(
    title: String,
    body: @Composable () -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    showCancelButton: Boolean

) {

    AlertDialog(
        onDismissRequest = {
            onClose()
        },
        title = {
            Text(title)
        },
        text = body,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onClose()
                }

            ) { Text("Ok") }
        },
        dismissButton = {
            if (showCancelButton) {
                Button(
                    onClick = {
                        onClose()
                    }
                ) { Text("Cancel") }
            }
        },
    )
}

@Composable
fun ShowTextDialog(
    title: String,
    msg: String,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    showCancelButton: Boolean = false
) {

    ShowDialog(
        title = title,
        body = {
            Text(
                msg,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
            )
        },
        onClose = onClose, onConfirm = onConfirm, showCancelButton = showCancelButton,
    )
}

@Composable
fun InfoDialog(msg: String, onClose: () -> Unit, onConfirm: () -> Unit) {
    ShowTextDialog(title = "Info", msg = msg, onClose = onClose, onConfirm = onConfirm)
}

@Composable
fun WarningDialog(msg: String, onClose: () -> Unit, onConfirm: () -> Unit) {
    ShowTextDialog(title = "Warning", msg = msg, onClose = onClose, onConfirm = onConfirm)
}

@Composable
fun ErrorDialog(msg: String, onClose: () -> Unit, onConfirm: () -> Unit) {
    ShowTextDialog(title = "Error", msg = msg, onClose = onClose, onConfirm = onConfirm)
}

@Composable
fun ConfirmDialog(
    title: String = "Confirmation",
    msg: String,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    ShowTextDialog(
        title = title,
        msg = msg,
        onClose = onClose,
        onConfirm = onConfirm,
        showCancelButton = true,
    )
}
