package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VaultApplication
import com.example.data.backup.GoogleDriveBackupFile
import com.example.data.backup.GoogleDriveBackupService
import com.example.data.backup.VaultBackupPackageService
import com.example.data.model.AlbumEntity
import com.example.data.model.BreakInLogEntity
import com.example.data.model.MediaItemEntity
import com.example.data.repository.VaultRepository
import com.example.data.repository.VaultStorageStats
import com.example.data.security.AppIconDisguise
import com.example.data.security.AppIconDisguiseManager
import com.example.data.security.PinVerificationResult
import com.example.data.security.VaultSecurityManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VaultBackupUiState(
    val isBackupInProgress: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val isFetchingDriveBackups: Boolean = false,
    val signedInGoogleEmail: String? = null,
    val driveBackups: List<GoogleDriveBackupFile> = emptyList(),
    val lastBackupTimestamp: Long = 0L,
    val statusMessage: String? = null
)

enum class VaultTab {
    ALL,
    PHOTOS,
    VIDEOS,
    ALBUMS,
    FAVORITES,
    TRASH,
    SETTINGS
}

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val isDecoyMode: Boolean = false,
    val isPinConfigured: Boolean = false,
    val isStealthCalculatorMode: Boolean = true,
    val isBiometricEnabled: Boolean = false,
    val currentIconDisguise: AppIconDisguise = AppIconDisguise.VAULT,
    val inactivityTimeoutSeconds: Int = 60,
    val activeTab: VaultTab = VaultTab.ALL,
    val selectedAlbum: AlbumEntity? = null,
    val selectedMediaItem: MediaItemEntity? = null,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VaultApplication
    private val repository: VaultRepository = app.repository
    private val securityManager: VaultSecurityManager = app.securityManager
    private val backupPackageService: VaultBackupPackageService = app.backupPackageService
    private val googleDriveService: GoogleDriveBackupService = app.googleDriveBackupService

    private val _backupUiState = MutableStateFlow(
        VaultBackupUiState(
            lastBackupTimestamp = securityManager.getLastBackupTime(),
            signedInGoogleEmail = googleDriveService.getSignedInAccount()?.email
        )
    )
    val backupUiState: StateFlow<VaultBackupUiState> = _backupUiState.asStateFlow()

    private val _uiState = MutableStateFlow(
        VaultUiState(
            isPinConfigured = securityManager.isPinConfigured(),
            isStealthCalculatorMode = securityManager.isStealthCalculatorMode(),
            isBiometricEnabled = securityManager.isBiometricEnabled(),
            currentIconDisguise = securityManager.getAppIconDisguise(),
            inactivityTimeoutSeconds = securityManager.getInactivityTimeoutSeconds()
        )
    )
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private val _userEvents = MutableSharedFlow<String>()
    val userEvents = _userEvents.asSharedFlow()

    fun postUserMessage(message: String) {
        viewModelScope.launch {
            _userEvents.emit(message)
        }
    }

    private var inactivityTimerJob: Job? = null

    // Query trigger state for reactive media loading
    private val isDecoyFlow = _uiState.map { it.isDecoyMode }
    private val activeTabFlow = _uiState.map { it.activeTab }
    private val selectedAlbumFlow = _uiState.map { it.selectedAlbum }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMediaList: StateFlow<List<MediaItemEntity>> = combine(
        isDecoyFlow,
        activeTabFlow,
        selectedAlbumFlow,
        _uiState.map { it.searchQuery }
    ) { isDecoy, tab, album, query ->
        Quadruple(isDecoy, tab, album, query)
    }.flatMapLatest { (isDecoy, tab, album, query) ->
        val baseFlow = when {
            album != null -> repository.getMediaByAlbum(album.id, isDecoy)
            tab == VaultTab.PHOTOS -> repository.getMediaByType("PHOTO", isDecoy)
            tab == VaultTab.VIDEOS -> repository.getMediaByType("VIDEO", isDecoy)
            tab == VaultTab.FAVORITES -> repository.getFavorites(isDecoy)
            tab == VaultTab.TRASH -> repository.getTrashMedia(isDecoy)
            else -> repository.getAllActiveMedia(isDecoy)
        }
        baseFlow.map { list ->
            if (query.isBlank()) list
            else list.filter {
                it.originalName.contains(query, ignoreCase = true) ||
                        it.notes.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val albumsList: StateFlow<List<AlbumEntity>> = repository.getAllAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val trashList: StateFlow<List<MediaItemEntity>> = isDecoyFlow.flatMapLatest { isDecoy ->
        repository.getTrashMedia(isDecoy)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val breakInLogs: StateFlow<List<BreakInLogEntity>> = repository.getBreakInLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalMediaCount: StateFlow<Int> = activeMediaList.map { it.size }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val storageStats: StateFlow<VaultStorageStats> = isDecoyFlow.map { isDecoy ->
        repository.getStorageStats(isDecoy)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaultStorageStats(0, 0, 0L, 0L)
    )

    val decoyMediaCount: StateFlow<Int> = repository.getDecoyMediaCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Auth & Security methods
    fun verifyPin(pin: String): Boolean {
        val result = securityManager.verifyPin(pin)
        return when (result) {
            PinVerificationResult.MASTER -> {
                _uiState.value = _uiState.value.copy(
                    isUnlocked = true,
                    isDecoyMode = false,
                    userMessage = null
                )
                resetInactivityTimer()
                true
            }
            PinVerificationResult.DECOY -> {
                _uiState.value = _uiState.value.copy(
                    isUnlocked = true,
                    isDecoyMode = true,
                    userMessage = null
                )
                resetInactivityTimer()
                true
            }
            PinVerificationResult.INCORRECT -> {
                viewModelScope.launch {
                    val masked = if (pin.length > 2) pin.take(1) + "*".repeat(pin.length - 2) + pin.takeLast(1) else "***"
                    repository.recordFailedAttempt(masked)
                    _userEvents.emit("Incorrect PIN entered")
                }
                false
            }
        }
    }

    fun lockVault() {
        inactivityTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isUnlocked = false,
            selectedMediaItem = null,
            selectedAlbum = null,
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    fun onAppBackgrounded() {
        if (securityManager.isAutoLockEnabled()) {
            lockVault()
        }
    }

    fun setupInitialPin(
        pin: String,
        question: String,
        answer: String,
        stealthCalculator: Boolean
    ) {
        securityManager.setMasterPin(pin)
        securityManager.setSecurityQuestionAndAnswer(question, answer)
        securityManager.setStealthCalculatorMode(stealthCalculator)
        _uiState.value = _uiState.value.copy(
            isPinConfigured = true,
            isStealthCalculatorMode = stealthCalculator,
            isUnlocked = true,
            isDecoyMode = false
        )
        resetInactivityTimer()
        viewModelScope.launch {
            _userEvents.emit("Vault PIN configured successfully!")
        }
    }

    fun getSecurityQuestion(): String = securityManager.getSecurityQuestion()

    fun verifySecurityAnswerAndReset(answer: String, newPin: String): Boolean {
        if (securityManager.verifySecurityAnswer(answer)) {
            securityManager.setMasterPin(newPin)
            _uiState.value = _uiState.value.copy(
                isPinConfigured = true,
                isUnlocked = true
            )
            resetInactivityTimer()
            viewModelScope.launch {
                _userEvents.emit("PIN successfully reset!")
            }
            return true
        }
        return false
    }

    fun setStealthCalculatorMode(enabled: Boolean) {
        securityManager.setStealthCalculatorMode(enabled)
        _uiState.value = _uiState.value.copy(isStealthCalculatorMode = enabled)
    }

    fun setDecoyPin(pin: String?) {
        securityManager.setDecoyPin(pin)
        viewModelScope.launch {
            _userEvents.emit(if (pin.isNullOrEmpty()) "Decoy PIN removed" else "Decoy PIN updated")
        }
    }

    fun hasDecoyPin(): Boolean = securityManager.hasDecoyPin()

    fun populateDecoyDummyMedia() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val count = repository.populateDecoyDummyMedia()
            _uiState.value = _uiState.value.copy(isLoading = false)
            _userEvents.emit("Generated $count believable dummy media items in Decoy Vault")
        }
    }

    fun clearDecoyMedia() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.clearDecoyMedia()
            _uiState.value = _uiState.value.copy(isLoading = false)
            _userEvents.emit("Decoy Vault media cleared")
        }
    }

    fun unlockWithBiometric() {
        _uiState.value = _uiState.value.copy(
            isUnlocked = true,
            isDecoyMode = false,
            userMessage = null
        )
        resetInactivityTimer()
    }

    fun onUserActivity() {
        if (_uiState.value.isUnlocked) {
            resetInactivityTimer()
        }
    }

    fun resetInactivityTimer() {
        inactivityTimerJob?.cancel()
        val timeoutSecs = _uiState.value.inactivityTimeoutSeconds
        if (timeoutSecs <= 0 || !_uiState.value.isUnlocked) {
            return
        }
        inactivityTimerJob = viewModelScope.launch {
            delay(timeoutSecs * 1000L)
            if (_uiState.value.isUnlocked) {
                lockVault()
                _userEvents.emit("Vault auto-locked due to inactivity (${formatInactivityLabel(timeoutSecs)})")
            }
        }
    }

    fun setInactivityTimeoutSeconds(seconds: Int) {
        securityManager.setInactivityTimeoutSeconds(seconds)
        _uiState.value = _uiState.value.copy(inactivityTimeoutSeconds = seconds)
        resetInactivityTimer()
        viewModelScope.launch {
            _userEvents.emit("Auto-lock timeout set to ${formatInactivityLabel(seconds)}")
        }
    }

    private fun formatInactivityLabel(seconds: Int): String {
        return when (seconds) {
            15 -> "15 seconds"
            30 -> "30 seconds"
            60 -> "1 minute"
            120 -> "2 minutes"
            300 -> "5 minutes"
            0 -> "Never"
            else -> "$seconds seconds"
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
        _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
        viewModelScope.launch {
            _userEvents.emit(if (enabled) "Biometric unlock enabled" else "Biometric unlock disabled")
        }
    }

    fun setAppIconDisguise(disguise: AppIconDisguise) {
        AppIconDisguiseManager.setAppIconDisguise(app, disguise)
        securityManager.setAppIconDisguise(disguise)
        _uiState.value = _uiState.value.copy(currentIconDisguise = disguise)
        viewModelScope.launch {
            _userEvents.emit("Home screen icon & name changed to '${disguise.displayName}'")
        }
    }

    // Navigation & View Actions
    fun setActiveTab(tab: VaultTab) {
        _uiState.value = _uiState.value.copy(
            activeTab = tab,
            selectedAlbum = null,
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    fun openAlbum(album: AlbumEntity) {
        _uiState.value = _uiState.value.copy(
            selectedAlbum = album,
            activeTab = VaultTab.ALL,
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    fun closeAlbum() {
        _uiState.value = _uiState.value.copy(
            selectedAlbum = null,
            activeTab = VaultTab.ALBUMS
        )
    }

    fun openMediaViewer(item: MediaItemEntity) {
        _uiState.value = _uiState.value.copy(selectedMediaItem = item)
    }

    fun closeMediaViewer() {
        _uiState.value = _uiState.value.copy(selectedMediaItem = null)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    // Selection Mode
    fun toggleSelectionMode() {
        val current = _uiState.value.isSelectionMode
        _uiState.value = _uiState.value.copy(
            isSelectionMode = !current,
            selectedIds = emptySet()
        )
    }

    fun toggleItemSelection(id: Long) {
        val currentSet = _uiState.value.selectedIds.toMutableSet()
        if (currentSet.contains(id)) {
            currentSet.remove(id)
        } else {
            currentSet.add(id)
        }
        _uiState.value = _uiState.value.copy(
            isSelectionMode = currentSet.isNotEmpty(),
            selectedIds = currentSet
        )
    }

    fun selectAllMedia(items: List<MediaItemEntity>) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedIds = items.map { it.id }.toSet()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    // Media Actions
    fun importUris(uris: List<Uri>, albumId: Long? = _uiState.value.selectedAlbum?.id) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.importMediaFromUris(
                uris = uris,
                albumId = albumId,
                isDecoy = _uiState.value.isDecoyMode
            )
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess { count ->
                _userEvents.emit("Safely stored $count media item${if (count > 1) "s" else ""} in private vault")
            }.onFailure { e ->
                _userEvents.emit("Import failed: ${e.localizedMessage}")
            }
        }
    }

    fun importCameraPhoto(file: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.importFromCameraFile(
                photoFile = file,
                albumId = _uiState.value.selectedAlbum?.id,
                isDecoy = _uiState.value.isDecoyMode
            )
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess {
                _userEvents.emit("Private camera photo saved into vault")
            }.onFailure { e ->
                _userEvents.emit("Failed to save camera photo: ${e.localizedMessage}")
            }
        }
    }

    fun toggleFavorite(item: MediaItemEntity) {
        viewModelScope.launch {
            val newFav = !item.isFavorite
            repository.toggleFavorite(item.id, newFav)
            if (_uiState.value.selectedMediaItem?.id == item.id) {
                _uiState.value = _uiState.value.copy(
                    selectedMediaItem = item.copy(isFavorite = newFav)
                )
            }
        }
    }

    fun moveSelectedToTrash() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveToTrash(ids)
            clearSelection()
            _userEvents.emit("Moved ${ids.size} item${if (ids.size > 1) "s" else ""} to Trash")
        }
    }

    fun moveSingleToTrash(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.moveToTrash(listOf(item.id))
            if (_uiState.value.selectedMediaItem?.id == item.id) {
                closeMediaViewer()
            }
            _userEvents.emit("Moved to Trash")
        }
    }

    fun restoreSelectedFromTrash() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.restoreFromTrash(ids)
            clearSelection()
            _userEvents.emit("Restored ${ids.size} item${if (ids.size > 1) "s" else ""}")
        }
    }

    fun restoreSingleFromTrash(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.restoreFromTrash(listOf(item.id))
            _userEvents.emit("Restored '${item.originalName}'")
        }
    }

    fun deletePermanentlySelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deletePermanently(ids)
            clearSelection()
            _userEvents.emit("Permanently deleted ${ids.size} item${if (ids.size > 1) "s" else ""}")
        }
    }

    fun deleteSinglePermanently(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.deletePermanently(listOf(item.id))
            _userEvents.emit("Permanently deleted '${item.originalName}'")
        }
    }

    fun moveSelectedToAlbum(albumId: Long?) {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.setAlbumForMedia(ids, albumId)
            clearSelection()
            _userEvents.emit("Moved ${ids.size} item${if (ids.size > 1) "s" else ""} to album")
        }
    }

    fun exportSelectedToDevice() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            var successCount = 0
            for (id in ids) {
                val item = repository.getMediaByIdDirect(id)
                if (item != null) {
                    val res = repository.exportMediaToPublic(item)
                    if (res.isSuccess) successCount++
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
            clearSelection()
            _userEvents.emit("Successfully exported $successCount item${if (successCount > 1) "s" else ""} to device gallery")
        }
    }

    fun exportSingleItem(item: MediaItemEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.exportMediaToPublic(item)
            _uiState.value = _uiState.value.copy(isLoading = false)
            res.onSuccess {
                _userEvents.emit("Exported to gallery: Unhidden_${item.originalName}")
            }.onFailure { e ->
                _userEvents.emit("Export failed: ${e.localizedMessage}")
            }
        }
    }

    fun updateMediaNotes(id: Long, notes: String) {
        viewModelScope.launch {
            repository.updateNotes(id, notes)
            if (_uiState.value.selectedMediaItem?.id == id) {
                _uiState.value = _uiState.value.copy(
                    selectedMediaItem = _uiState.value.selectedMediaItem?.copy(notes = notes)
                )
            }
            _userEvents.emit("Notes saved")
        }
    }

    // Album Management
    fun createAlbum(name: String, iconName: String = "folder") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createAlbum(name.trim(), iconName)
            _userEvents.emit("Album '$name' created")
        }
    }

    fun deleteAlbum(album: AlbumEntity) {
        viewModelScope.launch {
            repository.deleteAlbum(album.id)
            if (_uiState.value.selectedAlbum?.id == album.id) {
                closeAlbum()
            }
            _userEvents.emit("Album '${album.name}' removed")
        }
    }

    fun clearBreakInHistory() {
        viewModelScope.launch {
            repository.clearBreakInLogs()
            _userEvents.emit("Break-in logs cleared")
        }
    }

    fun getInternalFile(filePath: String): File = repository.getInternalFile(filePath)

    // ==========================================
    // Cloud Backup & Restore (Google Drive)
    // ==========================================

    fun getGoogleSignInClient(): GoogleSignInClient = googleDriveService.getGoogleSignInClient()

    fun checkGoogleDriveAccountAndFetchBackups() {
        val account = googleDriveService.getSignedInAccount()
        _backupUiState.value = _backupUiState.value.copy(
            signedInGoogleEmail = account?.email
        )
        if (account != null) {
            fetchDriveBackups()
        }
    }

    fun handleGoogleSignInResult(account: GoogleSignInAccount) {
        _backupUiState.value = _backupUiState.value.copy(
            signedInGoogleEmail = account.email
        )
        fetchDriveBackups()
        viewModelScope.launch {
            _userEvents.emit("Connected Google Drive: ${account.email}")
        }
    }

    fun signOutGoogle() {
        googleDriveService.getGoogleSignInClient().signOut()
        _backupUiState.value = _backupUiState.value.copy(
            signedInGoogleEmail = null,
            driveBackups = emptyList()
        )
        viewModelScope.launch {
            _userEvents.emit("Disconnected Google Drive")
        }
    }

    fun fetchDriveBackups() {
        val account = googleDriveService.getSignedInAccount() ?: return
        viewModelScope.launch {
            _backupUiState.value = _backupUiState.value.copy(isFetchingDriveBackups = true)
            try {
                val token = googleDriveService.getAccessToken(account)
                val list = googleDriveService.listBackups(token)
                _backupUiState.value = _backupUiState.value.copy(
                    driveBackups = list,
                    isFetchingDriveBackups = false
                )
            } catch (e: Exception) {
                _backupUiState.value = _backupUiState.value.copy(
                    isFetchingDriveBackups = false,
                    statusMessage = "Could not sync Drive backups: ${e.message}"
                )
            }
        }
    }

    fun backupToGoogleDrive(passphrase: String) {
        val account = googleDriveService.getSignedInAccount()
        if (account == null) {
            viewModelScope.launch {
                _userEvents.emit("Please sign in to Google first")
            }
            return
        }

        viewModelScope.launch {
            _backupUiState.value = _backupUiState.value.copy(
                isBackupInProgress = true,
                statusMessage = "Encrypting vault contents with AES-256-GCM..."
            )
            try {
                val encryptedFile = backupPackageService.createEncryptedBackupPackage(passphrase)
                _backupUiState.value = _backupUiState.value.copy(
                    statusMessage = "Uploading encrypted backup to Google Drive..."
                )

                val token = googleDriveService.getAccessToken(account)
                val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                googleDriveService.uploadEncryptedBackup(
                    accessToken = token,
                    backupFile = encryptedFile,
                    fileName = "vault_backup_$timeStr.vaultenc",
                    description = "Encrypted Vault Backup"
                )

                val now = System.currentTimeMillis()
                securityManager.setLastBackupTime(now)
                encryptedFile.delete()

                val updatedList = googleDriveService.listBackups(token)
                _backupUiState.value = _backupUiState.value.copy(
                    isBackupInProgress = false,
                    lastBackupTimestamp = now,
                    driveBackups = updatedList,
                    statusMessage = null
                )
                _userEvents.emit("Vault securely encrypted & backed up to Google Drive!")
            } catch (e: Exception) {
                _backupUiState.value = _backupUiState.value.copy(
                    isBackupInProgress = false,
                    statusMessage = "Backup failed: ${e.message}"
                )
                _userEvents.emit("Backup failed: ${e.message}")
            }
        }
    }

    fun restoreFromGoogleDrive(driveFile: GoogleDriveBackupFile, passphrase: String) {
        val account = googleDriveService.getSignedInAccount()
        if (account == null) {
            viewModelScope.launch { _userEvents.emit("Google account not connected") }
            return
        }

        viewModelScope.launch {
            _backupUiState.value = _backupUiState.value.copy(
                isRestoreInProgress = true,
                statusMessage = "Downloading encrypted backup from Google Drive..."
            )
            try {
                val token = googleDriveService.getAccessToken(account)
                val tempDownload = File(app.cacheDir, "temp_restore_${System.currentTimeMillis()}.vaultenc")
                googleDriveService.downloadBackupFile(token, driveFile.id, tempDownload)

                _backupUiState.value = _backupUiState.value.copy(
                    statusMessage = "Decrypting and restoring vault files..."
                )

                val bytes = tempDownload.readBytes()
                val result = backupPackageService.restoreEncryptedBackupPackage(bytes, passphrase)
                tempDownload.delete()

                _backupUiState.value = _backupUiState.value.copy(
                    isRestoreInProgress = false,
                    statusMessage = null
                )
                _userEvents.emit("Restored ${result.restoredMediaCount} media items and ${result.restoredAlbumsCount} albums!")
            } catch (e: Exception) {
                _backupUiState.value = _backupUiState.value.copy(
                    isRestoreInProgress = false,
                    statusMessage = "Restore failed: ${e.message}"
                )
                _userEvents.emit("Restore failed: Incorrect passphrase or corrupt file")
            }
        }
    }

    fun deleteGoogleDriveBackup(driveFile: GoogleDriveBackupFile) {
        val account = googleDriveService.getSignedInAccount() ?: return
        viewModelScope.launch {
            try {
                val token = googleDriveService.getAccessToken(account)
                googleDriveService.deleteBackupFile(token, driveFile.id)
                val updated = googleDriveService.listBackups(token)
                _backupUiState.value = _backupUiState.value.copy(driveBackups = updated)
                _userEvents.emit("Backup deleted from Google Drive")
            } catch (e: Exception) {
                _userEvents.emit("Could not delete backup: ${e.message}")
            }
        }
    }

    fun createLocalEncryptedBackup(passphrase: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            _backupUiState.value = _backupUiState.value.copy(
                isBackupInProgress = true,
                statusMessage = "Creating local encrypted backup..."
            )
            try {
                val backupFile = backupPackageService.createEncryptedBackupPackage(passphrase)
                val now = System.currentTimeMillis()
                securityManager.setLastBackupTime(now)
                _backupUiState.value = _backupUiState.value.copy(
                    isBackupInProgress = false,
                    lastBackupTimestamp = now,
                    statusMessage = null
                )
                onComplete(backupFile)
            } catch (e: Exception) {
                _backupUiState.value = _backupUiState.value.copy(
                    isBackupInProgress = false,
                    statusMessage = "Local backup failed: ${e.message}"
                )
                _userEvents.emit("Backup failed: ${e.message}")
                onComplete(null)
            }
        }
    }

    fun restoreFromLocalBackup(backupBytes: ByteArray, passphrase: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _backupUiState.value = _backupUiState.value.copy(
                isRestoreInProgress = true,
                statusMessage = "Decrypting and restoring vault..."
            )
            try {
                val result = backupPackageService.restoreEncryptedBackupPackage(backupBytes, passphrase)
                _backupUiState.value = _backupUiState.value.copy(
                    isRestoreInProgress = false,
                    statusMessage = null
                )
                _userEvents.emit("Successfully restored ${result.restoredMediaCount} items!")
                onComplete(true)
            } catch (e: Exception) {
                _backupUiState.value = _backupUiState.value.copy(
                    isRestoreInProgress = false,
                    statusMessage = "Restore failed: ${e.message}"
                )
                _userEvents.emit("Restore failed: Incorrect password or invalid file")
                onComplete(false)
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
