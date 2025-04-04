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
        customFilter: String? = null,
        operator: String = "="
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
            customFilter = customFilter,
            operator = operator,
            value = value
        )
    }

    private suspend fun <T : Number> searchMemoryBytes(
        targetBytes: ByteArray,
        size: Int,
        dataType: String,
        context: Context,
        converter: (ByteArray) -> T,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null,
        operator: String = "=",
        value: T
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
                            dataType = dataType,
                            operator = operator,
                            targetValue = value
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
        dataType: String,
        operator: String = "=",
        targetValue: T
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
                            channel,
                            operator,
                            targetValue
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
        channel: SendChannel<MatchInfo>,
        operator: String = "=",
        targetValue: T
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
                        val valueBytes = memoryChunk.copyOfRange(i, i + size)
                        val value = converter(valueBytes)

                        if (operator == "=" && matchesTarget(memoryChunk, i, targetBytes)) {
                            matches.add(MatchInfo(UUID.randomUUID().toString(), chunkAddress + i, value, dataType))
                        } else if (operator != "=" && compareValuesTypeSafe(value, targetValue, operator)) {
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

    suspend fun searchGroupValues(
        groupQuery: String,
        dataType: String,
        context: Context,
        regions: List<MemoryRegion>? = null,
        operator: String = "="
    ): List<MatchInfo> = coroutineScope {
        val parts = groupQuery.split(":")
        val valuesStr = parts[0]
        val range = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0

        val individualValues = valuesStr.split(";")
            .filter { it.isNotEmpty() }
            .map { it.trim() }

        val resultChannel = Channel<MatchInfo>(Channel.UNLIMITED)

        val searchJobs = individualValues.map { valueStr ->
            launch {
                try {
                    val results = when (dataType.lowercase()) {
                        "int" -> searchInt(valueStr.toInt(), dataType, context, regions, operator = operator)
                        "long" -> searchLong(valueStr.toLong(), dataType, context, regions, operator = operator)
                        "float" -> searchFloat(valueStr.toFloat(), dataType, context, regions, operator = operator)
                        "double" -> searchDouble(valueStr.toDouble(), dataType, context, regions, operator = operator)
                        else -> emptyList()
                    }

                    if (range > 0) {
                        results.forEach { match ->
                            resultChannel.send(match)

                            val valueToCompare = when (dataType.lowercase()) {
                                "int" -> match.prevValue.toInt()
                                "long" -> match.prevValue.toLong()
                                "float" -> match.prevValue.toFloat()
                                "double" -> match.prevValue.toDouble()
                                else -> return@forEach
                            }

                            val typeSize = when (dataType.lowercase()) {
                                "int", "float" -> 4
                                "long", "double" -> 8
                                else -> 4
                            }

                            for (offset in 1..range) {
                                searchAdditionalAddresses(match.address + offset * typeSize, valueToCompare, dataType, context, resultChannel, operator)
                                searchAdditionalAddresses(match.address - offset * typeSize, valueToCompare, dataType, context, resultChannel, operator)
                            }
                        }
                    } else {
                        results.forEach { resultChannel.send(it) }
                    }
                } catch (e: Exception) {
                    Log.e("MemoryScanner", "Error searching for value $valueStr with operator $operator: ${e.message}")
                }
            }
        }

        joinAll(*searchJobs.toTypedArray())
        resultChannel.close()

        resultChannel.toList().distinctBy { it.address }
    }

    private suspend fun searchAdditionalAddresses(
        address: Long,
        baseValue: Number,
        dataType: String,
        context: Context,
        resultChannel: SendChannel<MatchInfo>,
        operator: String = "="
    ) {
        try {
            val mem = HGMem()
            val size = when (dataType.lowercase()) {
                "int", "float" -> 4L
                "long", "double" -> 8L
                else -> return
            }

            mem.readMem(pid, address, size, context).onSuccess { bytes ->
                if (bytes.size < size) return@onSuccess

                when (dataType.lowercase()) {
                    "int" -> {
                        val currentInt = ByteBuffer.wrap(bytes)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .int
                        val targetInt = baseValue.toInt()
                        val shouldInclude = compareValues(currentInt, targetInt, operator)
                        if (shouldInclude) {
                            sendMatchInfo(resultChannel, address, currentInt, dataType)
                        }
                    }
                    "long" -> {
                        val currentLong = ByteBuffer.wrap(bytes)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .long
                        val targetLong = baseValue.toLong()
                        val shouldInclude = compareValues(currentLong, targetLong, operator)
                        if (shouldInclude) {
                            sendMatchInfo(resultChannel, address, currentLong, dataType)
                        }
                    }
                    "float" -> {
                        val currentFloat = ByteBuffer.wrap(bytes)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .float
                        val targetFloat = baseValue.toFloat()
                        val shouldInclude = compareValues(currentFloat, targetFloat, operator)
                        if (shouldInclude) {
                            sendMatchInfo(resultChannel, address, currentFloat, dataType)
                        }
                    }
                    "double" -> {
                        val currentDouble = ByteBuffer.wrap(bytes)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .double
                        val targetDouble = baseValue.toDouble()
                        val shouldInclude = compareValues(currentDouble, targetDouble, operator)
                        if (shouldInclude) {
                            sendMatchInfo(resultChannel, address, currentDouble, dataType)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MemoryScanner", "Error reading additional address $address: ${e.message}")
        }
    }

    private suspend fun sendMatchInfo(
        channel: SendChannel<MatchInfo>,
        address: Long,
        value: Number,
        dataType: String
    ) {
        val matchInfo = MatchInfo(
            UUID.randomUUID().toString(),
            address,
            value,
            dataType
        )
        try {
            channel.send(matchInfo)
        } catch (_: ClosedSendChannelException) {
        }
    }

    suspend fun filterValues(
        matches: List<MatchInfo>,
        filterExpression: String,
        context: Context,
        customOperator: String? = null
    ): List<MatchInfo> = coroutineScope {
        val operatorPattern = """([<>=!]+)(.+)""".toRegex()
        val operatorMatch = operatorPattern.find(filterExpression.trim())

        val operator = customOperator ?: operatorMatch?.groupValues?.get(1) ?: "="
        val valueStr = if (customOperator != null) {
            filterExpression.trim()
        } else {
            operatorMatch?.groupValues?.get(2)?.trim() ?: filterExpression.trim()
        }

        matches.map { match ->
            async {
                val currentValue = readCurrentValue(match, context) ?: return@async null

                val isMatch = when (match.valuetype.lowercase()) {
                    "int" -> {
                        val targetValue = valueStr.toIntOrNull() ?: return@async null
                        compareValues(currentValue.toInt(), targetValue, operator)
                    }
                    "long" -> {
                        val targetValue = valueStr.toLongOrNull() ?: return@async null
                        compareValues(currentValue.toLong(), targetValue, operator)
                    }
                    "float" -> {
                        val targetValue = valueStr.toFloatOrNull() ?: return@async null
                        compareValues(currentValue.toFloat(), targetValue, operator)
                    }
                    "double" -> {
                        val targetValue = valueStr.toDoubleOrNull() ?: return@async null
                        compareValues(currentValue.toDouble(), targetValue, operator)
                    }
                    else -> false
                }

                if (isMatch) match.copy(prevValue = currentValue) else null
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun readCurrentValue(match: MatchInfo, context: Context): Number? {
        val size = when (match.valuetype.lowercase()) {
            "int", "float" -> 4L
            "long", "double" -> 8L
            else -> 4L
        }

        val result = HGMem().readMem(pid, match.address, size, context)
        val bytes = result.getOrNull()

        if (bytes == null || bytes.isEmpty() || bytes.size < size) {
            return null
        }

        return try {
            when (match.valuetype.lowercase()) {
                "int" -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
                "long" -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).long
                "float" -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float
                "double" -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).double
                else -> null
            }
        } catch (e: Exception) {
            Log.e("MemoryScanner", "Error reading current value at ${match.address}: ${e.message}")
            null
        }
    }

    private fun compareValuesTypeSafe(value: Number, target: Number, operator: String): Boolean {
        return when {
            value is Int && target is Int -> compareValues(value, target, operator)
            value is Long && target is Long -> compareValues(value, target, operator)
            value is Float && target is Float -> compareValues(value, target, operator)
            value is Double && target is Double -> compareValues(value, target, operator)
            // Handle mixed numeric types by converting to double
            else -> compareValues(value.toDouble(), target.toDouble(), operator)
        }
    }

    private fun <T> compareValues(value: T, target: T, operator: String): Boolean
            where T : Comparable<T> {
        return when (operator) {
            "=" -> value == target
            "==" -> value == target
            "!=" -> value != target
            ">" -> value > target
            "<" -> value < target
            ">=" -> value >= target
            "<=" -> value <= target
            else -> false
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
        customFilter: String? = null,
        operator: String = "="
    ): List<MatchInfo> {
        return searchMemory(value, 4, dataType, context,
            { bytes -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int },
            regions, customFilter, operator
        )
    }

    suspend fun searchLong(
        value: Long,
        dataType: String,
        context: Context,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null,
        operator: String = "="
    ): List<MatchInfo> {
        return searchMemory(value, 8, dataType, context,
            { bytes -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).long },
            regions, customFilter, operator
        )
    }

    suspend fun searchFloat(
        value: Float,
        dataType: String,
        context: Context,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null,
        operator: String = "="
    ): List<MatchInfo> {
        return searchMemory(value, 4, dataType, context,
            { bytes -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float },
            regions, customFilter, operator
        )
    }

    suspend fun searchDouble(
        value: Double,
        dataType: String,
        context: Context,
        regions: List<MemoryRegion>? = null,
        customFilter: String? = null,
        operator: String = "="
    ): List<MatchInfo> {
        return searchMemory(value, 8, dataType, context,
            { bytes -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).double },
            regions, customFilter, operator
        )
    }

    suspend fun filterAddressesAuto(
        matches: List<MatchInfo>,
        targetValue: String,
        context: Context,
        operator: String = "="
    ): List<MatchInfo> = coroutineScope {
        val operatorPattern = """([<>=!]+)(.+)""".toRegex()
        val operatorMatch = operatorPattern.find(targetValue.trim())

        val actualOperator = operatorMatch?.groupValues?.get(1) ?: operator
        val valueStr = operatorMatch?.groupValues?.get(2)?.trim() ?: targetValue.trim()

        matches
            .map { match ->
                async {
                    val isValid = when (match.valuetype.lowercase()) {
                        "int" -> readAndCompare<Int>(match, 4, valueStr, context, actualOperator)
                        "long" -> readAndCompare<Long>(match, 8, valueStr, context, actualOperator)
                        "float" -> readAndCompare<Float>(match, 4, valueStr, context, actualOperator)
                        "double" -> readAndCompare<Double>(match, 8, valueStr, context, actualOperator)
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
        context: Context,
        operator: String = "="
    ): Boolean where T : Number, T : Comparable<T> {
        return HGMem().readMem(pid, match.address, bytes.toLong(), context)
            .fold(
                onSuccess = { memoryBytes ->
                    if (memoryBytes.size < bytes) return false

                    try {
                        val buffer = ByteBuffer.wrap(memoryBytes).order(ByteOrder.LITTLE_ENDIAN)

                        // Parse the current value from memory
                        val value: T = when (T::class) {
                            Int::class -> buffer.int as T
                            Long::class -> buffer.long as T
                            Float::class -> buffer.float as T
                            Double::class -> buffer.double as T
                            else -> return false
                        }

                        // Parse the target value from string
                        val targetValue: T = when (T::class) {
                            Int::class -> target.toIntOrNull() as T?
                            Long::class -> target.toLongOrNull() as T?
                            Float::class -> target.toFloatOrNull() as T?
                            Double::class -> target.toDoubleOrNull() as T?
                            else -> null
                        } ?: return false

                        // Compare the values
                        compareValues(value, targetValue, operator)

                    } catch (e: Exception) {
                        Log.e("MemoryScanner", "Error comparing values: ${e.message}", e)
                        false
                    }
                },
                onFailure = { false }
            )
    }
}