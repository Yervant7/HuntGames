package com.yervant.huntgames.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SettingsMenu() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Setting",
            //fontSize = MaterialTheme.typography.h3.fontSize,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
@Preview
fun SettingsMenuPreview() {
    SettingsMenu()
}