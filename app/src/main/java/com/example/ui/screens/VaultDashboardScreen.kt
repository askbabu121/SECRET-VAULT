package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AlbumEntity
import com.example.data.model.MediaItemEntity
import com.example.ui.theme.VaultAmber
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
import com.example.viewmodel.VaultTab
import com.example.viewmodel.VaultUiState
import java.io.File
import java.util.Locale

@Composable
fun VaultDashboardScreen(
    uiState: VaultUiState,
    mediaList: List<MediaItemEntity>,
    albumsList: List<AlbumEntity>,
    getFileForMedia: (String) -> File,
    onTabSelected: (VaultTab) -> Unit,
    onCloseAlbum: () -> Unit,
    onMediaClick: (MediaItemEntity) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onToggleItemSelection: (Long) -> Unit,
    onSelectAll: (List<MediaItemEntity>) -> Unit,
    onClearSelection: () -> Unit,
    onLockVault: () -> Unit,
    onImportUris: (List<Uri>) -> Unit,
    onCameraCapture: (File) -> Unit,
    onMoveSelectedToTrash: () -> Unit,
    onExportSelectedToDevice: () -> Unit,
    onMoveSelectedToAlbum: (Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPopulateDecoyDummyMedia: () -> Unit = {}
) {
    val context = LocalContext.current
    var showFabMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showMoveAlbumDialog by remember { mutableStateOf(false) }

    // Android Photo Picker launcher (photos and videos)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportUris(uris)
        }
    }

    // Camera Capture launcher
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraFile?.let { file ->
                onCameraCapture(file)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectedIds.size,
                    totalCount = mediaList.size,
                    onSelectAll = { onSelectAll(mediaList) },
                    onCloseSelection = onClearSelection
                )
            } else {
                StandardTopBar(
                    title = uiState.selectedAlbum?.name ?: "Secret Vault",
                    isDecoy = uiState.isDecoyMode,
                    isAlbumOpen = uiState.selectedAlbum != null,
                    onBackClick = onCloseAlbum,
                    onSearchToggle = { showSearchBar = !showSearchBar },
                    onSelectionToggle = onToggleSelectionMode,
                    onPanicLock = onLockVault
                )
            }

            // Search Bar
            AnimatedVisibility(visible = showSearchBar && !uiState.isSelectionMode) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search photos and videos by name...", color = VaultTextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VaultCyan) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = VaultTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultCyan,
                        unfocusedBorderColor = VaultCardBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    )
                )
            }

            // Category Tab Row (hidden when browsing inside a specific album)
            if (uiState.selectedAlbum == null) {
                CategoryTabs(
                    activeTab = uiState.activeTab,
                    onTabSelected = onTabSelected
                )
            }

            // Main Media Content
            if (mediaList.isEmpty()) {
                EmptyVaultState(
                    activeTab = uiState.activeTab,
                    selectedAlbum = uiState.selectedAlbum,
                    isDecoy = uiState.isDecoyMode,
                    onPopulateDecoy = onPopulateDecoyDummyMedia,
                    onImportClick = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(
                        start = 4.dp,
                        end = 4.dp,
                        top = 4.dp,
                        bottom = if (uiState.isSelectionMode) 90.dp else 80.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(mediaList, key = { it.id }) { item ->
                        val isSelected = uiState.selectedIds.contains(item.id)
                        MediaGridThumbnail(
                            item = item,
                            file = getFileForMedia(item.filePath),
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    onToggleItemSelection(item.id)
                                } else {
                                    onMediaClick(item)
                                }
                            },
                            onLongClick = {
                                if (!uiState.isSelectionMode) {
                                    onToggleSelectionMode()
                                    onToggleItemSelection(item.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button (Import / Camera)
        if (!uiState.isSelectionMode && uiState.activeTab != VaultTab.SETTINGS && uiState.activeTab != VaultTab.TRASH) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = VaultCyan,
                    contentColor = Color(0xFF0B0F19),
                    shape = CircleShape,
                    modifier = Modifier.testTag("vault_fab")
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Add Media"
                    )
                }

                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false },
                    modifier = Modifier.background(VaultSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Import from Gallery", color = VaultTextPrimary, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = VaultCyan) },
                        onClick = {
                            showFabMenu = false
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Take Secret Photo", color = VaultTextPrimary, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = VaultIndigo) },
                        onClick = {
                            showFabMenu = false
                            try {
                                val cacheDir = context.cacheDir
                                val photoFile = File.createTempFile("vault_capture_", ".jpg", cacheDir)
                                tempCameraFile = photoFile
                                val photoUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                cameraLauncher.launch(photoUri)
                            } catch (e: Exception) {
                                // Handled safely
                            }
                        }
                    )

                    if (uiState.isDecoyMode) {
                        DropdownMenuItem(
                            text = { Text("Generate Dummy Photos", color = VaultTextPrimary, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = VaultAmber) },
                            onClick = {
                                showFabMenu = false
                                onPopulateDecoyDummyMedia()
                            }
                        )
                    }
                }
            }
        }

        // Bottom Selection Actions Bar
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VaultSurface)
                    .border(1.dp, VaultCardBorder)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasSelection = uiState.selectedIds.isNotEmpty()

                BottomBarAction(
                    icon = Icons.Default.Download,
                    label = "Unhide",
                    enabled = hasSelection,
                    onClick = onExportSelectedToDevice
                )

                BottomBarAction(
                    icon = Icons.Default.DriveFileMove,
                    label = "Album",
                    enabled = hasSelection,
                    onClick = { showMoveAlbumDialog = true }
                )

                BottomBarAction(
                    icon = Icons.Default.Delete,
                    label = "Trash",
                    tint = VaultRose,
                    enabled = hasSelection,
                    onClick = onMoveSelectedToTrash
                )
            }
        }

        if (showMoveAlbumDialog) {
            MoveToAlbumDialog(
                albums = albumsList,
                currentAlbumId = uiState.selectedAlbum?.id,
                onDismiss = { showMoveAlbumDialog = false },
                onSelectAlbum = { albumId ->
                    onMoveSelectedToAlbum(albumId)
                    showMoveAlbumDialog = false
                }
            )
        }
    }
}

