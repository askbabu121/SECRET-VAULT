package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.GoogleDriveBackupFile
import com.example.data.model.BreakInLogEntity
import com.example.data.repository.VaultStorageStats
import com.example.data.security.AppIconDisguise
import com.example.viewmodel.VaultBackupUiState
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultIndigo
import com.example.ui.theme.VaultRose
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    isStealthMode: Boolean,
    onToggleStealthMode: (Boolean) -> Unit,
    currentDisguise: AppIconDisguise = AppIconDisguise.VAULT,
    onSelectDisguise: (AppIconDisguise) -> Unit = {},
    hasDecoyPin: Boolean,
    onSetDecoyPin: (String?) -> Unit,
    decoyMediaCount: Int,
    onPopulateDecoyDummyMedia: () -> Unit,
    onClearDecoyMedia: () -> Unit,
    isBiometricEnabled: Boolean = false,
    onToggleBiometric: (Boolean) -> Unit = {},
    isBiometricAvailableOnDevice: Boolean = true,
    inactivityTimeoutSeconds: Int = 60,
    onSetInactivityTimeout: (Int) -> Unit = {},
    backupUiState: VaultBackupUiState = VaultBackupUiState(),
    onConnectGoogleDrive: () -> Unit = {},
    onDisconnectGoogleDrive: () -> Unit = {},
    onRefreshDriveBackups: () -> Unit = {},
    onBackupToDrive: (passphrase: String) -> Unit = {},
    onRestoreFromDrive: (GoogleDriveBackupFile, passphrase: String) -> Unit = { _, _ -> },
    onDeleteDriveBackup: (GoogleDriveBackupFile) -> Unit = {},
    onExportLocalBackup: (passphrase: String) -> Unit = {},
    onImportLocalBackup: () -> Unit = {},
    securityQuestion: String,
    onUpdatePin: (newPin: String) -> Unit,
    breakInLogs: List<BreakInLogEntity>,
    onClearBreakInLogs: () -> Unit,
    storageStats: VaultStorageStats
) {
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDecoyPinDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showPrivacyInfoDialog by remember { mutableStateOf(false) }
    var showDisguiseDialog by remember { mutableStateOf(false) }
    var showInactivityDialog by remember { mutableStateOf(false) }
    var showCloudBackupDialog by remember { mutableStateOf(false) }
    var showDownloadAppGuideDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Storage Usage Card
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = VaultCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Vault Storage",
                                fontWeight = FontWeight.Bold,
                                color = VaultTextPrimary,
                                fontSize = 15.sp
                            )
                        }

                        Text(
                            text = formatSize(storageStats.totalSizeBytes),
                            color = VaultCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { 0.15f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = VaultCyan,
                        trackColor = VaultSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Device Free Space: ${formatSize(storageStats.availableSpaceBytes)}",
                        color = VaultTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Section: Stealth & Disguise
        item {
            Text(
                text = "STEALTH & DISGUISE",
                color = VaultTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VaultCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard)
            ) {
                Column {
                    SettingsSwitchRow(
                        icon = Icons.Default.Calculate,
                        title = "Calculator Disguise",
                        subtitle = "Disguise app as a working calculator. Enter PIN + '=' to open vault.",
                        checked = isStealthMode,
                        onCheckedChange = onToggleStealthMode
                    )

                    HorizontalDivider(color = VaultCardBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        icon = Icons.Default.AppShortcut,
                        title = "App Icon & Name Disguise",
                        subtitle = "Home Screen: ${currentDisguise.displayName}",
                        onClick = { showDisguiseDialog = true }
                    )
                }
            }
        }

        // Section: Security & Passcode
        item {
            Text(
                text = "SECURITY & PASSCODES",
                color = VaultTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VaultCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard)
            ) {
                Column {
                    SettingsSwitchRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Authentication",
                        subtitle = if (isBiometricAvailableOnDevice) "Unlock vault with fingerprint or face recognition" else "Biometrics not enrolled or supported on device",
                        checked = isBiometricEnabled,
                        onCheckedChange = onToggleBiometric
                    )

                    HorizontalDivider(color = VaultCardBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        icon = Icons.Default.LockReset,
                        title = "Change Master PIN",
                        subtitle = "Update your primary vault unlock code",
                        onClick = { showChangePinDialog = true }
                    )

                    HorizontalDivider(color = VaultCardBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        icon = Icons.Default.Timer,
                        title = "Auto-Lock on Inactivity",
                        subtitle = formatInactivitySummary(inactivityTimeoutSeconds),
                        onClick = { showInactivityDialog = true }
                    )

                    HorizontalDivider(color = VaultCardBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        icon = Icons.Default.VisibilityOff,
                        title = "Decoy Fake PIN",
                        subtitle = if (hasDecoyPin) "Decoy active ($decoyMediaCount dummy items)" else "Set a secondary fake PIN with dummy media",
                        onClick = { showDecoyPinDialog = true }
                    )

                    HorizontalDivider(color = VaultCardBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        icon = Icons.Default.Report,
                        title = "Break-in Logs",
                        subtitle = "${breakInLogs.size} failed attempt${if (breakInLogs.size != 1) "s" else ""} logged",
                        onClick = { showLogsDialog = true }
                    )
                }
            }
        }

        // Section: Encrypted Cloud Backup & Recovery
        item {
            Text(
                text = "ENCRYPTED CLOUD BACKUP & RECOVERY",
                color = VaultTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VaultCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard)
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.Default.CloudDone,
                        title = "Google Drive Encrypted Backup",
                        subtitle = if (backupUiState.signedInGoogleEmail != null)
                            "Connected: ${backupUiState.signedInGoogleEmail} · ${backupUiState.driveBackups.size} cloud backups"
                        else
                            "Encrypt and back up vault contents to Google Drive",
                        onClick = { showCloudBackupDialog = true }
                    )

                    HorizontalDivider(color = VaultCardBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        icon = Icons.Default.Download,
                        title = "Download App & Install Package",
                        subtitle = "Instructions to download APK or transfer to a new device",
                        onClick = { showDownloadAppGuideDialog = true }
                    )
                }
            }
        }

        // Section: Data & Isolation Info
        item {
            Text(
                text = "SANDBOX PRIVACY",
                color = VaultTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VaultCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VaultCard)
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.Default.EnhancedEncryption,
                        title = "Private Storage Architecture",
                        subtitle = "How your photos & videos are protected from other apps",
                        onClick = { showPrivacyInfoDialog = true }
                    )
                }
            }
        }
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onPinChanged = { newPin ->
                onUpdatePin(newPin)
                showChangePinDialog = false
            }
        )
    }

    // Decoy PIN Dialog
    if (showDecoyPinDialog) {
        DecoyPinDialog(
            hasDecoyPin = hasDecoyPin,
            decoyMediaCount = decoyMediaCount,
            onDismiss = { showDecoyPinDialog = false },
            onSaveDecoyPin = { pin, populateDummy ->
                onSetDecoyPin(pin)
                if (populateDummy) {
                    onPopulateDecoyDummyMedia()
                }
                showDecoyPinDialog = false
            },
            onPopulateDummyMedia = onPopulateDecoyDummyMedia,
            onClearDecoyMedia = onClearDecoyMedia
        )
    }

    // Break In Logs Dialog
    if (showLogsDialog) {
        BreakInLogsDialog(
            logs = breakInLogs,
            onDismiss = { showLogsDialog = false },
            onClear = onClearBreakInLogs
        )
    }

    // Privacy Info Dialog
    if (showPrivacyInfoDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyInfoDialog = false },
            containerColor = VaultSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = VaultCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Private Sandboxed Storage", color = VaultTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "1. Isolated File Directory: Imported media is copied into the app's sandboxed internal storage directory on this phone.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        "2. Invisible to System Gallery: Android's MediaStore and standard gallery apps (e.g., Google Photos, Samsung Gallery) are strictly prohibited by the Android OS from scanning private app sandbox files.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        "3. Complete Local Control: Nothing is uploaded to cloud servers without your permission. Your media stays 100% on your device.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        "4. Unhide Anytime: You can export/unhide any photo or video back to your public gallery whenever you choose.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
                ) {
                    Text("Got It", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showDisguiseDialog) {
        AppIconDisguiseDialog(
            currentDisguise = currentDisguise,
            onDismiss = { showDisguiseDialog = false },
            onSelectDisguise = onSelectDisguise
        )
    }

    if (showInactivityDialog) {
        InactivityTimeoutDialog(
            currentTimeout = inactivityTimeoutSeconds,
            onDismiss = { showInactivityDialog = false },
            onSelectTimeout = { timeout ->
                onSetInactivityTimeout(timeout)
                showInactivityDialog = false
            }
        )
    }

    if (showCloudBackupDialog) {
        CloudBackupDialog(
            backupUiState = backupUiState,
            onConnectGoogleDrive = onConnectGoogleDrive,
            onDisconnectGoogleDrive = onDisconnectGoogleDrive,
            onRefreshBackups = onRefreshDriveBackups,
            onBackupToDrive = onBackupToDrive,
            onRestoreFromDrive = onRestoreFromDrive,
            onDeleteDriveBackup = onDeleteDriveBackup,
            onExportLocalBackup = onExportLocalBackup,
            onImportLocalBackup = onImportLocalBackup,
            onShowDownloadAppGuide = {
                showDownloadAppGuideDialog = true
            },
            onDismiss = { showCloudBackupDialog = false }
        )
    }

    if (showDownloadAppGuideDialog) {
        DownloadAppGuideDialog(
            onDismiss = { showDownloadAppGuideDialog = false }
        )
    }
}

