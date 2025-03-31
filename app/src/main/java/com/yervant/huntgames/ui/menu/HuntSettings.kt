package com.yervant.huntgames.ui.menu

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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yervant.huntgames.backend.MemoryScanner.MemoryRegion

private var regionsSelected: List<MemoryRegion> = listOf()
private var customRegionFilter: String? = null

fun getSelectedRegions(): List<MemoryRegion> {
    return if (regionsSelected.isEmpty()) {
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
    } else {
        regionsSelected
    }
}

fun setRegions(regions: List<MemoryRegion>, customFilter: String? = null) {
    regionsSelected = regions
    customRegionFilter = customFilter
}

@Composable
fun SettingsMenu() {
    val words = listOf(
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
    val selectedWords = remember { mutableStateListOf<MemoryRegion>() }
    val customRegion = remember { mutableStateOf("") }

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
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        items(words) { region ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedWords.contains(region)) {
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
                                        checked = selectedWords.contains(region),
                                        onCheckedChange = { checked ->
                                            if (checked) selectedWords.add(region)
                                            else selectedWords.remove(region)
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
                                    setRegions(selectedWords.toList())
                                }
                            },
                            modifier = Modifier
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}