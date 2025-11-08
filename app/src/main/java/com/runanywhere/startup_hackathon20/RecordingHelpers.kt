package com.runanywhere.startup_hackathon20

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlue
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlueLight
import java.io.File
import java.util.*

// Helper function to load recordings from cache
fun loadRecordingsFromCache(context: Context): List<RecordingItem> {
    return try {
        val cacheDir = context.cacheDir
        val audioFiles = cacheDir.listFiles { file ->
            file.name.startsWith("recorded_audio_") && file.name.endsWith(".wav")
        }?.sortedByDescending { it.lastModified() }

        val recordings = mutableListOf<RecordingItem>()
        if (audioFiles != null) {
            for (i in audioFiles.indices) {
                val file = audioFiles[i]
                val duration = (file.length() / (44100 * 2)).toInt() // Estimate duration
                recordings.add(
                    RecordingItem(
                        id = file.absolutePath,
                        file = file,
                        name = "Recording ${i + 1}",
                        originalName = file.name,
                        timestamp = file.lastModified(),
                        duration = duration,
                        sizeBytes = file.length(),
                        waveformData = generateMockWaveformData(duration)
                    )
                )
            }
        }
        recordings
    } catch (e: Exception) {
        emptyList()
    }
}

// Date helper functions
fun isThisWeek(timestamp: Long): Boolean {
    val calendar = Calendar.getInstance()
    val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
    calendar.timeInMillis = timestamp
    val recordingWeek = calendar.get(Calendar.WEEK_OF_YEAR)
    return currentWeek == recordingWeek
}

fun isThisMonth(timestamp: Long): Boolean {
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    calendar.timeInMillis = timestamp
    val recordingMonth = calendar.get(Calendar.MONTH)
    return currentMonth == recordingMonth
}

// Media player handler
fun handlePlayPause(
    recording: RecordingItem,
    context: Context,
    callback: (MediaPlayer?, Boolean) -> Unit
) {
    try {
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(recording.file.absolutePath)
            prepare()
            start()
        }
        callback(mediaPlayer, true)
    } catch (e: Exception) {
        Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
        callback(null, false)
    }
}

// Share recording function
fun shareRecording(recording: RecordingItem, context: Context) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_SUBJECT, recording.name)
            putExtra(Intent.EXTRA_TEXT, "Sharing recording: ${recording.name}")
            // Note: In a real app, you'd need to use FileProvider for file sharing
        }
        context.startActivity(Intent.createChooser(intent, "Share Recording"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing recording", Toast.LENGTH_SHORT).show()
    }
}

// Empty state view
@Composable
fun EmptyStateView(
    message: String,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.List,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 18.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onClearFilters) {
            Text("Clear Filters", color = VocalBlue)
        }
    }
}

