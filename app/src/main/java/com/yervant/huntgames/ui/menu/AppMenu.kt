package com.yervant.huntgames.ui.menu

import com.kuhakupixel.libuberalles.overlay.OverlayContext
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yervant.huntgames.R
import com.kuhakupixel.libuberalles.overlay.service.dialog.OverlayInfoDialog
import com.yervant.huntgames.backend.rwProcMem
import com.yervant.huntgames.ui.HuntOverlayServiceEntry
import kotlinx.coroutines.*
import com.yervant.huntgames.ui.util.CreateTable

private var attachedStatusString: MutableState<String> = mutableStateOf("None")
private var svpkg: String = ""
private var svpid: Long = -1L

fun getInstalledApps(packageManager: PackageManager): List<PackageInfo> {
    return packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
}

fun savepid(): Long {
    return svpid
}

@Composable
fun AppMenu(overlayContext: OverlayContext?) {
    val packageManager = overlayContext!!.service.packageManager
    val currentAppList = remember { mutableStateListOf<PackageInfo>() }
    var searchTerm by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val installedApps = getInstalledApps(packageManager)
                withContext(Dispatchers.Main) {
                    currentAppList.addAll(installedApps.sortedBy {
                        it.applicationInfo.loadLabel(packageManager).toString()
                    })
                }
            }
        }
    }

    val filteredApps = remember(searchTerm, currentAppList) {
        if (searchTerm.isEmpty()) {
            currentAppList.sortedBy {
                it.applicationInfo.loadLabel(packageManager).toString()
            }
        } else {
            currentAppList.filter {
                it.applicationInfo.loadLabel(packageManager)
                    .toString()
                    .contains(searchTerm, ignoreCase = true)
            }.sortedBy {
                it.applicationInfo.loadLabel(packageManager).toString()
            }
        }
    }

    Column {
        TextField(
            value = searchTerm,
            onValueChange = { searchTerm = it },
            label = { Text("Search apps") },
            modifier = Modifier.fillMaxWidth()
        )

        _AppMenu(
            installedApps = filteredApps,
            onAppSelected = { packageName ->
                svpkg = packageName
                attachedStatusString.value = packageName
                OverlayInfoDialog(overlayContext!!).show(
                    title = "App selected: $packageName",
                    onConfirm = {
                        coroutineScope.launch {
                            val rwmem = rwProcMem()
                            rwmem.getpid(svpkg, overlayContext) { pid ->
                                svpid = pid
                            }
                        }
                    },
                    text = "",
                )
            },
            onRefreshClicked = {
                currentAppList.clear()
                val installedApps = getInstalledApps(packageManager)
                currentAppList.addAll(installedApps.sortedBy {
                    it.applicationInfo.loadLabel(packageManager).toString()
                })
            }
        )
    }
}

@Composable
fun AppTable(
    appList: List<PackageInfo>,
    onAppSelected: (packageName: String) -> Unit,
) {
    CreateTable(
        modifier = Modifier.padding(16.dp),
        colNames = listOf("Name", "Package"),
        colWeights = listOf(0.5f, 0.5f),
        itemCount = appList.size,
        onRowClicked = { rowIndex: Int ->
            onAppSelected(appList[rowIndex].packageName)
        },
        drawCell = { rowIndex: Int, colIndex: Int ->
            when (colIndex) {
                0 -> Text(text = appList[rowIndex].applicationInfo.loadLabel(LocalContext.current.packageManager).toString())
                1 -> Text(text = appList[rowIndex].packageName)
            }
        }
    )
}

@Composable
private fun _AppMenuContent(
    installedApps: List<PackageInfo>,
    onRefreshClicked: () -> Unit,
    onAppSelected: (packageName: String) -> Unit,
    buttonContainer: @Composable (content: @Composable () -> Unit) -> Unit
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Text("App selected: ${attachedStatusString.value}")
    }
    buttonContainer {
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Text("App selected: ${attachedStatusString.value}")
        }
        Button(onClick = onRefreshClicked, modifier = Modifier.padding(start = 10.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_refresh),
                contentDescription = "Refresh",
            )
        }
    }
    AppTable(
        appList = installedApps,
        onAppSelected = onAppSelected,
    )
}

@Composable
private fun _AppMenu(
    installedApps: List<PackageInfo>,
    onRefreshClicked: () -> Unit,
    onAppSelected: (packageName: String) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Column(modifier = Modifier.fillMaxSize()) {
                _AppMenuContent(
                    installedApps = installedApps,
                    onRefreshClicked = onRefreshClicked,
                    onAppSelected = onAppSelected,
                    buttonContainer = { content ->
                        Row(content = { content() })
                    }
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                _AppMenuContent(
                    installedApps = installedApps,
                    onRefreshClicked = onRefreshClicked,
                    onAppSelected = onAppSelected,
                    buttonContainer = { content ->
                        Column(content = { content() })
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAppTable() {
    val packageManager = LocalContext.current.packageManager
    val installedApps = getInstalledApps(packageManager)
    AppTable(
        appList = installedApps,
    ) { packageName: String ->
    }
}

@Composable
@Preview
fun AppMenuPreview() {
    AppMenu(null)
}
