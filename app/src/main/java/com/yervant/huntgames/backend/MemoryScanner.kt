package com.yervant.huntgames.backend

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.yervant.huntgames.ui.menu.MatchInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

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

    private suspend inline fun <reified T : Number> searchMemory(
        value: T,
        size: Int,
        dataType: String,
        context: Context,
        noinline converter: (ByteArray) -> T,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null
    ): List<MatchInfo> {
        val targetBytes = when (T::class) {
            Int::class -> ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value as Int)
                .array()
            Long::class -> ByteBuffer.allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(value as Long)
                .array()
            Float::class -> ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putFloat(value as Float)
                .array()
            Double::class -> ByteBuffer.allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putDouble(value as Double)
                .array()
            else -> throw IllegalArgumentException("Unsupported type")
        }

        return searchMemoryBytes(
            targetBytes = targetBytes,
            size = size,
            dataType = dataType,
            context = context,
            converter = converter,
            regions = regions,
            customFilter = customFilter
        )
    }

    private suspend fun <T : Number> searchMemoryBytes(
        targetBytes: ByteArray,
        size: Int,
        dataType: String,
        context: Context,
        converter: (ByteArray) -> T,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null
    ): List<MatchInfo> {
        val memoryRegions = readMemoryMaps()
        val channel = Channel<MatchInfo>(Channel.UNLIMITED)

        coroutineScope {
            val jobs = memoryRegions
                .filter { it.permissions.contains("r") }
                .filter { region ->
                    regions?.any { regionType ->
                        regionType.matches(region, customFilter)
                    } ?: true
                }
                .map { region ->
                    launch(Dispatchers.Default) {
                        processRegionBytes(
                            region = region,
                            targetBytes = targetBytes,
                            size = size,
                            context = context,
                            channel = channel,
                            converter = converter,
                            dataType = dataType
                        )
                    }
                }

            jobs.joinAll()
            channel.close()
        }

        return channel.toList()
    }

    private suspend fun <T : Number> processRegionBytes(
        region: MemoryRegions,
        targetBytes: ByteArray,
        size: Int,
        context: Context,
        channel: SendChannel<MatchInfo>,
        converter: (ByteArray) -> T,
        dataType: String
    ) = coroutineScope {
        var currentAddress = region.start
        val regionSize = region.end - region.start

        while (currentAddress < region.end && isActive) {
            val readSize = minOf(regionSize, 100 * 1024 * 1024).toInt()
            val chunkAddress = currentAddress

            launch(Dispatchers.IO) {
                HGMem().readMem(pid, chunkAddress, readSize.toLong(), context)
                    .onSuccess { memoryChunk ->
                        processChunkBytes(
                            memoryChunk,
                            chunkAddress,
                            targetBytes,
                            size,
                            converter,
                            dataType,
                            channel
                        )
                    }
            }

            currentAddress += readSize.toLong()
        }
    }

    private suspend fun <T : Number> processChunkBytes(
        memoryChunk: ByteArray,
        chunkAddress: Long,
        targetBytes: ByteArray,
        size: Int,
        converter: (ByteArray) -> T,
        dataType: String,
        channel: SendChannel<MatchInfo>
    ) = coroutineScope {
        val chunkLength = memoryChunk.size - size
        if (chunkLength <= 0) return@coroutineScope

        val numParts = Runtime.getRuntime().availableProcessors() * 2
        val baseSize = chunkLength / numParts
        val remainder = chunkLength % numParts

        (0 until numParts)
            .map { part ->
                async(Dispatchers.Default) {
                    val start = part * baseSize + if (part < remainder) part else remainder
                    val end = start + baseSize + if (part < remainder) 1 else 0
                    val matches = mutableListOf<MatchInfo>()

                    for (i in start until end) {
                        if (matchesTarget(memoryChunk, i, targetBytes)) {
                            val valueBytes = memoryChunk.copyOfRange(i, i + size)
                            val value = converter(valueBytes)
                            matches.add(MatchInfo(UUID.randomUUID().toString(), chunkAddress + i, value, dataType))
                        }
                    }
                    matches
                }
            }
            .awaitAll()
            .flatten()
            .forEach {
                try {
                    channel.send(it)
                } catch (_: ClosedSendChannelException) {
                }
            }
    }

    private fun matchesTarget(memory: ByteArray, index: Int, target: ByteArray): Boolean {
        for (j in target.indices) {
            if (memory[index + j] != target[j]) return false
        }
        return true
    }

    suspend fun searchInt(
        value: Int,
        dataType: String,
        context: Context,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null
    ): List<MatchInfo> {
        return searchMemory(value, 4, dataType, context,
            { bytes -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int },
            regions, customFilter
        )
    }

    suspend fun searchLong(
        value: Long,
        dataType: String,
        context: Context,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null
    ): List<MatchInfo> {
        return searchMemory(value, 8, dataType, context,
            { bytes -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).long },
            regions, customFilter
        )
    }

    suspend fun searchFloat(
        value: Float,
        dataType: String,
        context: Context,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null
    ): List<MatchInfo> {
        return searchMemory(value, 4, dataType, context,
            { bytes -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float },
            regions, customFilter
        )
    }

    suspend fun searchDouble(
        value: Double,
        dataType: String,
        context: Context,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null
    ): List<MatchInfo> {
        return searchMemory(value, 8, dataType, context,
            { bytes -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).double },
            regions, customFilter
        )
    }

    suspend fun filterAddressesAuto(
        matches: List<MatchInfo>,
        targetValue: String,
        context: Context
    ): List<MatchInfo> = coroutineScope {
        matches
            .map { match ->
                async {
                    val isValid = when (match.valuetype.lowercase()) {
                        "int" -> readAndCompare<Int>(match, 4, targetValue, context)
                        "long" -> readAndCompare<Long>(match, 8, targetValue, context)
                        "float" -> readAndCompare<Float>(match, 4, targetValue, context)
                        "double" -> readAndCompare<Double>(match, 8, targetValue, context)
                        else -> false
                    }
                    isValid to match
                }
            }
            .awaitAll()
            .filter { it.first }
            .map { it.second }
    }

    private suspend inline fun <reified T> readAndCompare(
        match: MatchInfo,
        bytes: Int,
        target: String,
        context: Context
    ): Boolean {
        return HGMem().readMem(pid, match.address, bytes.toLong(), context)
            .getOrNull()
            ?.let { memoryBytes ->
                try {
                    val value = ByteBuffer.wrap(memoryBytes)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .let { buf ->
                            when (T::class) {
                                Int::class -> buf.int
                                Long::class -> buf.long
                                Float::class -> buf.float
                                Double::class -> buf.double
                                else -> null
                            }
                        }

                    when (T::class) {
                        Int::class -> value == target.toIntOrNull()
                        Long::class -> value == target.toLongOrNull()
                        Float::class -> value == target.toFloatOrNull()
                        Double::class -> value == target.toDoubleOrNull()
                        else -> false
                    }
                } catch (e: Exception) {
                    Log.e("MemoryScanner", "Error comparing values", e)
                    false
                }
            } ?: false
    }
}