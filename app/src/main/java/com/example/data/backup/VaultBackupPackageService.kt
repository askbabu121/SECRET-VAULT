package com.example.data.backup

import android.content.Context
import com.example.data.dao.VaultDao
import com.example.data.model.AlbumEntity
import com.example.data.model.MediaItemEntity
import com.example.data.security.VaultBackupCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupManifest(
    val version: Int,
    val createdAt: Long,
    val mediaCount: Int,
    val albumCount: Int
)

data class RestoreResult(
    val restoredMediaCount: Int,
    val restoredAlbumsCount: Int,
    val manifest: BackupManifest
)

class VaultBackupPackageService(
    private val context: Context,
    private val vaultDao: VaultDao
) {

    suspend fun createEncryptedBackupPackage(passphrase: String): File = withContext(Dispatchers.IO) {
        val albums = vaultDao.getAllAlbumsDirect()
        val mediaItems = vaultDao.getAllMasterMediaDirect().filter { !it.isDeleted }

        val zipOutStream = ByteArrayOutputStream()
        ZipOutputStream(zipOutStream).use { zip ->
            // 1. Write manifest
            val manifestJson = JSONObject().apply {
                put("version", 1)
                put("createdAt", System.currentTimeMillis())
                put("mediaCount", mediaItems.size)
                put("albumCount", albums.size)
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifestJson.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 2. Write albums
            val albumsArray = JSONArray()
            albums.forEach { album ->
                albumsArray.put(JSONObject().apply {
                    put("id", album.id)
                    put("name", album.name)
                    put("iconName", album.iconName)
                    put("createdAt", album.createdAt)
                })
            }
            zip.putNextEntry(ZipEntry("albums.json"))
            zip.write(albumsArray.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 3. Write media metadata
            val mediaArray = JSONArray()
            mediaItems.forEach { item ->
                mediaArray.put(JSONObject().apply {
                    put("id", item.id)
                    put("fileName", item.fileName)
                    put("originalName", item.originalName)
                    put("mediaType", item.mediaType)
                    put("mimeType", item.mimeType)
                    put("filePath", item.filePath)
                    put("fileSizeBytes", item.fileSizeBytes)
                    put("dateAdded", item.dateAdded)
                    put("albumId", item.albumId ?: JSONObject.NULL)
                    put("isFavorite", item.isFavorite)
                    put("notes", item.notes)
                    put("durationMs", item.durationMs)
                })
            }
            zip.putNextEntry(ZipEntry("media.json"))
            zip.write(mediaArray.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 4. Pack actual files
            val baseDir = context.filesDir
            mediaItems.forEach { item ->
                val sourceFile = File(baseDir, item.filePath)
                if (sourceFile.exists()) {
                    zip.putNextEntry(ZipEntry("files/" + item.filePath))
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(zip)
                    }
                    zip.closeEntry()
                }
            }
        }

        val plainZipBytes = zipOutStream.toByteArray()
        val encryptedBytes = VaultBackupCrypto.encrypt(plainZipBytes, passphrase)

        val backupDir = File(context.cacheDir, "vault_backups").apply { mkdirs() }
        val backupFile = File(backupDir, "vault_backup_${System.currentTimeMillis()}.vaultenc")
        FileOutputStream(backupFile).use { it.write(encryptedBytes) }
        backupFile
    }

    suspend fun restoreEncryptedBackupPackage(
        backupBytes: ByteArray,
        passphrase: String
    ): RestoreResult = withContext(Dispatchers.IO) {
        val plainZipBytes = VaultBackupCrypto.decrypt(backupBytes, passphrase)

        var manifestJson: JSONObject? = null
        var albumsJson: JSONArray? = null
        var mediaJson: JSONArray? = null
        val extractedFiles = mutableMapOf<String, ByteArray>()

        ZipInputStream(ByteArrayInputStream(plainZipBytes)).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val entryBytes = zip.readBytes()
                when (entry.name) {
                    "manifest.json" -> {
                        manifestJson = JSONObject(String(entryBytes, Charsets.UTF_8))
                    }
                    "albums.json" -> {
                        albumsJson = JSONArray(String(entryBytes, Charsets.UTF_8))
                    }
                    "media.json" -> {
                        mediaJson = JSONArray(String(entryBytes, Charsets.UTF_8))
                    }
                    else -> {
                        if (entry.name.startsWith("files/")) {
                            val relativePath = entry.name.removePrefix("files/")
                            extractedFiles[relativePath] = entryBytes
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        if (manifestJson == null || mediaJson == null) {
            throw IllegalArgumentException("Corrupt backup file: missing manifest or media records")
        }

        val manifest = BackupManifest(
            version = manifestJson!!.optInt("version", 1),
            createdAt = manifestJson!!.optLong("createdAt", 0L),
            mediaCount = manifestJson!!.optInt("mediaCount", 0),
            albumCount = manifestJson!!.optInt("albumCount", 0)
        )

        // Restore media files to context.filesDir
        val baseDir = context.filesDir
        extractedFiles.forEach { (relPath, bytes) ->
            val targetFile = File(baseDir, relPath)
            targetFile.parentFile?.mkdirs()
            FileOutputStream(targetFile).use { it.write(bytes) }
        }

        // Restore albums
        val restoredAlbums = mutableListOf<AlbumEntity>()
        if (albumsJson != null) {
            for (i in 0 until albumsJson!!.length()) {
                val obj = albumsJson!!.getJSONObject(i)
                restoredAlbums.add(
                    AlbumEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        iconName = obj.optString("iconName", "folder"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            if (restoredAlbums.isNotEmpty()) {
                vaultDao.insertAlbumsList(restoredAlbums)
            }
        }

        // Restore media entities
        val restoredMedia = mutableListOf<MediaItemEntity>()
        for (i in 0 until mediaJson!!.length()) {
            val obj = mediaJson!!.getJSONObject(i)
            val albumId = if (obj.isNull("albumId")) null else obj.optLong("albumId")
            restoredMedia.add(
                MediaItemEntity(
                    id = obj.optLong("id", 0L),
                    fileName = obj.getString("fileName"),
                    originalName = obj.optString("originalName", obj.getString("fileName")),
                    mediaType = obj.optString("mediaType", "PHOTO"),
                    mimeType = obj.optString("mimeType", "image/*"),
                    filePath = obj.getString("filePath"),
                    fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
                    dateAdded = obj.optLong("dateAdded", System.currentTimeMillis()),
                    albumId = albumId,
                    isFavorite = obj.optBoolean("isFavorite", false),
                    isDeleted = false,
                    deletedTimestamp = null,
                    notes = obj.optString("notes", ""),
                    durationMs = obj.optLong("durationMs", 0L),
                    isDecoy = false
                )
            )
        }
        if (restoredMedia.isNotEmpty()) {
            vaultDao.insertMediaList(restoredMedia)
        }

        RestoreResult(
            restoredMediaCount = restoredMedia.size,
            restoredAlbumsCount = restoredAlbums.size,
            manifest = manifest
        )
    }
}
