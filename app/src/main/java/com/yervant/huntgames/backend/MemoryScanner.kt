package com.yervant.huntgames.backend

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.yervant.huntgames.ui.menu.MatchInfo
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MemoryScanner(private val pid: Int) {

    enum class MemoryRegion {
        ALLOC,
        BSS,
        DATA,
        HEAP,
        JAVA_HEAP,
        ANONYMOUS,
        CODE_SYSTEM,
        STACK,
        ASHMEM,
        CUSTOM;

        companion object {
            fun fromStr(s: String): MemoryRegion? {
                return entries.find { it.name == s.uppercase() }
            }
        }

        fun matches(entry: MemoryRegions, customFilter: String? = null): Boolean {
            return when (this) {
                ALLOC -> entry.path.contains("[anon:libc_malloc]")
                BSS -> entry.path.contains("[anon:.bss]")
                DATA -> entry.path.contains("/data/app/")
                HEAP -> entry.path.contains("[heap]")
                JAVA_HEAP -> entry.path.contains("/dev/ashmem")
                ANONYMOUS -> entry.path.isEmpty()
                CODE_SYSTEM -> entry.path.contains("/system")
                STACK -> entry.path.contains("[stack]")
                ASHMEM -> entry.path.contains("/dev/ashmem/dalvik")
                CUSTOM -> customFilter?.let { entry.path.contains(it) } ?: false
            }
        }
    }

    data class MemoryRegions(
        val start: Long,
        val end: Long,
        val permissions: String,
        val offset: Long,
        val device: String,
        val inode: Long,
        val path: String
    )

    private fun readMemoryMaps(): List<MemoryRegions> {
        val output = Shell.cmd("cat /proc/$pid/maps").exec()

        return output.out.map { line ->
            val parts = line.split("\\s+".toRegex())
            val addressRange = parts[0].split("-")

            MemoryRegions(
                start = addressRange[0].toLong(16),
                end = addressRange[1].toLong(16),
                permissions = parts[1],
                offset = parts[2].toLong(16),
                device = parts[3],
                inode = parts[4].toLongOrNull() ?: 0L,
                path = if (parts.size > 5) parts[5] else ""
            )
        }
    }

    private suspend fun <T> searchMemory(
        value: T,
        size: Int,
        dataType: String,
        context: Context,
        convert: (ByteArray) -> T,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null
    ): List<MatchInfo> {
        val foundAddresses = mutableListOf<Long>()
        val memoryRegions = readMemoryMaps()

        memoryRegions
            .filter { it.permissions.contains("r") } // Apenas regiões legíveis
            .filter { region ->
                regions?.let {
                    it.any { regionType ->
                        regionType.matches(MemoryRegions(
                            start = region.start,
                            end = region.end,
                            permissions = region.permissions,
                            offset = region.offset,
                            device = region.device,
                            inode = region.inode,
                            path = region.path
                        ), customFilter)
                    }
                } ?: true
            }
            .forEach { region ->
                var currentAddress = region.start
                val regionSize = region.end - region.start

                while (currentAddress < region.end) {
                    val readSize = minOf(regionSize, 100 * 1024 * 1024).toInt() // Leitura em blocos de 1MB

                    try {
                        HGMem().readMem(
                            pid,
                            currentAddress,
                            readSize.toLong(),
                            context
                        ).onSuccess { memorychunk ->
                            for (i in 0 until memorychunk.size - size) {
                                val bytes = memorychunk.sliceArray(i until i + size)
                                try {
                                    if (convert(bytes) == value) {
                                        foundAddresses.add(currentAddress + i)
                                    }
                                } catch (e: Exception) {
                                    e.message?.let { Log.d("MemoryScanner", it) }
                                }
                            }
                        }.onFailure { e ->
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    currentAddress += readSize.toLong()
                }
            }

        val res: MutableList<MatchInfo> = mutableListOf()
        foundAddresses.forEach { addr ->
            res.add(MatchInfo(addr, value.toString(), dataType))
        }
        return res
    }

    suspend fun searchInt(
        value: Int,
        dataType: String,
        context: Context,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null
    ): List<MatchInfo> {
        return searchMemory(value, 4, dataType, context, { bytes ->
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
        }, regions, customFilter)
    }

    suspend fun searchLong(
        value: Long,
        dataType: String,
        context: Context,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null
    ): List<MatchInfo> {
        return searchMemory(value, 8, dataType, context, { bytes ->
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).long
        }, regions, customFilter)
    }

    suspend fun searchFloat(
        value: Float,
        dataType: String,
        context: Context,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null
    ): List<MatchInfo> {
        return searchMemory(value, 4, dataType, context, { bytes ->
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float
        }, regions, customFilter)
    }

    suspend fun searchDouble(
        value: Double,
        dataType: String,
        context: Context,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null
    ): List<MatchInfo> {
        return searchMemory(value, 8, dataType, context, { bytes ->
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).double
        }, regions, customFilter)
    }

    suspend fun filterAddressesAuto(
        matchs: List<MatchInfo>,
        targetValue: String,
        context: Context
    ): List<MatchInfo> {
        return matchs.filter { match ->
            when (match.valuetype.lowercase()) {
                "int" -> {
                    val readValue = HGMem().readMem(pid, match.address, 4, context).getOrElse { null }
                    if (readValue == null) {
                        false
                    } else {
                        val value = ByteBuffer.wrap(readValue).order(ByteOrder.LITTLE_ENDIAN).int
                        value == targetValue.toInt()
                    }
                }
                "long" -> {
                    val readValue = HGMem().readMem(pid, match.address, 8, context).getOrElse { null }
                    if (readValue == null) {
                        false
                    } else {
                        val value = ByteBuffer.wrap(readValue).order(ByteOrder.LITTLE_ENDIAN).long
                        value == targetValue.toLong()
                    }
                }
                "float" -> {
                    val readValue = HGMem().readMem(pid, match.address, 4, context).getOrElse { null }
                    if (readValue == null) {
                        false
                    } else {
                        val value = ByteBuffer.wrap(readValue).order(ByteOrder.LITTLE_ENDIAN).float
                        value == targetValue.toFloat()
                    }
                }
                "double" -> {
                    val readValue = HGMem().readMem(pid, match.address, 8, context).getOrElse { null }
                    if (readValue == null) {
                        false
                    } else {
                        val value = ByteBuffer.wrap(readValue).order(ByteOrder.LITTLE_ENDIAN).double
                        value == targetValue.toDouble()
                    }
                }

                else -> { throw IllegalArgumentException("Unsupported data type: ${match.valuetype}") }
            }
        }
    }
}