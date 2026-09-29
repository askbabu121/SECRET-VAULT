package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val originalName: String,
    val mediaType: String, // "PHOTO" or "VIDEO"
    val mimeType: String,
    val filePath: String, // relative to filesDir e.g. "vault_media/abc.jpg"
    val fileSizeBytes: Long,
    val dateAdded: Long = System.currentTimeMillis(),
    val albumId: Long? = null,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long? = null,
    val notes: String = "",
    val durationMs: Long = 0L,
    val isDecoy: Boolean = false
)
