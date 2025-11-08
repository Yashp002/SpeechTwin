package com.runanywhere.startup_hackathon20

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlue
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlueLight
import kotlinx.coroutines.delay

@Composable
fun CustomMediaPlayerControls(
    mediaPlayer: MediaPlayer?,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpeedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress bar and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatTime(currentPosition),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.width(40.dp)
                )

                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { progress ->
                        val newPosition = (progress * duration).toInt()
                        onSeek(newPosition)
                    },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = VocalBlue,
                        activeTrackColor = VocalBlue,
                        inactiveTrackColor = VocalBlueLight.copy(alpha = 0.3f)
                    )
                )

                Text(
                    text = formatTime(duration),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.width(40.dp)
                )
            }

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed control button
                Box {
                    OutlinedButton(
                        onClick = { showSpeedMenu = true },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = VocalBlue
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "${playbackSpeed}×",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${speed}×",
                                        color = if (speed == playbackSpeed) VocalBlue else Color.Unspecified
                                    )
                                },
                                onClick = {
                                    onSpeedChange(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Play/Pause button
                FloatingActionButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(56.dp),
                    containerColor = VocalBlue,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(72.dp)) // Balance the layout
            }
        }
    }
}

@Composable
fun ExpandedPlayerSheet(
    recording: RecordingItem,
    mediaPlayer: MediaPlayer?,
    isPlaying: Boolean,
    currentPosition: Int,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Now Playing",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VocalBlue
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray
                )
            }
        }

        // Recording info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Waveform thumbnail
            WaveformThumbnail(
                waveformData = recording.waveformData,
                modifier = Modifier.size(80.dp, 60.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = recording.formattedTimestamp,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${recording.formattedDuration} • ${recording.formattedSize}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Health score badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        recording.healthScoreColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = recording.vocalHealthScore.toString(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Media controls
        CustomMediaPlayerControls(
            mediaPlayer = mediaPlayer,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = recording.duration * 1000, // Convert to milliseconds
            playbackSpeed = playbackSpeed,
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onSpeedChange = onSpeedChange
        )
    }
}

private fun formatTime(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

// Extension function to set playback speed (Android 6.0+)
fun MediaPlayer.setPlaybackSpeed(speed: Float) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            playbackParams = playbackParams.setSpeed(speed)
        } catch (e: Exception) {
            // Fallback for unsupported devices
        }
    }
}