@Composable
fun StandardTopBar(
    title: String,
    isDecoy: Boolean,
    isAlbumOpen: Boolean,
    onBackClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onSelectionToggle: () -> Unit,
    onPanicLock: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (isAlbumOpen) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VaultTextPrimary
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VaultCard)
                        .border(1.dp, VaultCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = VaultCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                    if (isDecoy) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(VaultAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("DECOY", color = VaultAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = "Encrypted Local Storage",
                    fontSize = 11.sp,
                    color = VaultTextSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSearchToggle) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = VaultTextPrimary)
            }

            IconButton(onClick = onSelectionToggle) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Select", tint = VaultTextPrimary)
            }

            // Panic Lock Button
            IconButton(
                onClick = onPanicLock,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(VaultRose.copy(alpha = 0.15f))
                    .testTag("panic_lock_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Instant Lock",
                    tint = VaultRose,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onCloseSelection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VaultSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCloseSelection) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = VaultTextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$selectedCount Selected",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
        }

        TextButton(onClick = onSelectAll) {
            Text("Select All ($totalCount)", color = VaultCyan, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun CategoryTabs(
    activeTab: VaultTab,
    onTabSelected: (VaultTab) -> Unit
) {
    val tabs = listOf(
        Pair(VaultTab.ALL, "All"),
        Pair(VaultTab.PHOTOS, "Photos"),
        Pair(VaultTab.VIDEOS, "Videos"),
        Pair(VaultTab.ALBUMS, "Albums"),
        Pair(VaultTab.FAVORITES, "Favorites"),
        Pair(VaultTab.TRASH, "Trash"),
        Pair(VaultTab.SETTINGS, "Settings")
    )

    ScrollableTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0),
        containerColor = VaultDarkBg,
        contentColor = VaultCyan,
        edgePadding = 12.dp,
        divider = {},
        indicator = { tabPositions ->
            val idx = tabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0)
            if (idx < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                    color = VaultCyan,
                    height = 2.dp
                )
            }
        }
    ) {
        tabs.forEach { (tab, title) ->
            val isSelected = activeTab == tab
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) VaultCyan else VaultTextSecondary
                    )
                }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaGridThumbnail(
    item: MediaItemEntity,
    file: File,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(VaultCard)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(file)
                .crossfade(true)
                .build(),
            contentDescription = item.originalName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay at bottom for badges
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )

        // Video Duration badge
        if (item.mediaType == "VIDEO") {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                if (item.durationMs > 0) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = formatDuration(item.durationMs),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Favorite heart badge
        if (item.isFavorite) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = VaultRose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }

        // Selection Checkmark
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) VaultCyan.copy(alpha = 0.35f) else Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) VaultCyan else Color.Black.copy(alpha = 0.5f))
                        .border(1.5.dp, if (isSelected) VaultCyan else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF0B0F19),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val sec = ms / 1000
    val m = sec / 60
    val s = sec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

@Composable
fun EmptyVaultState(
    activeTab: VaultTab,
    selectedAlbum: AlbumEntity?,
    isDecoy: Boolean = false,
    onPopulateDecoy: () -> Unit = {},
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(VaultCard)
                .border(1.dp, if (isDecoy) VaultAmber.copy(alpha = 0.5f) else VaultCardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isDecoy -> Icons.Default.Shield
                    activeTab == VaultTab.VIDEOS -> Icons.Default.Videocam
                    activeTab == VaultTab.FAVORITES -> Icons.Default.Favorite
                    else -> Icons.Default.PhotoLibrary
                },
                contentDescription = null,
                tint = if (isDecoy) VaultAmber else VaultCyan,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when {
                isDecoy -> "Decoy Vault is Empty"
                selectedAlbum != null -> "Album '${selectedAlbum.name}' is Empty"
                activeTab == VaultTab.FAVORITES -> "No Favorite Media"
                activeTab == VaultTab.VIDEOS -> "No Secret Videos"
                activeTab == VaultTab.PHOTOS -> "No Secret Photos"
                else -> "Your Private Vault is Empty"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = VaultTextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = when {
                isDecoy -> "To mislead anyone forcing you to unlock your phone, populate this decoy vault with harmless dummy photos (recipes, dog pictures, vacation notes)."
                activeTab == VaultTab.FAVORITES -> "Tap the heart icon on any photo or video to add it to your favorites."
                else -> "Import photos & videos from your phone. They will be stored in isolated private storage, completely hidden from device gallery apps."
            },
            fontSize = 13.sp,
            color = VaultTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDecoy) {
                Button(
                    onClick = onPopulateDecoy,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Dummy Photos", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
                }
            }

            if (activeTab != VaultTab.FAVORITES) {
                Button(
                    onClick = onImportClick,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0B0F19))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Media", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BottomBarAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    tint: Color = VaultCyan,
    onClick: () -> Unit
) {
    val finalTint = if (enabled) tint else VaultTextMuted

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = finalTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = finalTint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
