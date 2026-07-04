package com.jexpop.appkotlininggas.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DriveManager {

    private const val APP_FOLDER = "ecogar"
    private const val BACKUPS_FOLDER = "backups"
    private const val CSV_FOLDER = "csv"
    private const val SQL_FOLDER = "sql"

    private fun getDriveService(context: Context): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        ).apply { selectedAccount = account.account }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Ecogar").build()
    }

    private suspend fun getOrCreateFolder(
        drive: Drive,
        folderName: String,
        parentId: String? = null
    ): String = withContext(Dispatchers.IO) {
        val query = buildString {
            append("mimeType='application/vnd.google-apps.folder'")
            append(" and name='$folderName'")
            append(" and trashed=false")
            if (parentId != null) append(" and '$parentId' in parents")
        }

        val result = drive.files().list()
            .setQ(query)
            .setFields("files(id, name)")
            .execute()

        if (result.files.isNotEmpty()) {
            result.files.first().id
        } else {
            val metadata = File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
                if (parentId != null) parents = listOf(parentId)
            }
            drive.files().create(metadata)
                .setFields("id")
                .execute().id
        }
    }

    suspend fun uploadCsvBackup(
        context: Context,
        fileName: String,
        data: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = getDriveService(context)
                ?: throw Exception("No hay sesión de Google activa")

            val appFolderId = getOrCreateFolder(drive, APP_FOLDER)
            val backupsFolderId = getOrCreateFolder(drive, BACKUPS_FOLDER, appFolderId)
            val csvFolderId = getOrCreateFolder(drive, CSV_FOLDER, backupsFolderId)

            val existingFile = drive.files().list()
                .setQ("name='$fileName' and '$csvFolderId' in parents and trashed=false")
                .setFields("files(id)")
                .execute().files.firstOrNull()

            if (existingFile != null) {
                drive.files().update(
                    existingFile.id,
                    null,
                    ByteArrayContent("application/octet-stream", data)
                ).execute()
            } else {
                val metadata = File().apply {
                    name = fileName
                    parents = listOf(csvFolderId)
                }
                drive.files().create(
                    metadata,
                    ByteArrayContent("application/octet-stream", data)
                ).setFields("id").execute()
            }
            Unit
        }
    }

    suspend fun uploadSqlBackup(
        context: Context,
        fileName: String,
        data: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = getDriveService(context)
                ?: throw Exception("No hay sesión de Google activa")

            val appFolderId = getOrCreateFolder(drive, APP_FOLDER)
            val backupsFolderId = getOrCreateFolder(drive, BACKUPS_FOLDER, appFolderId)
            val sqlFolderId = getOrCreateFolder(drive, SQL_FOLDER, backupsFolderId)

            val existingFile = drive.files().list()
                .setQ("name='$fileName' and '$sqlFolderId' in parents and trashed=false")
                .setFields("files(id)")
                .execute().files.firstOrNull()

            if (existingFile != null) {
                drive.files().update(
                    existingFile.id,
                    null,
                    ByteArrayContent("application/octet-stream", data)
                ).execute()
            } else {
                val metadata = File().apply {
                    name = fileName
                    parents = listOf(sqlFolderId)
                }
                drive.files().create(
                    metadata,
                    ByteArrayContent("application/octet-stream", data)
                ).setFields("id").execute()
            }
            Unit
        }
    }
}