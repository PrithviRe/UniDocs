package com.example.unidocs.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object FileUtils {
    
    suspend fun readBytesFromUri(context: Context, uri: Uri): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            // Take persistable permission for the URI
            takePersistablePermissions(context, uri)
            
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                Result.success(bytes)
            } else {
                Result.failure(IOException("Failed to read from URI: $uri"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeBytesToUri(context: Context, uri: Uri, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Take persistable permission for the URI
            takePersistablePermissions(context, uri)
            
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(data) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun takePersistablePermissions(context: Context, uri: Uri): Boolean {
        return try {
            val flags = IntentFlags.PERSISTABLE_FLAGS
            context.contentResolver.takePersistableUriPermission(uri, flags)
            true
        } catch (e: Exception) {
            // If we can't take persistable permission, try to get temporary access
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use {
                    // Just test if we can access the URI
                    true
                } ?: false
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) it.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getFileSize(context: Context, uri: Uri): Long? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex >= 0) it.getLong(sizeIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}

object IntentFlags {
    const val PERSISTABLE_FLAGS = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or 
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
}
