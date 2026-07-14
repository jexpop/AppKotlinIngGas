package com.jexpop.appkotlininggas.data

import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StorageManager {

    private const val BUCKET = "backups"
    private const val CSV_PATH = "csv"
    private const val SQL_PATH = "sql"

    data class RemoteBackupFile(
        val name: String
    )

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

    suspend fun listSqlBackups(): Result<List<RemoteBackupFile>> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.storage.from(BUCKET).list(SQL_PATH)
                .mapNotNull { file ->
                    val name = file.name
                    if (name.isBlank() || name == ".emptyFolderPlaceholder") null
                    else RemoteBackupFile(name = name)
                }
        }
    }

    suspend fun downloadSqlBackup(fileName: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.storage.from(BUCKET).downloadAuthenticated("$SQL_PATH/$fileName")
        }
    }
}
