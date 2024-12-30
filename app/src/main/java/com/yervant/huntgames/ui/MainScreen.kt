package com.yervant.huntgames.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(openFilePicker: () -> Unit) {
    val ctx = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Happy Hunting!",
                style = MaterialTheme.typography.headlineMedium
            )

            Button(
                onClick = {
                    val serviceIntent = Intent(ctx, OverlayService::class.java)
                    ctx.startForegroundService(serviceIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Hunting")
            }

            Button(
                onClick = {
                    val stopServiceIntent = Intent(ctx, OverlayService::class.java)
                    ctx.stopService(stopServiceIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Hunting")
            }

            Button(
                onClick = openFilePicker,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import lua file")
            }
        }
    }
}