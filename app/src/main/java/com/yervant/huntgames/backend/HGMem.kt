package com.yervant.huntgames.backend

import android.content.Context
import android.util.Log
import com.yervant.huntgames.ui.getSharedKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

class HGMem {
    private val defaultKey = "12345678"

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

    private external fun readMemory(pid: Int, addr: Long, size: Long, key: String): ByteArray
    private external fun writeMemory(pid: Int, addr: Long, data: ByteArray, key: String): Long

    companion object {
        private const val TAG = "HGMem"

        init {
            System.loadLibrary("huntgames")
        }
    }
}