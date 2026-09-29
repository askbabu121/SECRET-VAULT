package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.security.BiometricAuthManager
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.BackupPassphraseInputDialog
import com.example.ui.screens.LockScreen
import com.example.ui.screens.MediaViewerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupPinScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.screens.VaultDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultDarkBg
import com.example.viewmodel.VaultTab
import com.example.viewmodel.VaultViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import java.io.File

class MainActivity : FragmentActivity() {

    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val lifecycleOwner = LocalLifecycleOwner.current

                // Auto-lock when app moves to background
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) {
                            viewModel.onAppBackgrounded()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                VaultAppContent(
                    viewModel = viewModel,
                    onTriggerBiometric = { promptBiometricAuth() }
                )
            }
        }
    }

    private fun promptBiometricAuth() {
        BiometricAuthManager.showBiometricPrompt(
            activity = this,
            title = "Unlock Secret Vault",
            subtitle = "Verify your fingerprint or face to access",
            negativeButtonText = "Use PIN",
            onSuccess = {
                viewModel.unlockWithBiometric()
            },
            onError = { errorMsg ->
                // Feedback handled
            }
        )
    }
}

@Composable
fun VaultAppContent(
    viewModel: VaultViewModel,
    onTriggerBiometric: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaList by viewModel.activeMediaList.collectAsStateWithLifecycle()
    val albumsList by viewModel.albumsList.collectAsStateWithLifecycle()
    val trashList by viewModel.trashList.collectAsStateWithLifecycle()
    val breakInLogs by viewModel.breakInLogs.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val decoyMediaCount by viewModel.decoyMediaCount.collectAsStateWithLifecycle()
    val backupUiState by viewModel.backupUiState.collectAsStateWithLifecycle()

    var pendingExportFile by remember { mutableStateOf<File?>(null) }
    var pendingImportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showImportPassphraseDialog by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.handleGoogleSignInResult(account)
            }
        } catch (e: Exception) {
            viewModel.postUserMessage("Google Sign-In failed: ${e.message}")
        }
    }

    val createBackupDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null && pendingExportFile != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    pendingExportFile!!.inputStream().use { it.copyTo(out) }
                }
                viewModel.postUserMessage("Encrypted backup saved to file successfully!")
            } catch (e: Exception) {
                viewModel.postUserMessage("Failed to save backup: ${e.message}")
            } finally {
                pendingExportFile?.delete()
                pendingExportFile = null
            }
        }
    }

    val pickBackupDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    pendingImportBytes = bytes
                    showImportPassphraseDialog = true
                }
            } catch (e: Exception) {
                viewModel.postUserMessage("Could not read backup file: ${e.message}")
            }
        }
    }

    val isBiometricAvailable = remember(context) {
        BiometricAuthManager.isBiometricAvailable(context)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.userEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Auto prompt biometric when locked if enabled
    LaunchedEffect(uiState.isUnlocked, uiState.isBiometricEnabled) {
        if (!uiState.isUnlocked && uiState.isBiometricEnabled && isBiometricAvailable) {
            onTriggerBiometric()
        }
    }

    // Handle Back Press depending on screen state
    BackHandler(enabled = true) {
        when {
            uiState.selectedMediaItem != null -> viewModel.closeMediaViewer()
            uiState.selectedAlbum != null -> viewModel.closeAlbum()
            uiState.isSelectionMode -> viewModel.clearSelection()
            uiState.activeTab != VaultTab.ALL -> viewModel.setActiveTab(VaultTab.ALL)
            uiState.isUnlocked -> viewModel.lockVault()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = VaultDarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(uiState.isUnlocked) {
                    if (uiState.isUnlocked) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                viewModel.onUserActivity()
                            }
                        }
                    }
                }
        ) {
            when {
                // First-time onboarding: Set up Master PIN & Security question
                !uiState.isPinConfigured -> {
                    SetupPinScreen(
                        onSetupComplete = { pin, question, answer, stealthCalculator ->
                            viewModel.setupInitialPin(
                                pin = pin,
                                question = question,
                                answer = answer,
                                stealthCalculator = stealthCalculator
                            )
                        }
                    )
                }

                // Locked: Show Calculator Disguise or Keypad Lock Screen
                !uiState.isUnlocked -> {
                    LockScreen(
                        isStealthMode = uiState.isStealthCalculatorMode,
                        onToggleStealthMode = { viewModel.setStealthCalculatorMode(it) },
                        onPinEntered = { enteredPin ->
                            viewModel.verifyPin(enteredPin)
                        },
                        isBiometricEnabled = uiState.isBiometricEnabled && isBiometricAvailable,
                        onBiometricClick = onTriggerBiometric,
                        securityQuestion = viewModel.getSecurityQuestion(),
                        onResetPinWithRecovery = { answer, newPin ->
                            viewModel.verifySecurityAnswerAndReset(answer, newPin)
                        }
                    )
                }

                // Fullscreen Media Viewer
                uiState.selectedMediaItem != null -> {
                    val currentItem = uiState.selectedMediaItem!!
                    MediaViewerScreen(
                        item = currentItem,
                        internalFile = viewModel.getInternalFile(currentItem.filePath),
                        albums = albumsList,
                        onClose = { viewModel.closeMediaViewer() },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onExportToGallery = { viewModel.exportSingleItem(it) },
                        onMoveToTrash = { viewModel.moveSingleToTrash(it) },
                        onMoveToAlbum = { albumId -> viewModel.moveSelectedToAlbum(albumId) },
                        onUpdateNotes = { id, notes -> viewModel.updateMediaNotes(id, notes) }
                    )
                }

                // Albums Tab
                uiState.activeTab == VaultTab.ALBUMS && uiState.selectedAlbum == null -> {
                    AlbumsScreen(
                        albums = albumsList,
                        onSelectAlbum = { viewModel.openAlbum(it) },
                        onCreateAlbum = { viewModel.createAlbum(it) },
                        onDeleteAlbum = { viewModel.deleteAlbum(it) }
                    )
                }

                // Trash Tab
                uiState.activeTab == VaultTab.TRASH -> {
                    TrashScreen(
                        trashItems = trashList,
                        getFileForMedia = { path -> viewModel.getInternalFile(path) },
                        onRestore = { viewModel.restoreSingleFromTrash(it) },
                        onDeletePermanently = { viewModel.deleteSinglePermanently(it) },
                        onEmptyTrash = {
                            viewModel.deletePermanentlySelected()
                        }
                    )
                }

                // Settings Tab
                uiState.activeTab == VaultTab.SETTINGS -> {
                    SettingsScreen(
                        isStealthMode = uiState.isStealthCalculatorMode,
                        onToggleStealthMode = { viewModel.setStealthCalculatorMode(it) },
                        currentDisguise = uiState.currentIconDisguise,
                        onSelectDisguise = { disguise -> viewModel.setAppIconDisguise(disguise) },
                        hasDecoyPin = viewModel.hasDecoyPin(),
                        onSetDecoyPin = { viewModel.setDecoyPin(it) },
                        decoyMediaCount = decoyMediaCount,
                        onPopulateDecoyDummyMedia = { viewModel.populateDecoyDummyMedia() },
                        onClearDecoyMedia = { viewModel.clearDecoyMedia() },
                        isBiometricEnabled = uiState.isBiometricEnabled,
                        onToggleBiometric = { enabled -> viewModel.setBiometricEnabled(enabled) },
                        isBiometricAvailableOnDevice = isBiometricAvailable,
                        inactivityTimeoutSeconds = uiState.inactivityTimeoutSeconds,
                        onSetInactivityTimeout = { timeout -> viewModel.setInactivityTimeoutSeconds(timeout) },
                        backupUiState = backupUiState,
                        onConnectGoogleDrive = {
                            val signInIntent = viewModel.getGoogleSignInClient().signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        },
                        onDisconnectGoogleDrive = { viewModel.signOutGoogle() },
                        onRefreshDriveBackups = { viewModel.fetchDriveBackups() },
                        onBackupToDrive = { pass -> viewModel.backupToGoogleDrive(pass) },
                        onRestoreFromDrive = { file, pass -> viewModel.restoreFromGoogleDrive(file, pass) },
                        onDeleteDriveBackup = { file -> viewModel.deleteGoogleDriveBackup(file) },
                        onExportLocalBackup = { pass ->
                            viewModel.createLocalEncryptedBackup(pass) { file ->
                                if (file != null) {
                                    pendingExportFile = file
                                    createBackupDocLauncher.launch(file.name)
                                }
                            }
                        },
                        onImportLocalBackup = {
                            pickBackupDocLauncher.launch(arrayOf("*/*"))
                        },
                        securityQuestion = viewModel.getSecurityQuestion(),
                        onUpdatePin = { newPin -> viewModel.setupInitialPin(newPin, viewModel.getSecurityQuestion(), "", uiState.isStealthCalculatorMode) },
                        breakInLogs = breakInLogs,
                        onClearBreakInLogs = { viewModel.clearBreakInHistory() },
                        storageStats = storageStats
                    )
                }

                // Dashboard (All, Photos, Videos, Favorites, or Album Content)
                else -> {
                    VaultDashboardScreen(
                        uiState = uiState,
                        mediaList = mediaList,
                        albumsList = albumsList,
                        getFileForMedia = { path -> viewModel.getInternalFile(path) },
                        onTabSelected = { viewModel.setActiveTab(it) },
                        onCloseAlbum = { viewModel.closeAlbum() },
                        onMediaClick = { viewModel.openMediaViewer(it) },
                        onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                        onToggleItemSelection = { viewModel.toggleItemSelection(it) },
                        onSelectAll = { viewModel.selectAllMedia(it) },
                        onClearSelection = { viewModel.clearSelection() },
                        onLockVault = { viewModel.lockVault() },
                        onImportUris = { viewModel.importUris(it) },
                        onCameraCapture = { viewModel.importCameraPhoto(it) },
                        onMoveSelectedToTrash = { viewModel.moveSelectedToTrash() },
                        onExportSelectedToDevice = { viewModel.exportSelectedToDevice() },
                        onMoveSelectedToAlbum = { viewModel.moveSelectedToAlbum(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onPopulateDecoyDummyMedia = { viewModel.populateDecoyDummyMedia() }
                    )
                }
            }

            // Global loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = VaultCyan)
                }
            }

            // Local file restore passphrase dialog
            if (showImportPassphraseDialog && pendingImportBytes != null) {
                BackupPassphraseInputDialog(
                    title = "Decrypt & Restore File",
                    description = "Enter the PIN or passphrase used when this .vaultenc backup file was created.",
                    confirmButtonText = "Decrypt & Restore",
                    onDismiss = {
                        showImportPassphraseDialog = false
                        pendingImportBytes = null
                    },
                    onConfirm = { pass ->
                        showImportPassphraseDialog = false
                        val bytes = pendingImportBytes!!
                        pendingImportBytes = null
                        viewModel.restoreFromLocalBackup(bytes, pass) { _ -> }
                    }
                )
            }
        }
    }
}