// Recording card component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingCard(
    recording: RecordingItem,
    isCompareModeActive: Boolean,
    isSelected: Boolean,
    isPlaying: Boolean,
    isExpanded: Boolean,
    playbackProgress: Int,
    playbackSpeed: Float,
    mediaPlayer: MediaPlayer?,
    onTap: () -> Unit,
    onLongPress: (String) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onExpandPlayer: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    // Determine the actual playback state
    val isActuallyPlaying = isPlaying && mediaPlayer?.isPlaying == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // Handle swipe gestures
                    }
                ) { _, dragAmount ->
                    if (dragAmount > 100) {
                        onSwipeRight()
                    } else if (dragAmount < -100) {
                        onSwipeLeft()
                    }
                }
            }
            .clickable { onTap() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> VocalBlue.copy(alpha = 0.1f)
                isPlaying -> VocalBlueLight.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection checkbox or waveform thumbnail
                if (isCompareModeActive) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onTap() },
                        colors = CheckboxDefaults.colors(checkedColor = VocalBlue)
                    )
                } else {
                    WaveformThumbnail(
                        waveformData = recording.waveformData,
                        modifier = Modifier.size(60.dp, 40.dp)
                    )
                }

                // Recording info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = recording.formattedTimestamp,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${recording.formattedDuration} • ${recording.formattedSize}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Health score badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            recording.healthScoreColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = recording.vocalHealthScore.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // More menu
                if (!isCompareModeActive) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play/Pause button
                        IconButton(
                            onClick = onTap,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isActuallyPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isActuallyPlaying) "Pause" else if (isPlaying) "Resume" else "Play",
                                tint = VocalBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Menu button
                        Box {
                            IconButton(
                                onClick = {
                                    showMenu = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            ) {
                                Icon(Icons.Default.MoreVert, "More")
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = {
                                        onLongPress("rename")
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    leadingIcon = { Icon(Icons.Default.Share, null) },
                                    onClick = {
                                        onLongPress("share")
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Analyze") },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    onClick = {
                                        onLongPress("analyze")
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (recording.isBaseline) "Remove Baseline" else "Set as Baseline") },
                                    leadingIcon = { Icon(Icons.Default.Star, null) },
                                    onClick = {
                                        onLongPress("baseline")
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                    onClick = {
                                        onLongPress("delete")
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Playback progress
            if (isPlaying && playbackProgress > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { playbackProgress / 1000f }, // Assuming progress is in milliseconds
                    modifier = Modifier.fillMaxWidth(),
                    color = VocalBlue,
                    trackColor = VocalBlueLight.copy(alpha = 0.3f)
                )
            }

            // Expanded player controls
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                CustomMediaPlayerControls(
                    mediaPlayer = mediaPlayer,
                    isPlaying = isPlaying,
                    currentPosition = playbackProgress,
                    duration = recording.duration * 1000,
                    playbackSpeed = playbackSpeed,
                    onPlayPause = onTap,
                    onSeek = { /* Handle seek */ },
                    onSpeedChange = { /* Handle speed change */ }
                )
            }
        }
    }
}

// Sort dropdown menu
@Composable
fun SortDropdownMenu(
    currentOption: SortOption,
    onOptionSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        SortOption.values().forEach { option ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = option.displayName,
                        color = if (option == currentOption) VocalBlue else Color.Unspecified
                    )
                },
                onClick = {
                    onOptionSelected(option)
                    onDismiss()
                }
            )
        }
    }
}

// Filter dropdown menu
@Composable
fun FilterDropdownMenu(
    currentOption: FilterOption,
    onOptionSelected: (FilterOption) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        FilterOption.values().forEach { option ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = option.displayName,
                        color = if (option == currentOption) VocalBlue else Color.Unspecified
                    )
                },
                onClick = {
                    onOptionSelected(option)
                    onDismiss()
                }
            )
        }
    }
}

// Storage info footer
@Composable
fun StorageInfoFooter(
    recordingCount: Int,
    totalSize: Long,
    onClearOld: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = VocalBlueLight.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$recordingCount recordings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Using ${String.format("%.2f MB", totalSize / (1024.0 * 1024.0))}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            TextButton(onClick = onClearOld) {
                Text("Clear Old", color = VocalBlue)
            }
        }
    }
}

// Comparison dialog
@Composable
fun ComparisonDialog(
    recording1: RecordingItem,
    recording2: RecordingItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recording Comparison") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ComparisonRow("Recording 1", recording1.name, recording2.name)
                ComparisonRow(
                    "Health Score",
                    recording1.vocalHealthScore.toString(),
                    recording2.vocalHealthScore.toString()
                )
                ComparisonRow(
                    "Duration",
                    recording1.formattedDuration,
                    recording2.formattedDuration
                )
                ComparisonRow("Date", recording1.formattedTimestamp, recording2.formattedTimestamp)
                ComparisonRow("Size", recording1.formattedSize, recording2.formattedSize)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ComparisonRow(label: String, value1: String, value2: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = VocalBlue
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = value1, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(text = "vs", fontSize = 12.sp, color = Color.Gray)
            Text(
                text = value2,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}

// Smart suggestions card
@Composable
fun SmartSuggestionsCard(
    recordings: List<RecordingItem>,
    modifier: Modifier = Modifier
) {
    if (recordings.isNotEmpty()) {
        val bestRecording = recordings.maxByOrNull { it.vocalHealthScore }
        bestRecording?.let { best ->
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = VocalBlue.copy(alpha = 0.05f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = VocalBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Smart Suggestion",
                            fontWeight = FontWeight.Bold,
                            color = VocalBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your best recording was \"${best.name}\" with a score of ${best.vocalHealthScore}. What did you do differently that day?",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}