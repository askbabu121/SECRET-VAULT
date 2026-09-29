package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.data.dao.VaultDao
import com.example.data.model.AlbumEntity
import com.example.data.model.BreakInLogEntity
import com.example.data.model.MediaItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class VaultStorageStats(
    val photoCount: Int,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val availableSpaceBytes: Long
)

class VaultRepository(
    private val context: Context,
    private val vaultDao: VaultDao
) {
    private val vaultDir: File by lazy {
        val dir = File(context.filesDir, "vault_storage")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    // Media Flows
    fun getAllActiveMedia(isDecoy: Boolean): Flow<List<MediaItemEntity>> =
        vaultDao.getAllActiveMedia(isDecoy)

    fun getMediaByType(type: String, isDecoy: Boolean): Flow<List<MediaItemEntity>> =
        vaultDao.getMediaByType(type, isDecoy)

    fun getFavorites(isDecoy: Boolean): Flow<List<MediaItemEntity>> =
        vaultDao.getFavorites(isDecoy)

    fun getTrashMedia(isDecoy: Boolean): Flow<List<MediaItemEntity>> =
        vaultDao.getTrashMedia(isDecoy)

    fun getMediaByAlbum(albumId: Long, isDecoy: Boolean): Flow<List<MediaItemEntity>> =
        vaultDao.getMediaByAlbum(albumId, isDecoy)

    fun getMediaById(id: Long): Flow<MediaItemEntity?> =
        vaultDao.getMediaById(id)

    suspend fun getMediaByIdDirect(id: Long): MediaItemEntity? =
        vaultDao.getMediaByIdDirect(id)

    // Albums
    fun getAllAlbums(): Flow<List<AlbumEntity>> = vaultDao.getAllAlbums()

    suspend fun createAlbum(name: String, iconName: String = "folder"): Long {
        return vaultDao.insertAlbum(AlbumEntity(name = name, iconName = iconName))
    }

    suspend fun deleteAlbum(albumId: Long) {
        vaultDao.clearAlbumFromMedia(albumId)
        vaultDao.deleteAlbum(albumId)
    }

    suspend fun updateAlbum(album: AlbumEntity) {
        vaultDao.updateAlbum(album)
    }

    fun getMediaCountForAlbum(albumId: Long, isDecoy: Boolean): Flow<Int> =
        vaultDao.getMediaCountForAlbum(albumId, isDecoy)

    // Break In Logs
    fun getBreakInLogs(): Flow<List<BreakInLogEntity>> = vaultDao.getBreakInLogs()

    suspend fun recordFailedAttempt(enteredMasked: String) {
        vaultDao.insertBreakInLog(
            BreakInLogEntity(
                timestamp = System.currentTimeMillis(),
                attemptType = "WRONG_PIN",
                enteredCodeMasked = enteredMasked
            )
        )
    }

    suspend fun clearBreakInLogs() {
        vaultDao.clearBreakInLogs()
    }

    // Media Operations
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        vaultDao.toggleFavorite(id, isFavorite)
    }

    suspend fun moveToTrash(ids: List<Long>) {
        vaultDao.moveToTrash(ids, System.currentTimeMillis())
    }

    suspend fun restoreFromTrash(ids: List<Long>) {
        vaultDao.restoreFromTrash(ids)
    }

    suspend fun setAlbumForMedia(ids: List<Long>, albumId: Long?) {
        vaultDao.setMediaAlbum(ids, albumId)
    }

    suspend fun updateNotes(id: Long, notes: String) {
        vaultDao.updateNotes(id, notes)
    }

    suspend fun deletePermanently(ids: List<Long>) = withContext(Dispatchers.IO) {
        val items = vaultDao.getMediaByIdsDirect(ids)
        for (item in items) {
            val file = getInternalFile(item.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        vaultDao.deleteMediaByIds(ids)
    }

    fun getInternalFile(filePath: String): File {
        return File(context.filesDir, filePath)
    }

    suspend fun importMediaFromUris(
        uris: List<Uri>,
        albumId: Long? = null,
        isDecoy: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val contentResolver = context.contentResolver
            for (uri in uris) {
                val mime = contentResolver.getType(uri) ?: "image/jpeg"
                val isVideo = mime.startsWith("video/")
                val mediaType = if (isVideo) "VIDEO" else "PHOTO"

                // Determine extension and display name
                val (originalName, ext) = getFileNameAndExt(uri, isVideo, mime)
                val safeFileName = "vault_${if (isVideo) "vid" else "img"}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$ext"
                val relativePath = "vault_storage/$safeFileName"
                val destFile = File(vaultDir, safeFileName)

                var fileSizeBytes = 0L
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        val buffer = ByteArray(16 * 1024)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            fileSizeBytes += bytesRead
                        }
                        outputStream.flush()
                    }
                }

                var durationMs = 0L
                if (isVideo) {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(destFile.absolutePath)
                        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        durationMs = durationStr?.toLongOrNull() ?: 0L
                        retriever.release()
                    } catch (e: Exception) {
                        // ignore duration extraction failure
                    }
                }

                val entity = MediaItemEntity(
                    fileName = safeFileName,
                    originalName = originalName,
                    mediaType = mediaType,
                    mimeType = mime,
                    filePath = relativePath,
                    fileSizeBytes = fileSizeBytes,
                    dateAdded = System.currentTimeMillis(),
                    albumId = albumId,
                    durationMs = durationMs,
                    isDecoy = isDecoy
                )

                vaultDao.insertMedia(entity)
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromCameraFile(
        photoFile: File,
        albumId: Long? = null,
        isDecoy: Boolean = false
    ): Result<MediaItemEntity> = withContext(Dispatchers.IO) {
        try {
            val safeFileName = "vault_camera_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val relativePath = "vault_storage/$safeFileName"
            val destFile = File(vaultDir, safeFileName)

            photoFile.copyTo(destFile, overwrite = true)
            val size = destFile.length()
            if (photoFile.exists()) {
                photoFile.delete()
            }

            val entity = MediaItemEntity(
                fileName = safeFileName,
                originalName = "Camera_${System.currentTimeMillis()}.jpg",
                mediaType = "PHOTO",
                mimeType = "image/jpeg",
                filePath = relativePath,
                fileSizeBytes = size,
                dateAdded = System.currentTimeMillis(),
                albumId = albumId,
                isDecoy = isDecoy
            )
            val id = vaultDao.insertMedia(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportMediaToPublic(item: MediaItemEntity): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = getInternalFile(item.filePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("File not found in vault"))
            }

            val resolver = context.contentResolver
            val isVideo = item.mediaType == "VIDEO"
            val collectionUri = if (isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "Unhidden_${item.originalName}")
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativeDir = if (isVideo) Environment.DIRECTORY_MOVIES + "/VaultExport" else Environment.DIRECTORY_PICTURES + "/VaultExport"
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val insertedUri = resolver.insert(collectionUri, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create media store entry"))

            resolver.openOutputStream(insertedUri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(insertedUri, contentValues, null, null)
            }

            Result.success(insertedUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun populateDecoyDummyMedia(): Int = withContext(Dispatchers.IO) {
        val dummyList = listOf(
            DummyTemplate(
                title = "Family Trip - Grand Canyon Sunset",
                category = "Travel",
                notes = "Amazing sunset view with everyone at the South Rim.",
                topColor = 0xFFE65100.toInt(),
                bottomColor = 0xFFF57C00.toInt(),
                accentColor = 0xFFFFD54F.toInt(),
                iconSymbol = "🌄"
            ),
            DummyTemplate(
                title = "Baking Recipe - Homemade Choco Cake",
                category = "Recipes",
                notes = "2 cups flour, 1 cup cocoa, 1.5 cup sugar, 2 eggs. Bake at 350F for 30 mins.",
                topColor = 0xFF4E342E.toInt(),
                bottomColor = 0xFF6D4C41.toInt(),
                accentColor = 0xFFFFCC80.toInt(),
                iconSymbol = "🍰"
            ),
            DummyTemplate(
                title = "Max at the Park - Afternoon Walk",
                category = "Pets",
                notes = "Playing fetch at the community dog park. He loved chasing the frisbee!",
                topColor = 0xFF2E7D32.toInt(),
                bottomColor = 0xFF43A047.toInt(),
                accentColor = 0xFFA5D6A7.toInt(),
                iconSymbol = "🐕"
            ),
            DummyTemplate(
                title = "Morning Coffee & Journaling",
                category = "Lifestyle",
                notes = "Quiet Sunday morning reading and setting goals for the upcoming week.",
                topColor = 0xFF37474F.toInt(),
                bottomColor = 0xFF546E7A.toInt(),
                accentColor = 0xFF80CBC4.toInt(),
                iconSymbol = "☕"
            ),
            DummyTemplate(
                title = "Workout Routine & Daily Checklist",
                category = "Health",
                notes = "30 min cardio, 20 min core, stretch. Drank 2.5L water.",
                topColor = 0xFF1565C0.toInt(),
                bottomColor = 0xFF1976D2.toInt(),
                accentColor = 0xFF90CAF9.toInt(),
                iconSymbol = "🏃"
            ),
            DummyTemplate(
                title = "Downtown Skyline & City Walk",
                category = "Photography",
                notes = "Walking past the bridges during blue hour.",
                topColor = 0xFF283593.toInt(),
                bottomColor = 0xFF3949AB.toInt(),
                accentColor = 0xFFC5CAE9.toInt(),
                iconSymbol = "🏙️"
            )
        )

        var created = 0
        for (template in dummyList) {
            val safeFileName = "vault_decoy_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val destFile = File(vaultDir, safeFileName)
            val relativePath = "vault_storage/$safeFileName"

            // Generate crisp believable dummy image
            createDummyImageBitmap(template, destFile)

            val entity = MediaItemEntity(
                fileName = safeFileName,
                originalName = "${template.title}.jpg",
                mediaType = "PHOTO",
                mimeType = "image/jpeg",
                filePath = relativePath,
                fileSizeBytes = destFile.length(),
                dateAdded = System.currentTimeMillis() - (created * 86400000L), // stagger dates across days
                albumId = null,
                isFavorite = created == 0 || created == 2,
                notes = template.notes,
                isDecoy = true
            )
            vaultDao.insertMedia(entity)
            created++
        }
        created
    }

    suspend fun clearDecoyMedia(): Unit = withContext(Dispatchers.IO) {
        val decoyItems = vaultDao.getAllDecoyMediaDirect()
        for (item in decoyItems) {
            val file = getInternalFile(item.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        vaultDao.deleteAllDecoyMedia()
    }

    fun getDecoyMediaCount(): Flow<Int> = vaultDao.getDecoyMediaCount()

    private fun createDummyImageBitmap(template: DummyTemplate, outputFile: File) {
        val width = 800
        val height = 800
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Gradient Background
        val shader = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            template.topColor, template.bottomColor,
            android.graphics.Shader.TileMode.CLAMP
        )
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Decorative subtle grid circles & shapes
        val accentPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = template.accentColor
            alpha = 40
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(width * 0.8f, height * 0.2f, 180f, accentPaint)
        canvas.drawCircle(width * 0.2f, height * 0.85f, 220f, accentPaint)

        // Card Frame
        val cardPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            alpha = 30
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRoundRect(
            width * 0.1f, height * 0.15f,
            width * 0.9f, height * 0.85f,
            40f, 40f, cardPaint
        )

        // Border
        val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            alpha = 70
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(
            width * 0.1f, height * 0.15f,
            width * 0.9f, height * 0.85f,
            40f, 40f, borderPaint
        )

        // Emoji Symbol
        val emojiPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 90f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText(template.iconSymbol, width / 2f, height * 0.38f, emojiPaint)

        // Title text
        val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 34f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText(template.title, width / 2f, height * 0.50f, titlePaint)

        // Category Pill
        val tagPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = template.accentColor
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("• ${template.category} •", width / 2f, height * 0.56f, tagPaint)

        // Description / Notes text preview
        val notesPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xEEFFFFFF.toInt()
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val preview = if (template.notes.length > 45) template.notes.take(45) + "..." else template.notes
        canvas.drawText(preview, width / 2f, height * 0.68f, notesPaint)

        // Watermark timestamp
        val timePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xAAFFFFFF.toInt()
            textSize = 18f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("Shot on Mobile Camera", width / 2f, height * 0.78f, timePaint)

        FileOutputStream(outputFile).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
        }
        bitmap.recycle()
    }

    suspend fun getStorageStats(isDecoy: Boolean): VaultStorageStats = withContext(Dispatchers.IO) {
        val photos = vaultDao.getMediaByType("PHOTO", isDecoy)
        var totalBytes = 0L
        val allFiles = vaultDir.listFiles() ?: emptyArray()
        for (f in allFiles) {
            totalBytes += f.length()
        }
        val freeSpace = context.filesDir.freeSpace
        VaultStorageStats(
            photoCount = 0, // calculated from flow in ViewModel
            videoCount = 0,
            totalSizeBytes = totalBytes,
            availableSpaceBytes = freeSpace
        )
    }

    private fun getFileNameAndExt(uri: Uri, isVideo: Boolean, mime: String): Pair<String, String> {
        var name = "media_${System.currentTimeMillis()}"
        var ext = if (isVideo) "mp4" else "jpg"

        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    val foundName = it.getString(index)
                    if (!foundName.isNullOrBlank()) {
                        name = foundName
                        val dotIdx = name.lastIndexOf('.')
                        if (dotIdx != -1 && dotIdx < name.length - 1) {
                            ext = name.substring(dotIdx + 1).lowercase()
                        }
                    }
                }
            }
        }

        if (ext.isEmpty()) {
            val extensionFromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            if (!extensionFromMime.isNullOrEmpty()) {
                ext = extensionFromMime
            }
        }
        return Pair(name, ext)
    }
}

data class DummyTemplate(
    val title: String,
    val category: String,
    val notes: String,
    val topColor: Int,
    val bottomColor: Int,
    val accentColor: Int,
    val iconSymbol: String
)
