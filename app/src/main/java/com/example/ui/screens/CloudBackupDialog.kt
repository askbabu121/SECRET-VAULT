package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.backup.GoogleDriveBackupFile
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import com.example.viewmodel.VaultBackupUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudBackupDialog(
    backupUiState: VaultBackupUiState,
    onConnectGoogleDrive: () -> Unit,
    onDisconnectGoogleDrive: () -> Unit,
    onRefreshBackups: () -> Unit,
    onBackupToDrive: (passphrase: String) -> Unit,
    onRestoreFromDrive: (GoogleDriveBackupFile, passphrase: String) -> Unit,
    onDeleteDriveBackup: (GoogleDriveBackupFile) -> Unit,
    onExportLocalBackup: (passphrase: String) -> Unit,
    onImportLocalBackup: () -> Unit,
    onShowDownloadAppGuide: () -> Unit,
    onDismiss: () -> Unit
) {
    var showBackupPassphraseDialog by remember { mutableStateOf(false) }
    var showExportLocalPassphraseDialog by remember { mutableStateOf(false) }
    var selectedRestoreFile by remember { mutableStateOf<GoogleDriveBackupFile?>(null) }
    var fileToDelete by remember { mutableStateOf<GoogleDriveBackupFile?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp))
                .background(VaultDarkBg)
                .border(1.5.dp, VaultCardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VaultCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = VaultCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Google Drive Backup",
                                color = VaultTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "AES-256-GCM Zero-Knowledge Recovery",
                                color = VaultCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Close", color = VaultTextSecondary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Google Drive Status Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, VaultCardBorder, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = VaultCard)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Google Drive Account",
                                            color = VaultTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = if (backupUiState.signedInGoogleEmail != null)
                                                "Connected: ${backupUiState.signedInGoogleEmail}"
                                            else
                                                "Not connected · Sign in to enable cloud backup",
                                            color = if (backupUiState.signedInGoogleEmail != null) VaultCyan else VaultTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (backupUiState.signedInGoogleEmail != null) {
                                        TextButton(onClick = onDisconnectGoogleDrive) {
                                            Text("Disconnect", color = Color(0xFFF87171), fontSize = 12.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = onConnectGoogleDrive,
                                            colors = ButtonDefaults.buttonColors(containerColor = VaultCyan),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Connect", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }

                                if (backupUiState.lastBackupTimestamp > 0L) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val dateStr = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
                                        .format(Date(backupUiState.lastBackupTimestamp))
                                    Text(
                                        text = "Last backup: $dateStr",
                                        color = VaultTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // In-progress status banner
                    if (backupUiState.isBackupInProgress || backupUiState.isRestoreInProgress) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, VaultCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = VaultCyan.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp,
                                        color = VaultCyan
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = backupUiState.statusMessage ?: "Processing backup operation...",
                                        color = VaultTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Main Action Buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showBackupPassphraseDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = backupUiState.signedInGoogleEmail != null && !backupUiState.isBackupInProgress && !backupUiState.isRestoreInProgress,
                                colors = ButtonDefaults.buttonColors(containerColor = VaultCyan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF0B0F19), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Back Up Now", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = onRefreshBackups,
                                enabled = backupUiState.signedInGoogleEmail != null && !backupUiState.isFetchingDriveBackups,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultCyan)
                            ) {
                                if (backupUiState.isFetchingDriveBackups) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = VaultCyan)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Cloud Backups List Section
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CLOUD BACKUPS (${backupUiState.driveBackups.size})",
                                color = VaultTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (backupUiState.signedInGoogleEmail == null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = VaultCard)
                            ) {
                                Text(
                                    text = "Connect Google Drive to see existing encrypted backups or create a new recovery point.",
                                    color = VaultTextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else if (backupUiState.driveBackups.isEmpty() && !backupUiState.isFetchingDriveBackups) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = VaultCard)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, tint = VaultTextSecondary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No Google Drive backups yet",
                                        color = VaultTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Tap 'Back Up Now' to create your first encrypted backup.",
                                        color = VaultTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(backupUiState.driveBackups) { file ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, VaultCardBorder, RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = VaultCard)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(VaultCyan.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.EnhancedEncryption,
                                                contentDescription = null,
                                                tint = VaultCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = file.formattedDate,
                                                color = VaultTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Size: ${file.formattedSize} · Encrypted",
                                                color = VaultTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = { selectedRestoreFile = file },
                                            enabled = !backupUiState.isRestoreInProgress && !backupUiState.isBackupInProgress,
                                            colors = ButtonDefaults.buttonColors(containerColor = VaultCyan.copy(alpha = 0.2f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Restore,
                                                contentDescription = null,
                                                tint = VaultCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore", color = VaultCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(onClick = { fileToDelete = file }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete backup",
                                                tint = Color(0xFFF87171).copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Local & Offline Backup Section
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "OFFLINE & LOCAL BACKUP",
                            color = VaultTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, VaultCardBorder, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = VaultCard)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showExportLocalPassphraseDialog = true }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Export Encrypted Backup (.vaultenc)", color = VaultTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("Save encrypted file locally to share or store anywhere", color = VaultTextSecondary, fontSize = 11.sp)
                                    }
                                }

                                HorizontalDivider(color = VaultCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onImportLocalBackup() }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Import & Restore from File", color = VaultTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("Select a .vaultenc file to restore your media", color = VaultTextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // App Download Guide Link
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShowDownloadAppGuide() }
                                .border(1.dp, VaultCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = VaultCyan.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Download APK / Install on Devices", color = VaultTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Learn how to download the APK or transfer to other phones", color = VaultCyan, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Enter Passphrase to Backup to Google Drive
    if (showBackupPassphraseDialog) {
        BackupPassphraseInputDialog(
            title = "Encrypt & Back Up to Drive",
            description = "Enter an encryption passphrase or your Master PIN. All media files and albums will be encrypted using military-grade AES-256-GCM before uploading to Google Drive.",
            confirmButtonText = "Start Backup",
            onDismiss = { showBackupPassphraseDialog = false },
            onConfirm = { pass ->
                showBackupPassphraseDialog = false
                onBackupToDrive(pass)
            }
        )
    }

    // Dialog: Enter Passphrase to Export Local Backup
    if (showExportLocalPassphraseDialog) {
        BackupPassphraseInputDialog(
            title = "Export Encrypted Backup",
            description = "Choose a passphrase or use your Master PIN to encrypt this local backup package (.vaultenc).",
            confirmButtonText = "Export Package",
            onDismiss = { showExportLocalPassphraseDialog = false },
            onConfirm = { pass ->
                showExportLocalPassphraseDialog = false
                onExportLocalBackup(pass)
            }
        )
    }

    // Dialog: Restore confirmation and passphrase entry
    selectedRestoreFile?.let { driveFile ->
        BackupPassphraseInputDialog(
            title = "Restore from Google Drive",
            description = "Enter the PIN/passphrase used when creating this backup (${driveFile.formattedDate}). Your photos, videos, and albums will be securely restored.",
            confirmButtonText = "Decrypt & Restore",
            onDismiss = { selectedRestoreFile = null },
            onConfirm = { pass ->
                onRestoreFromDrive(driveFile, pass)
                selectedRestoreFile = null
            }
        )
    }

    // Dialog: Confirm Delete Backup
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            containerColor = VaultSurface,
            title = { Text("Delete Cloud Backup?", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete the backup from ${file.formattedDate} (${file.formattedSize}) from Google Drive?",
                    color = VaultTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDriveBackup(file)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel", color = VaultTextSecondary)
                }
            }
        )
    }
}

