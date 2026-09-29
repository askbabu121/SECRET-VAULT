package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AlbumEntity
import com.example.data.model.BreakInLogEntity
import com.example.data.model.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM media_items WHERE isDeleted = 0 AND isDecoy = :isDecoy ORDER BY dateAdded DESC")
    fun getAllActiveMedia(isDecoy: Boolean): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isDeleted = 0 AND isDecoy = :isDecoy AND mediaType = :mediaType ORDER BY dateAdded DESC")
    fun getMediaByType(mediaType: String, isDecoy: Boolean): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isDeleted = 0 AND isDecoy = :isDecoy AND isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavorites(isDecoy: Boolean): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isDeleted = 1 AND isDecoy = :isDecoy ORDER BY deletedTimestamp DESC")
    fun getTrashMedia(isDecoy: Boolean): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isDeleted = 0 AND isDecoy = :isDecoy AND albumId = :albumId ORDER BY dateAdded DESC")
    fun getMediaByAlbum(albumId: Long, isDecoy: Boolean): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    fun getMediaById(id: Long): Flow<MediaItemEntity?>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaByIdDirect(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE id IN (:ids)")
    suspend fun getMediaByIdsDirect(ids: List<Long>): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE isDecoy = 0")
    suspend fun getAllMasterMediaDirect(): List<MediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(items: List<MediaItemEntity>): List<Long>

    @Update
    suspend fun updateMedia(item: MediaItemEntity)

    @Delete
    suspend fun deleteMedia(item: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteMediaByIds(ids: List<Long>)

    @Query("UPDATE media_items SET isDeleted = 1, deletedTimestamp = :timestamp WHERE id IN (:ids)")
    suspend fun moveToTrash(ids: List<Long>, timestamp: Long)

    @Query("UPDATE media_items SET isDeleted = 0, deletedTimestamp = NULL WHERE id IN (:ids)")
    suspend fun restoreFromTrash(ids: List<Long>)

    @Query("UPDATE media_items SET isFavorite = :isFav WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFav: Boolean)

    @Query("UPDATE media_items SET albumId = :albumId WHERE id IN (:ids)")
    suspend fun setMediaAlbum(ids: List<Long>, albumId: Long?)

    @Query("UPDATE media_items SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)

    // Albums
    @Query("SELECT * FROM albums ORDER BY createdAt ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums")
    suspend fun getAllAlbumsDirect(): List<AlbumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumsList(albums: List<AlbumEntity>): List<Long>

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE id = :albumId")
    suspend fun deleteAlbum(albumId: Long)

    @Query("UPDATE media_items SET albumId = NULL WHERE albumId = :albumId")
    suspend fun clearAlbumFromMedia(albumId: Long)

    @Query("SELECT COUNT(*) FROM media_items WHERE isDeleted = 0 AND isDecoy = :isDecoy AND albumId = :albumId")
    fun getMediaCountForAlbum(albumId: Long, isDecoy: Boolean): Flow<Int>

    // Stats
    @Query("SELECT SUM(fileSizeBytes) FROM media_items WHERE isDeleted = 0 AND isDecoy = :isDecoy")
    fun getTotalSizeBytes(isDecoy: Boolean): Flow<Long?>

    @Query("SELECT COUNT(*) FROM media_items WHERE isDeleted = 0 AND isDecoy = :isDecoy AND mediaType = 'PHOTO'")
    fun getPhotoCount(isDecoy: Boolean): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE isDeleted = 0 AND isDecoy = :isDecoy AND mediaType = 'VIDEO'")
    fun getVideoCount(isDecoy: Boolean): Flow<Int>

    @Query("SELECT * FROM media_items WHERE isDecoy = 1")
    suspend fun getAllDecoyMediaDirect(): List<MediaItemEntity>

    @Query("DELETE FROM media_items WHERE isDecoy = 1")
    suspend fun deleteAllDecoyMedia()

    @Query("SELECT COUNT(*) FROM media_items WHERE isDecoy = 1 AND isDeleted = 0")
    fun getDecoyMediaCount(): Flow<Int>

    // Break-in logs
    @Query("SELECT * FROM break_in_logs ORDER BY timestamp DESC LIMIT 50")
    fun getBreakInLogs(): Flow<List<BreakInLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakInLog(log: BreakInLogEntity)

    @Query("DELETE FROM break_in_logs")
    suspend fun clearBreakInLogs()
}
