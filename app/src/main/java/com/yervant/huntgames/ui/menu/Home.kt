package com.yervant.huntgames.ui.menu

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.yervant.huntgames.ui.HuntOverlayServiceEntry
import com.yervant.huntgames.ui.util.ConfirmDialog


@Composable
fun HomeMenu(askForOverlayPermission: () -> Unit) {
    val context: Context = LocalContext.current
    val showAskForDrawOverOtherApp: MutableState<Boolean> = remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = fun() {
                if (!Settings.canDrawOverlays(context)) {
                    showAskForDrawOverOtherApp.value = true
                    return
                }
                //
                startOverlayServiceEntry(context)
            },
        ) {
            Text(
                text = "Start Hunting",
                fontWeight = FontWeight.Bold,
            )
        }
    }
    if (showAskForDrawOverOtherApp.value) {
        ConfirmDialog(
            msg = "Please Grant Overlay draw app permission",
            onClose = { showAskForDrawOverOtherApp.value = false },
            onConfirm = {
                askForOverlayPermission()
            },
        )

    }
}

fun startOverlayServiceEntry(context: Context) {
    val intent = Intent(context.applicationContext, HuntOverlayServiceEntry::class.java)
    context.startForegroundService(intent)

}

@Composable
@Preview
fun HomeMenuPreview() {
    HomeMenu(
        askForOverlayPermission = fun() {
            //
        },
    )
}