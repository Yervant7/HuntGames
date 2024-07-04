package com.yervant.huntgames.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kuhakupixel.libuberalles.overlay.OverlayPermission
import com.yervant.huntgames.ui.theme.HuntGamesTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkFile()) {
            try {
                Toast.makeText(this@MainActivity, "Copying RWMem", Toast.LENGTH_SHORT).show()
                copyfile.FileUtil.copyAssetFileToInternalStorage(this, "RWMem", "bin/RWMem")
                copyfile.FileUtil.copyAssetFileToInternalStorage(this, "libkeystone.so", "bin/libkeystone.so")
                copyfile.FileUtil.copyAssetFileToInternalStorage(this, "libkeystone.so.0", "bin/libkeystone.so.0")
            } catch (e: IOException) {
                e.printStackTrace()
                throw IOException("failed to copy RWMem")
            }
        }
        rootCheck()
        modulecheck()
        executeRootCommand("chmod +x /data/data/com.yervant.huntgames/files/bin/RWMem")

        setContent {
            HuntGamesTheme(darkTheme = true) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        askForOverlayPermission = {
                            OverlayPermission.askForOverlayPermission(
                                context = applicationContext,
                                componentActivity = this,
                            )
                        },
                    )
                }
            }
        }
    }

    fun executeRootCommand(command: String): List<String> {
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

    private fun rootCheck() {
        val output = executeRootCommand("id")
        if (output.isEmpty()) {
            Toast.makeText(this@MainActivity, "APP Not Have Root Access", Toast.LENGTH_SHORT).show()
            throw Exception("root access missing")
        } else if (output[0].startsWith("uid=0")) {
            Toast.makeText(this@MainActivity, "APP Have Root Access", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "APP Not Have Root Access", Toast.LENGTH_SHORT).show()
            throw Exception("root access missing")
        }
    }

    private fun modulecheck() {
        val output = executeRootCommand("ls /dev/rwMem")
        if (output.contains("No such file or directory")) {
            Toast.makeText(this@MainActivity, "Module Not Found", Toast.LENGTH_SHORT).show()
            throw Exception("module not found")
        } else {
            Toast.makeText(this@MainActivity, "Module Found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkFile(): Boolean {
        val context = this@MainActivity
        val file = File(context.filesDir, "bin/RWMem")
        val file2 = File(context.filesDir, "bin/libkeystone.so")
        val file3 = File(context.filesDir, "bin/libkeystone.so.0")
        if (!file.exists() && !file2.exists() && !file3.exists()) {
            Toast.makeText(context, "RWMem not found", Toast.LENGTH_SHORT).show()
            return false
        } else {
            Toast.makeText(context, "RWMem found", Toast.LENGTH_SHORT).show()
            return true
        }
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HuntGamesTheme {
        Greeting("Android")
    }
}