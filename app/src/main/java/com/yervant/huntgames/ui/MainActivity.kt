package com.yervant.huntgames.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.InputStream


class MainActivity : ComponentActivity() {

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleFileImport(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!suCheck() || !modulecheck()) {
            Toast.makeText(this, "ERROR Root access or Module missing", Toast.LENGTH_SHORT).show()
        }

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
                        openFilePicker = { getContent.launch("*/*") }
                    )
                }
            }
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

    private fun modulecheck(): Boolean {
        val output = executeSuCommand("ls /dev/rwProcMem")
        if (output.isEmpty()) {
            return false
        } else {
            for (line in output) {
                if (line.startsWith("ls: /dev/rwProcMem:")) {
                    return false
                } else if (line.startsWith("/dev/rwProcMem")) {
                    return true
                }
            }
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

    private fun handleFileImport(uri: Uri) {
        val fileName = getFileName(uri)
        if (fileName.endsWith(".lua")) {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            inputStream?.let {
                val file = File(filesDir, fileName)
                val outputStream = FileOutputStream(file)
                it.copyTo(outputStream)
                outputStream.close()
                it.close()
            }
        } else {
            Toast.makeText(this@MainActivity, "Only import files .lua", Toast.LENGTH_SHORT).show()
            throw Exception("not a lua file")
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