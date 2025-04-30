package com.yervant.huntmem.ui.menu

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yervant.huntmem.backend.MemoryScanner
import com.yervant.huntmem.backend.MemoryScanner.MemoryRegion
import com.yervant.huntmem.backend.Process

private var regionsSelected: List<MemoryRegion> = listOf()
private var customRegionFilter: String? = null

fun getSelectedRegions(): List<MemoryRegion> {
    return regionsSelected.ifEmpty {
        listOf(
            MemoryRegion.ALLOC,
            MemoryRegion.BSS,
            MemoryRegion.DATA,
            MemoryRegion.HEAP,
            MemoryRegion.JAVA_HEAP,
            MemoryRegion.ANONYMOUS,
            MemoryRegion.STACK,
            MemoryRegion.ASHMEM
        )
    }
}

fun getCustomFilter(): String? {
    return customRegionFilter
}

fun setRegions(regions: List<MemoryRegion>, customFilter: String? = null) {
    regionsSelected = regions
    customRegionFilter = customFilter
}

fun calculateRegionSize(pid: Int, region: MemoryRegion, customFilter: String? = null): Float {
    val memoryMaps = MemoryScanner(pid).readMemoryMaps()
    val matchingRegions = memoryMaps.filter { region.matches(it, customFilter) }

    val totalBytes = matchingRegions.sumOf { it.end - it.start }
    return (totalBytes / (1024f * 1024f))
}

@SuppressLint("DefaultLocale")
fun formatSize(sizeInMb: Float): String {
    return String.format("%.2f MB", sizeInMb)
}

@Composable
fun SettingsMenu() {
    val regions = listOf(
        MemoryRegion.ALLOC,
        MemoryRegion.BSS,
        MemoryRegion.DATA,
        MemoryRegion.HEAP,
        MemoryRegion.JAVA_HEAP,
        MemoryRegion.ANONYMOUS,
        MemoryRegion.STACK,
        MemoryRegion.CODE_SYSTEM,
        MemoryRegion.ASHMEM
    )
    val selectedRegions = remember { mutableStateListOf<MemoryRegion>() }
    val customRegion = remember { mutableStateOf("") }

    val regionSizes = remember { mutableStateMapOf<MemoryRegion, Float>() }

    val pid = isattached().currentPid()
    val lastPid = isattached().lastPid()

    LaunchedEffect(pid != -1 && Process().processIsRunning(pid.toString()) && !(lastPid != -1 && lastPid != pid)) {
        regions.forEach { region ->
            regionSizes[region] = calculateRegionSize(pid, region)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(regions) { region ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedRegions.contains(region)) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedRegions.contains(region),
                                        onCheckedChange = { checked ->
                                            if (checked) selectedRegions.add(region)
                                            else selectedRegions.remove(region)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    Text(
                                        text = region.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = formatSize(regionSizes[region] ?: 0f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(90.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customRegion.value,
                            onValueChange = { customRegion.value = it },
                            label = { Text("Custom Region Filter") },
                            placeholder = { Text("Example: libunity.so") },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Button(
                            onClick = {
                                if (customRegion.value.isNotBlank()) {
                                    setRegions(listOf(MemoryRegion.CUSTOM), customRegion.value)
                                } else {
                                    setRegions(selectedRegions.toList())
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Save")
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}