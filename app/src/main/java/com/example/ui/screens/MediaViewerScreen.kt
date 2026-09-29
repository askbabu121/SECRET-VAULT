package com.example.ui.screens

import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AlbumEntity
import com.example.data.model.MediaItemEntity
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultIndigo
import com.example.ui.theme.VaultRose
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaViewerScreen(
    item: MediaItemEntity,
    internalFile: File,
    albums: List<AlbumEntity>,
    onClose: () -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onExportToGallery: (MediaItemEntity) -> Unit,
    onMoveToTrash: (MediaItemEntity) -> Unit,
    onMoveToAlbum: (albumId: Long?) -> Unit,
    onUpdateNotes: (id: Long, notes: String) -> Unit
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showAlbumDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (item.mediaType == "VIDEO") {
            VideoPlayerView(
                videoFile = internalFile,
                showControls = showControls,
                onToggleControls = { showControls = !showControls }
            )
        } else {
            PhotoZoomView(
                photoFile = internalFile,
                onTap = { showControls = !showControls }
            )
        }

        // Top Overlay Bar
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("viewer_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = item.originalName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())
                            .format(Date(item.dateAdded)),
                        color = VaultTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onToggleFavorite(item) }) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) VaultRose else Color.White
                        )
                    }

                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Overlay Bar
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewerActionButton(
                    icon = Icons.Default.Download,
                    label = "Unhide",
                    onClick = { onExportToGallery(item) }
                )

                ViewerActionButton(
                    icon = Icons.Default.DriveFileMove,
                    label = "Album",
                    onClick = { showAlbumDialog = true }
                )

                ViewerActionButton(
                    icon = Icons.Default.EditNote,
                    label = "Notes",
                    onClick = { showNotesDialog = true }
                )

                ViewerActionButton(
                    icon = Icons.Default.Delete,
                    label = "Trash",
                    tint = VaultRose,
                    onClick = { showDeleteConfirm = true }
                )
            }
        }

        // Dialogs
        if (showInfoDialog) {
            MediaInfoDialog(
                item = item,
                onDismiss = { showInfoDialog = false }
            )
        }

        if (showNotesDialog) {
            MediaNotesDialog(
                currentNotes = item.notes,
                onDismiss = { showNotesDialog = false },
                onSaveNotes = { notes ->
                    onUpdateNotes(item.id, notes)
                    showNotesDialog = false
                }
            )
        }

        if (showAlbumDialog) {
            MoveToAlbumDialog(
                albums = albums,
                currentAlbumId = item.albumId,
                onDismiss = { showAlbumDialog = false },
                onSelectAlbum = { albumId ->
                    onMoveToAlbum(albumId)
                    showAlbumDialog = false
                }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = VaultSurface,
                title = { Text("Move to Trash?", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "This item will be moved to the Vault Trash. You can restore it later or delete permanently to free up space.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            onMoveToTrash(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultRose)
                    ) {
                        Text("Move to Trash", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = VaultTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun PhotoZoomView(
    photoFile: File,
    onTap: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    if (newScale > 1f) {
                        offset = Offset(offset.x + pan.x, offset.y + pan.y)
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoFile)
                .crossfade(true)
                .build(),
            contentDescription = "Private Photo",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun VideoPlayerView(
    videoFile: File,
    showControls: Boolean,
    onToggleControls: () -> Unit
) {
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    // Position polling
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoViewInstance?.let { vv ->
                currentPosMs = vv.currentPosition.toLong()
                val dur = vv.duration.toLong()
                if (dur > 0) durationMs = dur
            }
            delay(250)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onToggleControls() },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setVideoPath(videoFile.absolutePath)
                    setOnPreparedListener { mp ->
                        durationMs = mp.duration.toLong()
                        videoViewInstance = this
                        start()
                        isPlaying = true
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        currentPosMs = durationMs
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center Play/Pause button overlay
        AnimatedVisibility(visible = showControls) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable {
                        videoViewInstance?.let { vv ->
                            if (vv.isPlaying) {
                                vv.pause()
                                isPlaying = false
                            } else {
                                if (currentPosMs >= durationMs && durationMs > 0) {
                                    vv.seekTo(0)
                                }
                                vv.start()
                                isPlaying = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // Bottom progress bar
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 70.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosMs),
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatTime(durationMs),
                        color = VaultTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Slider(
                    value = if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                    onValueChange = { frac ->
                        val target = (frac * durationMs).toInt()
                        videoViewInstance?.seekTo(target)
                        currentPosMs = target.toLong()
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = VaultCyan,
                        activeTrackColor = VaultCyan,
                        inactiveTrackColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
}

@Composable
fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = tint, fontSize = 11.sp)
    }
}

@Composable
fun MediaInfoDialog(
    item: MediaItemEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = { Text("Media Details", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("File Name", item.originalName)
                InfoRow("Media Type", item.mediaType)
                InfoRow("MIME Type", item.mimeType)
                InfoRow("File Size", formatFileSize(item.fileSizeBytes))
                InfoRow(
                    "Date Secured",
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(item.dateAdded))
                )
                if (item.notes.isNotBlank()) {
                    InfoRow("Notes", item.notes)
                }
                InfoRow("Storage Location", "Private Sandboxed Storage (Encrypted / Invisible to Gallery)")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = VaultCyan)
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Column {
        Text(text = label, color = VaultTextSecondary, fontSize = 11.sp)
        Text(text = value, color = VaultTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.getDefault(), "%.2f MB", mb)
}

@Composable
fun MediaNotesDialog(
    currentNotes: String,
    onDismiss: () -> Unit,
    onSaveNotes: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = { Text("Private Memo / Notes", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Secret notes about this photo/video") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VaultCyan,
                    unfocusedBorderColor = VaultCardBorder,
                    focusedTextColor = VaultTextPrimary,
                    unfocusedTextColor = VaultTextPrimary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onSaveNotes(text) },
                colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
            ) {
                Text("Save Notes", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
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
fun MoveToAlbumDialog(
    albums: List<AlbumEntity>,
    currentAlbumId: Long?,
    onDismiss: () -> Unit,
    onSelectAlbum: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = { Text("Move to Album", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = if (currentAlbumId == null) VaultCyan.copy(alpha = 0.2f) else VaultCard,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectAlbum(null) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text("No Album (Uncategorized)", color = VaultTextPrimary, fontSize = 14.sp)
                }

                albums.forEach { album ->
                    Surface(
                        color = if (currentAlbumId == album.id) VaultCyan.copy(alpha = 0.2f) else VaultCard,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAlbum(album.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(album.name, color = VaultTextPrimary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextSecondary)
            }
        }
    )
}
