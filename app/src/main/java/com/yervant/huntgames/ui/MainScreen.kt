package com.yervant.huntgames.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

@Composable
fun MainScreen(openBootPicker: () -> Unit) {
    val ctx = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Settings", "Patch Boot")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }

        when (selectedTab) {
            0 -> SettingsTab(ctx)
            1 -> PatchBootTab(openBootPicker, ctx)
        }
    }
}

@Composable
private fun SettingsTab(ctx: Context) {
    var textInput by remember { mutableStateOf("") }

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

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Enter a key") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                saveSharedKey(ctx, "user_key", textInput)
                Toast.makeText(ctx, "Key saved", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Key")
        }

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
    }
}

@Composable
private fun PatchBootTab(
    openBootPicker: () -> Unit,
    ctx: Context
) {
    val logs = remember { mutableStateListOf<String>() }
    var isPatching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Boot Image Patching", style = MaterialTheme.typography.titleMedium)

                Button(
                    onClick = openBootPicker,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPatching
                ) {
                    Text("Select Boot Image")
                }

                Button(
                    onClick = {
                        scope.launch {
                            isPatching = true
                            logs.clear()
                            try {
                                var key = "12345678"
                                val skey = getSharedKey(ctx, "user_key")
                                if (!skey.isNullOrEmpty()) {
                                    key = skey
                                }
                                val result = patchBootImage(ctx, key, logs)
                                if (result) {
                                    logs.add("✅ Patching completed successfully!")
                                    logs.add("Check downloads folder for patched_boot")
                                    copyFileToDownloads(ctx, "patched_boot.img")
                                } else {
                                    logs.add("❌ Patching failed!")
                                }
                            } finally {
                                isPatching = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPatching
                ) {
                    Text("Start Patching")
                }
            }
        }

        OutlinedCard(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs.size) { index ->
                    Text(
                        text = logs[index],
                        style = MaterialTheme.typography.bodySmall,
                        color = if (logs[index].startsWith("✅")) Color.Green else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun executeShell(cmd: String, allowNonZeroExit: Boolean = false): String {
    val res = Shell.cmd(cmd).exec()
    return if (res.code == 0 || allowNonZeroExit) {
        res.out.joinToString("\n")
    } else {
        "Error executing command: $cmd\nExit Code: ${res.code}\n${res.err.joinToString("\n")}"
    }
}

private suspend fun patchBootImage(
    context: Context,
    superkey: String,
    logs: SnapshotStateList<String>
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "patch")
            executeShell("cd ${dir.absolutePath}")
            var log = executeShell("${dir.absolutePath}/magiskboot unpack ${dir.absolutePath}/boot.img")
            logs.add(log)
            var kpimgver = "kpimg"

            log = executeShell("${dir.absolutePath}/kptools -c -i ${dir.absolutePath}/kernel", allowNonZeroExit = true)
            if (log.contains("is PATCHED.")) {
                kpimgver = "kpimg-with-kp"
            }
            logs.add(log)

            log = executeShell("${dir.absolutePath}/kptools -p -i ${dir.absolutePath}/kernel -S \"$superkey\" -k ${dir.absolutePath}/$kpimgver -o ${dir.absolutePath}/new-kernel")
            logs.add(log)
            log = executeShell("rm ${dir.absolutePath}/kernel")
            logs.add(log)
            log = executeShell("mv ${dir.absolutePath}/new-kernel ${dir.absolutePath}/kernel")
            logs.add(log)
            log = executeShell("${dir.absolutePath}/magiskboot repack ${dir.absolutePath}/boot.img")
            logs.add(log)
            log = executeShell("mv ${dir.absolutePath}/new-boot.img ${dir.absolutePath}/patched_boot.img")
            logs.add(log)

            logs.add("Boot image patched successfully!")
            true
        } catch (e: Exception) {
            logs.add("Error: ${e.message}")
            false
        }
    }
}

fun copyFileToDownloads(context: Context, fileName: String) {
    val dir = File(context.filesDir, "patch")
    val sourceFile = File(dir, fileName)

    if (!sourceFile.exists()) {
        Log.e("CopyToDownloads", "File not found!")
        return
    }

    val resolver = context.contentResolver
    val fileMimeType = "*/*"

    val outputStream: OutputStream?

    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, fileMimeType)
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }

    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

    outputStream = uri?.let { resolver.openOutputStream(it) }

    outputStream?.use { output ->
        FileInputStream(sourceFile).use { input ->
            input.copyTo(output)
        }
    }
}

fun saveSharedKey(context: Context, key: String, value: String) {
    val sharedPreferences = context.getSharedPreferences(
        "hg_prefs",
        Context.MODE_PRIVATE
    )
    with(sharedPreferences.edit()) {
        putString(key, value)
        apply()
    }
}

fun getSharedKey(context: Context, key: String): String? {
    val sharedPreferences = context.getSharedPreferences(
        "hg_prefs",
        Context.MODE_PRIVATE
    )
    return sharedPreferences.getString(key, null)
}