private fun formatInactivitySummary(seconds: Int): String {
    return when (seconds) {
        15 -> "After 15 seconds of inactivity"
        30 -> "After 30 seconds of inactivity"
        60 -> "After 1 minute of inactivity"
        120 -> "After 2 minutes of inactivity"
        300 -> "After 5 minutes of inactivity"
        0 -> "Disabled (Never auto-lock)"
        else -> "After $seconds seconds of inactivity"
    }
}

data class InactivityOption(
    val seconds: Int,
    val title: String,
    val description: String
)

@Composable
fun InactivityTimeoutDialog(
    currentTimeout: Int,
    onDismiss: () -> Unit,
    onSelectTimeout: (Int) -> Unit
) {
    val options = remember {
        listOf(
            InactivityOption(15, "15 seconds", "Maximum privacy, locks rapidly"),
            InactivityOption(30, "30 seconds", "High security for quick visits"),
            InactivityOption(60, "1 minute", "Recommended standard balance"),
            InactivityOption(120, "2 minutes", "Convenient for reviewing media"),
            InactivityOption(300, "5 minutes", "Extended viewing time"),
            InactivityOption(0, "Never", "Manual lock only")
        )
    }

    var selected by remember { mutableStateOf(currentTimeout) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = VaultCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Auto-Lock on Inactivity",
                    color = VaultTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Automatically secure and conceal the vault after a period of no screen touches while unlocked.",
                    color = VaultTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                options.forEach { option ->
                    val isChecked = selected == option.seconds
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option.seconds }
                            .border(
                                1.5.dp,
                                if (isChecked) VaultCyan else VaultCardBorder,
                                RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) VaultCyan.copy(alpha = 0.12f) else VaultCard
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    color = VaultTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = option.description,
                                    color = VaultTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            RadioButton(
                                selected = isChecked,
                                onClick = { selected = option.seconds },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = VaultCyan,
                                    unselectedColor = VaultCardBorder
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectTimeout(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
            ) {
                Text("Save", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
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
fun AppIconDisguiseDialog(
    currentDisguise: AppIconDisguise,
    onDismiss: () -> Unit,
    onSelectDisguise: (AppIconDisguise) -> Unit
) {
    var selected by remember { mutableStateOf(currentDisguise) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AppShortcut, contentDescription = null, tint = VaultCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("App Icon & Name Disguise", color = VaultTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Change the launcher icon and label shown on your phone's home screen. The app disguise makes it appear as an innocent utility.",
                    color = VaultTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                AppIconDisguise.entries.forEach { option ->
                    val isChecked = selected == option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option }
                            .border(
                                1.5.dp,
                                if (isChecked) VaultCyan else VaultCardBorder,
                                RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) VaultCyan.copy(alpha = 0.12f) else VaultCard
                        )
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
                                // Preview icon badge
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when (option) {
                                                AppIconDisguise.NOTES -> Color(0xFFF59E0B)
                                                AppIconDisguise.CALCULATOR -> Color(0xFF0284C7)
                                                AppIconDisguise.CLOCK -> Color(0xFF4F46E5)
                                                AppIconDisguise.VAULT -> Color(0xFF0F172A)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (option) {
                                            AppIconDisguise.NOTES -> Icons.Default.EditNote
                                            AppIconDisguise.CALCULATOR -> Icons.Default.Calculate
                                            AppIconDisguise.CLOCK -> Icons.Default.AccessTime
                                            AppIconDisguise.VAULT -> Icons.Default.Shield
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = option.displayName,
                                        color = VaultTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = option.subtitle,
                                        color = VaultTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            RadioButton(
                                selected = isChecked,
                                onClick = { selected = option },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = VaultCyan,
                                    unselectedColor = VaultCardBorder
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelectDisguise(selected)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
            ) {
                Text("Apply Disguise", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
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
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VaultSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(text = title, color = VaultTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = subtitle, color = VaultTextSecondary, fontSize = 12.sp)
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = VaultTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VaultSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = VaultCyan, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(text = title, color = VaultTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = subtitle, color = VaultTextSecondary, fontSize = 12.sp)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VaultCyan,
                checkedTrackColor = VaultCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = VaultTextMuted,
                uncheckedTrackColor = VaultCardBorder
            )
        )
    }
}

@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    onPinChanged: (String) -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = { Text("Change Master PIN", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text("New 4-digit PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultCyan,
                        unfocusedBorderColor = VaultCardBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    )
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text("Confirm New PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultCyan,
                        unfocusedBorderColor = VaultCardBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    )
                )

                error?.let {
                    Text(text = it, color = VaultRose, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length < 4) {
                        error = "PIN must be 4 digits"
                    } else if (newPin != confirmPin) {
                        error = "PINs do not match"
                    } else {
                        onPinChanged(newPin)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
            ) {
                Text("Save PIN", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
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
fun DecoyPinDialog(
    hasDecoyPin: Boolean,
    decoyMediaCount: Int,
    onDismiss: () -> Unit,
    onSaveDecoyPin: (pin: String, populateDummy: Boolean) -> Unit,
    onPopulateDummyMedia: () -> Unit,
    onClearDecoyMedia: () -> Unit
) {
    var decoyPin by remember { mutableStateOf("") }
    var autoPopulate by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = VaultCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Decoy Vault & PIN", color = VaultTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "A Decoy PIN opens a fake secondary vault if someone forces you to unlock your phone. To make it convincing, you can populate it with realistic harmless photos.",
                    color = VaultTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = decoyPin,
                    onValueChange = { if (it.length <= 4) decoyPin = it },
                    label = { Text("4-digit Decoy PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultCyan,
                        unfocusedBorderColor = VaultCardBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { autoPopulate = !autoPopulate }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = autoPopulate,
                        onCheckedChange = { autoPopulate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VaultCyan,
                            checkedTrackColor = VaultCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = VaultTextMuted,
                            uncheckedTrackColor = VaultCardBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Populate with Dummy Media",
                            color = VaultTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Adds harmless photos (recipes, dog, sunset)",
                            color = VaultTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (hasDecoyPin) {
                    HorizontalDivider(color = VaultCardBorder, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Decoy Items: $decoyMediaCount",
                            color = VaultCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        TextButton(onClick = onPopulateDummyMedia) {
                            Text("+ Add Dummy Media", color = VaultCyan, fontSize = 12.sp)
                        }
                    }

                    if (decoyMediaCount > 0) {
                        TextButton(
                            onClick = onClearDecoyMedia,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Clear Decoy Media", color = VaultRose, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (decoyPin.length == 4) {
                        onSaveDecoyPin(decoyPin, autoPopulate)
                    }
                },
                enabled = decoyPin.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
            ) {
                Text("Save Decoy PIN", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (hasDecoyPin) {
                    TextButton(onClick = { onSaveDecoyPin("", false) }) {
                        Text("Remove Decoy", color = VaultRose)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = VaultTextSecondary)
                }
            }
        }
    )
}

@Composable
fun BreakInLogsDialog(
    logs: List<BreakInLogEntity>,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Break-in Attempts", color = VaultTextPrimary, fontWeight = FontWeight.Bold)
                if (logs.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = VaultRose)
                    }
                }
            }
        },
        text = {
            if (logs.isEmpty()) {
                Text("No break-in attempts recorded. Your vault is secure!", color = VaultEmerald, fontSize = 13.sp)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { log ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = VaultCard),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Wrong PIN Entered", color = VaultRose, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                                        color = VaultTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    log.enteredCodeMasked,
                                    color = VaultTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = VaultCyan)
            }
        }
    )
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.getDefault(), "%.2f GB", gb)
}