@Composable
fun BackupPassphraseInputDialog(
    title: String,
    description: String,
    confirmButtonText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = VaultCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = VaultTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(description, color = VaultTextSecondary, fontSize = 13.sp)

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Encryption Passphrase / PIN") },
                    placeholder = { Text("Enter PIN or passphrase") },
                    singleLine = true,
                    visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isVisible = !isVisible }) {
                            Icon(
                                if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = VaultTextSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultCyan,
                        unfocusedBorderColor = VaultCardBorder,
                        focusedLabelColor = VaultCyan,
                        unfocusedLabelColor = VaultTextSecondary,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AES-256 authenticated encryption", color = VaultCyan, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (passphrase.isNotBlank()) onConfirm(passphrase.trim()) },
                enabled = passphrase.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
            ) {
                Text(confirmButtonText, color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextSecondary)
            }
        }
    )
}

@Composable
fun DownloadAppGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = null, tint = VaultCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download & Install App", color = VaultTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "You can download the APK package directly to your phone or Android device:",
                    color = VaultTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VaultCardBorder, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = VaultCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("1. Top-Right Settings Menu", color = VaultCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("In Google AI Studio, click the settings / download menu at the top-right of your workspace.", color = VaultTextSecondary, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("2. Export APK or ZIP", color = VaultCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Select 'Download APK' to get the installable .apk file directly, or 'Download ZIP' to get the full source code.", color = VaultTextSecondary, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("3. Device Recovery & Migration", color = VaultCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Install the app on any new phone, connect your Google Drive, and restore your encrypted vault with one click!", color = VaultTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
            ) {
                Text("Got It", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
            }
        }
    )
}
