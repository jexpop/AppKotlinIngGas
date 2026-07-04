package com.jexpop.appkotlininggas.data

import android.content.Context
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StorageManager {

    private const val BUCKET = "backups"
    private const val CSV_PATH = "csv"
    private const val SQL_PATH = "sql"

    suspend fun uploadCsvBackup(
        fileName: String,
        data: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "$CSV_PATH/$fileName"
            supabase.storage.from(BUCKET).upload(path, data) {
                upsert = true
            }
            Unit
        }
    }

    suspend fun uploadSqlBackup(
        fileName: String,
        data: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "$SQL_PATH/$fileName"
            supabase.storage.from(BUCKET).upload(path, data) {
                upsert = true
            }
            Unit
        }
    }
}