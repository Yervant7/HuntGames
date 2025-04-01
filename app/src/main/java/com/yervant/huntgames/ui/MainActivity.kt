package com.yervant.huntgames.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.activity.compose.setContent
import com.yervant.huntgames.ui.theme.HuntGamesTheme
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.core.net.toUri
import java.io.IOException


class MainActivity : ComponentActivity() {

    private val getContentBoot = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleBootImport(it) }
    }

    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (Settings.canDrawOverlays(this)) {
                showMainScreen()
            } else {
                Toast.makeText(this, "Permissão para exibir sobreposição negada.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        if (!suCheck()) {
            Toast.makeText(this, "ERROR Root access missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val dir = File(filesDir, "patch")
        if (dir.isDirectory) {
            executeSuCommand("rm -rf ${dir.absolutePath}/*")
        } else {
            dir.mkdirs()
        }

        val file = File(filesDir, "patch/magiskboot")
        if (!file.exists()) {
            copyfile.FileUtil.copyAssetFileToInternalStorage(this, "magiskboot", "patch/magiskboot")
        }
        val f = file.absolutePath
        executeSuCommand("chmod +x $f")
        val fil = File(filesDir, "patch/kptools")
        if (!fil.exists()) {
            copyfile.FileUtil.copyAssetFileToInternalStorage(this, "kptools", "patch/kptools")
        }
        val fi = fil.absolutePath
        executeSuCommand("chmod +x $fi")
        val afil = File(filesDir, "patch/kpimg")
        if (!afil.exists()) {
            copyfile.FileUtil.copyAssetFileToInternalStorage(this, "kpimg", "patch/kpimg")
        }
        val bfil = File(filesDir, "patch/kpimg-with-kp")
        if (!bfil.exists()) {
            copyfile.FileUtil.copyAssetFileToInternalStorage(this, "kpimg-with-kp", "patch/kpimg-with-kp")
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            showMainScreen()
        }
    }

    private fun showMainScreen() {
        setContent {
            HuntGamesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        openBootPicker = { getContentBoot.launch("*/*") }
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    private fun suCheck(): Boolean {
        val output = executeSuCommand("id")
        if (output.isEmpty()) {
            Log.d("MainActivity", "APP Not Have Root Access")
        } else if (output[0].startsWith("uid=0")) {
            Log.d("MainActivity", "APP Have Root Access")
            return true
        } else {
            Log.d("MainActivity", "APP Not Have Root Access")
        }
        return false
    }

    private fun executeSuCommand(command: String): List<String> {
        val output = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.inputStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    output.add(line)
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return output
    }

    private fun handleBootImport(uri: Uri) {
        val fileName = getFileName(uri)
        if (fileName.endsWith(".img")) {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            inputStream?.let {
                val file = File(filesDir, "patch/boot.img")
                if (file.exists()) {
                    file.delete()
                }
                val outputStream = FileOutputStream(file)
                it.copyTo(outputStream)
                outputStream.close()
                it.close()
            }
        } else {
            Toast.makeText(this@MainActivity, "Only import files .img", Toast.LENGTH_SHORT).show()
            throw Exception("not a img file")
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return name
    }
}

class copyfile {

    object FileUtil {

        @Throws(IOException::class)
        fun copyAssetFileToInternalStorage(context: Context, assetFileName: String, internalFilePath: String) {
            context.assets.open(assetFileName).use { inputStream ->
                val internalFile = File(context.filesDir, internalFilePath)
                internalFile.parentFile?.mkdirs()
                FileOutputStream(internalFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
}