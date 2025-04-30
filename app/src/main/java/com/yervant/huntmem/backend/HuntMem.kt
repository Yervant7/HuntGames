package com.yervant.huntmem.backend

import android.content.Context
import android.util.Log
import com.yervant.huntmem.backend.MemoryScanner.MemoryRegions
import com.yervant.huntmem.ui.getSharedKey
import com.yervant.huntmem.ui.menu.MatchInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class HuntMem {
    private val defaultKey = "yervant7github"

    suspend fun readMem(
        pid: Int,
        addr: Long,
        size: Long,
        context: Context
    ): Result<ByteArray> = runCatching {
        withContext(Dispatchers.IO) {
            var key = defaultKey
            val skey = getSharedKey(context, "user_key")
            if (!skey.isNullOrEmpty()) {
                key = skey
            }
            readMemory(pid, addr, size, key)
        }
    }.onFailure {
        Log.e(TAG, "Error reading", it)
    }

    suspend fun writeMem(
        pid: Int,
        address: Long,
        datatype: String,
        value: String,
        context: Context
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            var key = defaultKey
            val skey = getSharedKey(context, "user_key")
            if (!skey.isNullOrEmpty()) {
                key = skey
            }

            val byteArray = when (datatype.lowercase()) {
                "int" -> {
                    val intValue = value.toInt()
                    ByteBuffer.allocate(4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(intValue)
                        .array()
                }
                "long" -> {
                    val longValue = value.toLong()
                    ByteBuffer.allocate(8)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putLong(longValue)
                        .array()
                }
                "float" -> {
                    val floatValue = value.toFloat()
                    ByteBuffer.allocate(4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putFloat(floatValue)
                        .array()
                }
                "double" -> {
                    val doubleValue = value.toDouble()
                    ByteBuffer.allocate(8)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putDouble(doubleValue)
                        .array()
                }
                else -> throw IllegalArgumentException("Unsupported data type: $datatype")
            }

            writeMemory(pid, address, byteArray, key)
            return@withContext
        }
    }.onFailure {
        Log.e(TAG, "Error writing", it)
    }

    suspend fun search(
        pid: Int,
        value: String,
        dataType: String,
        context: Context,
        regions: List<MemoryRegions>,
    ): Result<List<MatchInfo>> = runCatching {
        withContext(Dispatchers.IO) {
            var key = defaultKey
            val skey = getSharedKey(context, "user_key")
            if (!skey.isNullOrEmpty()) {
                key = skey
            }
            val matchs: MutableList<MatchInfo> = mutableListOf()
            val res = searchMemory(pid, regions, dataType, value, key)
            res.forEach { match ->
                matchs.add(match.copy(id = UUID.randomUUID().toString()))
            }
            matchs
        }
    }.onFailure {
        Log.e(TAG, "Error searching", it)
    }

    suspend fun searchGroup(
        pid: Int,
        value: String,
        dataType: String,
        context: Context,
        regions: List<MemoryRegions>,
    ): Result<List<MatchInfo>> = runCatching {
        withContext(Dispatchers.IO) {
            var key = defaultKey
            val skey = getSharedKey(context, "user_key")
            if (!skey.isNullOrEmpty()) {
                key = skey
            }
            val matchs: MutableList<MatchInfo> = mutableListOf()
            val res = searchMemoryGroup(pid, regions, dataType, value, key)
            res.forEach { match ->
                matchs.add(match.copy(id = UUID.randomUUID().toString()))
            }
            matchs
        }
    }.onFailure {
        Log.e(TAG, "Error searching group", it)
    }

    suspend fun searchRange(
        pid: Int,
        valueMin: String,
        valueMax: String,
        dataType: String,
        context: Context,
        regions: List<MemoryRegions>,
    ): Result<List<MatchInfo>> = runCatching {
        withContext(Dispatchers.IO) {
            var key = defaultKey
            val skey = getSharedKey(context, "user_key")
            if (!skey.isNullOrEmpty()) {
                key = skey
            }
            val matchs: MutableList<MatchInfo> = mutableListOf()
            val res = searchMemoryRange(pid, regions, dataType, valueMin, valueMax, key)
            res.forEach { match ->
                matchs.add(match.copy(id = UUID.randomUUID().toString()))
            }
            matchs
        }
    }.onFailure {
        Log.e(TAG, "Error searching range", it)
    }

    private external fun readMemory(pid: Int, addr: Long, size: Long, key: String): ByteArray
    private external fun writeMemory(pid: Int, addr: Long, data: ByteArray, key: String): Long

    private external fun searchMemory(
        pid: Int,
        regions: List<MemoryRegions>,
        dataType: String,
        value: String,
        key: String
    ): List<MatchInfo>

    private external fun searchMemoryGroup(
        pid: Int,
        regions: List<MemoryRegions>,
        dataType: String,
        value: String,
        key: String
    ): List<MatchInfo>

    private external fun searchMemoryRange(pid: Int, memoryRegions: List<MemoryRegions>,
                                   valueType: String, minValue: String,
                                   maxValue: String, key: String): List<MatchInfo>

    companion object {
        private const val TAG = "huntmem"

        init {
            System.loadLibrary("huntmem")
        }
    }
}