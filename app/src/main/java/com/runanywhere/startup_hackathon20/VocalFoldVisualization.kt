package com.runanywhere.startup_hackathon20

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.runanywhere.startup_hackathon20.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * 3D Waveform Visualization - MATLAB-Style Voice Signal in 3D Space
 * Shows voice audio as an animated 3D sine/cosine waveform
 */

data class VocalFoldMetrics(
    val healthScore: Float,
    val pitch: Float,
    val loudness: Float,
    val jitter: Float,
    val shimmer: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocalFoldVisualizationScreen(
    metrics: VocalFoldMetrics,
    recordingName: String = "Current Analysis",
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentMetrics by remember { mutableStateOf(metrics) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var showAnnotations by remember { mutableStateOf(true) }
    var rotationX by remember { mutableStateOf(30f) }
    var rotationY by remember { mutableStateOf(45f) }
    var zoom by remember { mutableStateOf(1f) }
    
    // Recording state
    var isRecording by remember { mutableStateOf(false) }
    var recordingProgress by remember { mutableStateOf(0f) }
    var countdown by remember { mutableStateOf(10) }
    var permissionGranted by remember { mutableStateOf(false) }
    var audioRecorder by remember { mutableStateOf<AudioRecorder?>(null) }
    var recordedFile by remember { mutableStateOf<java.io.File?>(null) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    
    // Check permission
    LaunchedEffect(Unit) {
        permissionGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (!granted) {
            android.widget.Toast.makeText(
                context,
                "Microphone permission required",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Handle recording lifecycle
    LaunchedEffect(isRecording) {
        if (isRecording && audioRecorder == null) {
            // Start recording
            val recorder = AudioRecorder()
            audioRecorder = recorder

            val success = recorder.startRecording()
            if (!success) {
                isRecording = false
                audioRecorder = null
                android.widget.Toast.makeText(
                    context,
                    "Failed to start recording",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } else if (!isRecording && audioRecorder != null) {
            // Stop recording and analyze
            audioRecorder?.stopRecording()

            try {
                val cacheDir = context.cacheDir
                val audioFile = java.io.File(cacheDir, "waveform_${System.currentTimeMillis()}.wav")
                val saved = audioRecorder?.saveToWavFile(audioFile) ?: false

                if (saved && audioFile.exists()) {
                    recordedFile = audioFile

                    // Analyze the recording
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val analyzer = VoiceAnalyzer()
                            val result = analyzer.analyze(audioFile.absolutePath)

                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                // Update metrics with new analysis
                                currentMetrics = VocalFoldMetrics(
                                    healthScore = result.healthScore.toFloat(),
                                    pitch = result.pitch.toFloat(),
                                    loudness = result.loudness.toFloat(),
                                    jitter = result.jitter.toFloat(),
                                    shimmer = result.shimmer.toFloat()
                                )

                                android.widget.Toast.makeText(
                                    context,
                                    "✓ Analysis complete! Updated waveform",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: Exception) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Analysis failed: ${e.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    "Error saving recording",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

            audioRecorder = null
            countdown = 10
            recordingProgress = 0f
        }
    }

    // Recording countdown
    LaunchedEffect(isRecording) {
        if (isRecording) {
            for (i in 10 downTo 0) {
                if (!isRecording) break
                countdown = i
                recordingProgress = (10 - i) / 10f

                if (i == 0) {
                    isRecording = false
                    break
                }
                delay(1000)
            }
        }
    }

    // Cleanup on dispose
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                audioRecorder?.stopRecording()
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("3D Voice Waveform", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (isRecording) "Recording... ${countdown}s" else "MATLAB-Style Visualization",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRecording) ErrorRed else TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = TextOnPrimary,
                    navigationIconContentColor = TextOnPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0E14),
                            Color(0xFF1A1A2E)
                        )
                    )
                )
        ) {
            // Main 3D Waveform Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0D1117).copy(
                        alpha = if (isRecording) 0.7f else 1f
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 3D Waveform Visualization
                    Waveform3DCanvas(
                        metrics = currentMetrics,
                        isPlaying = isPlaying && !isRecording,
                        playbackSpeed = playbackSpeed,
                        rotationX = rotationX,
                        rotationY = rotationY,
                        zoom = zoom,
                        showAnnotations = showAnnotations,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    rotationY += dragAmount.x * 0.5f
                                    rotationX -= dragAmount.y * 0.5f
                                    rotationX = rotationX.coerceIn(-90f, 90f)
                                }
                            }
                    )

                    // Recording indicator
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    progress = { recordingProgress },
                                    modifier = Modifier.size(120.dp),
                                    strokeWidth = 12.dp,
                                    color = ErrorRed,
                                    trackColor = Color.White.copy(alpha = 0.3f)
                                )
                                androidx.compose.foundation.layout.Spacer(
                                    modifier = Modifier.height(
                                        16.dp
                                    )
                                )
                                Text(
                                    "${countdown}s",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Recording...",
                                    fontSize = 18.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Watermark
                    Text(
                        "SpeechTwin 3D",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.3f),
                        fontWeight = FontWeight.Light
                    )
                }
            }

            // Metrics Panel
            MetricsPanel(
                metrics = currentMetrics,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Control Panel
            ControlPanel(
                isPlaying = isPlaying,
                isPlayingAudio = isPlayingAudio,
                playbackSpeed = playbackSpeed,
                showAnnotations = showAnnotations,
                hasRecordedAudio = recordedFile != null,
                onPlayPauseClick = {
                    if (recordedFile != null) {
                        // Play/pause recorded audio
                        if (isPlayingAudio) {
                            mediaPlayer?.pause()
                            isPlayingAudio = false
                        } else {
                            try {
                                if (mediaPlayer == null) {
                                    mediaPlayer = android.media.MediaPlayer().apply {
                                        setDataSource(recordedFile?.absolutePath)
                                        setOnCompletionListener {
                                            isPlayingAudio = false
                                        }
                                        prepare()
                                    }
                                }
                                mediaPlayer?.start()
                                isPlayingAudio = true
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Playback error",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        // Just toggle animation
                        isPlaying = !isPlaying
                    }
                },
                onSpeedChange = { playbackSpeed = it },
                onAnnotationsToggle = { showAnnotations = !showAnnotations },
                onResetView = { 
                    rotationX = 30f
                    rotationY = 45f
                    zoom = 1f
                },
                onZoomIn = { zoom = (zoom * 1.2f).coerceAtMost(3f) },
                onZoomOut = { zoom = (zoom / 1.2f).coerceAtLeast(0.5f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Recording Button at Bottom
            RecordingButton(
                isRecording = isRecording,
                recordingProgress = recordingProgress,
                countdown = countdown,
                permissionGranted = permissionGranted,
                onRecordClick = {
                    if (permissionGranted) {
                        isRecording = !isRecording
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun Waveform3DCanvas(
    metrics: VocalFoldMetrics,
    isPlaying: Boolean,
    playbackSpeed: Float,
    rotationX: Float,
    rotationY: Float,
    zoom: Float,
    showAnnotations: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    // Time progress for animation
    val timeProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (3000f / playbackSpeed).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val baseRadius = size.minDimension * 0.35f * zoom
        
        // Draw 3D grid background
        draw3DGrid(centerX, centerY, baseRadius, rotationX, rotationY)
        
        // Draw 3D waveform
        draw3DWaveform(
            centerX = centerX,
            centerY = centerY,
            radius = baseRadius,
            metrics = metrics,
            timeProgress = if (isPlaying) timeProgress else 0f,
            rotationX = rotationX,
            rotationY = rotationY
        )
        
        // Draw axes
        if (showAnnotations) {
            draw3DAxes(centerX, centerY, baseRadius, rotationX, rotationY)
        }
    }
}

private fun DrawScope.draw3DGrid(
    centerX: Float,
    centerY: Float,
    radius: Float,
    rotX: Float,
    rotY: Float
) {
    // Draw floor grid
    val gridSize = 10
    val gridSpacing = radius * 2f / gridSize
    
    for (i in -gridSize..gridSize) {
        val x1 = i * gridSpacing
        val z1 = -radius
        val z2 = radius
        
        val (x1_2d, y1_2d) = project3D(x1, -radius, z1, centerX, centerY, rotX, rotY)
        val (x2_2d, y2_2d) = project3D(x1, -radius, z2, centerX, centerY, rotX, rotY)
        
        drawLine(
            color = Color(0xFF2E3440).copy(alpha = 0.3f),
            start = Offset(x1_2d, y1_2d),
            end = Offset(x2_2d, y2_2d),
            strokeWidth = 1f
        )
    }
    
    for (i in -gridSize..gridSize) {
        val z1 = i * gridSpacing
        val x1 = -radius
        val x2 = radius
        
        val (x1_2d, y1_2d) = project3D(x1, -radius, z1, centerX, centerY, rotX, rotY)
        val (x2_2d, y2_2d) = project3D(x2, -radius, z1, centerX, centerY, rotX, rotY)
        
        drawLine(
            color = Color(0xFF2E3440).copy(alpha = 0.3f),
            start = Offset(x1_2d, y1_2d),
            end = Offset(x2_2d, y2_2d),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.draw3DWaveform(
    centerX: Float,
    centerY: Float,
    radius: Float,
    metrics: VocalFoldMetrics,
    timeProgress: Float,
    rotationX: Float,
    rotationY: Float
) {
    val points = 200
    val path = Path()
    var firstPoint = true
    
    // Generate waveform points
    val waveformPoints = mutableListOf<Triple<Float, Float, Float>>()
    
    for (i in 0 until points) {
        val t = (i.toFloat() / points) * 4f * PI.toFloat()
        
        // Create complex waveform based on voice metrics
        val frequency = metrics.pitch / 50f
        val amplitude = metrics.loudness / 100f * radius * 0.5f
        
        // X: time axis
        val x = (i.toFloat() / points - 0.5f) * radius * 2f
        
        // Y: primary sine wave (pitch)
        val y = sin(t * frequency + timeProgress) * amplitude
        
        // Z: cosine component (creates 3D spiral effect)
        val z = cos(t * frequency * 0.7f + timeProgress) * amplitude * 0.7f
        
        // Add jitter effect
        val jitterNoise = if (metrics.jitter > 2f) {
            sin(t * 17f) * (metrics.jitter * 0.05f) * amplitude
        } else 0f
        
        waveformPoints.add(Triple(x, y + jitterNoise, z))
    }
    
    // Draw waveform with gradient color
    for (i in 0 until waveformPoints.size - 1) {
        val (x1, y1, z1) = waveformPoints[i]
        val (x2, y2, z2) = waveformPoints[i + 1]
        
        val (x1_2d, y1_2d) = project3D(x1, y1, z1, centerX, centerY, rotationX, rotationY)
        val (x2_2d, y2_2d) = project3D(x2, y2, z2, centerX, centerY, rotationX, rotationY)
        
        // Color based on position and health
        val colorProgress = i.toFloat() / points
        val healthColor = when {
            metrics.healthScore >= 80 -> Color(0xFF4CAF50)
            metrics.healthScore >= 60 -> Color(0xFFFFC107)
            else -> Color(0xFFF44336)
        }
        
        val waveColor = Color(
            red = lerp(AccentTeal.red, healthColor.red, colorProgress),
            green = lerp(AccentTeal.green, healthColor.green, colorProgress),
            blue = lerp(AccentTeal.blue, healthColor.blue, colorProgress)
        )
        
        // Draw line segment with glow
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(waveColor, waveColor.copy(alpha = 0.6f)),
                start = Offset(x1_2d, y1_2d),
                end = Offset(x2_2d, y2_2d)
            ),
            start = Offset(x1_2d, y1_2d),
            end = Offset(x2_2d, y2_2d),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        
        // Draw glow effect
        drawLine(
            color = waveColor.copy(alpha = 0.2f),
            start = Offset(x1_2d, y1_2d),
            end = Offset(x2_2d, y2_2d),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
    }
    
    // Draw data points at intervals
    for (i in waveformPoints.indices step 10) {
        val (x, y, z) = waveformPoints[i]
        val (x_2d, y_2d) = project3D(x, y, z, centerX, centerY, rotationX, rotationY)
        
        drawCircle(
            color = AccentTeal,
            radius = 4f,
            center = Offset(x_2d, y_2d)
        )
    }
}

private fun DrawScope.draw3DAxes(
    centerX: Float,
    centerY: Float,
    radius: Float,
    rotX: Float,
    rotY: Float
) {
    // X-axis (Red)
    val (x1, y1) = project3D(-radius, 0f, 0f, centerX, centerY, rotX, rotY)
    val (x2, y2) = project3D(radius, 0f, 0f, centerX, centerY, rotX, rotY)
    drawLine(
        color = Color.Red.copy(alpha = 0.7f),
        start = Offset(x1, y1),
        end = Offset(x2, y2),
        strokeWidth = 3f
    )
    
    // Y-axis (Green)
    val (x3, y3) = project3D(0f, -radius, 0f, centerX, centerY, rotX, rotY)
    val (x4, y4) = project3D(0f, radius, 0f, centerX, centerY, rotX, rotY)
    drawLine(
        color = Color.Green.copy(alpha = 0.7f),
        start = Offset(x3, y3),
        end = Offset(x4, y4),
        strokeWidth = 3f
    )
    
    // Z-axis (Blue)
    val (x5, y5) = project3D(0f, 0f, -radius, centerX, centerY, rotX, rotY)
    val (x6, y6) = project3D(0f, 0f, radius, centerX, centerY, rotX, rotY)
    drawLine(
        color = Color.Blue.copy(alpha = 0.7f),
        start = Offset(x5, y5),
        end = Offset(x6, y6),
        strokeWidth = 3f
    )
}

// 3D projection helper
private fun project3D(
    x: Float,
    y: Float,
    z: Float,
    centerX: Float,
    centerY: Float,
    rotX: Float,
    rotY: Float
): Pair<Float, Float> {
    // Convert rotation to radians
    val rotXRad = rotX * PI.toFloat() / 180f
    val rotYRad = rotY * PI.toFloat() / 180f
    
    // Rotate around X axis
    val y1 = y * cos(rotXRad) - z * sin(rotXRad)
    val z1 = y * sin(rotXRad) + z * cos(rotXRad)
    
    // Rotate around Y axis
    val x2 = x * cos(rotYRad) + z1 * sin(rotYRad)
    val z2 = -x * sin(rotYRad) + z1 * cos(rotYRad)
    
    // Perspective projection
    val perspective = 1f / (1f + z2 / 1000f)
    
    return Pair(
        centerX + x2 * perspective,
        centerY + y1 * perspective
    )
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

@Composable
fun MetricsPanel(
    metrics: VocalFoldMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetricItem("Pitch", "${metrics.pitch.toInt()} Hz", PitchPurple)
            MetricItem("Loudness", "${metrics.loudness.toInt()} dB", LoudnessBlue)
            MetricItem("Health", "${metrics.healthScore.toInt()}%", when {
                metrics.healthScore >= 80 -> SuccessGreen
                metrics.healthScore >= 60 -> WarningOrange
                else -> ErrorRed
            })
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
fun ControlPanel(
    isPlaying: Boolean,
    isPlayingAudio: Boolean,
    playbackSpeed: Float,
    showAnnotations: Boolean,
    hasRecordedAudio: Boolean,
    onPlayPauseClick: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onAnnotationsToggle: () -> Unit,
    onResetView: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(AccentTeal, CircleShape)
            ) {
                Icon(
                    if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlayingAudio) "Pause" else "Play",
                    tint = Color.White
                )
            }
            
            // Speed Control
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Speed", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Row {
                    TextButton(
                        onClick = { onSpeedChange(0.5f) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (playbackSpeed == 0.5f) AccentTeal else TextSecondary
                        )
                    ) {
                        Text("0.5x", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { onSpeedChange(1f) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (playbackSpeed == 1f) AccentTeal else TextSecondary
                        )
                    ) {
                        Text("1x", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { onSpeedChange(2f) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (playbackSpeed == 2f) AccentTeal else TextSecondary
                        )
                    ) {
                        Text("2x", fontSize = 12.sp)
                    }
                }
            }
            
            // Zoom Controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onZoomIn, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ZoomIn, "Zoom In", tint = TextSecondary)
                }
                IconButton(onClick = onZoomOut, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ZoomOut, "Zoom Out", tint = TextSecondary)
                }
            }
            
            // Reset
            IconButton(onClick = onResetView) {
                Icon(Icons.Default.Refresh, "Reset", tint = TextSecondary)
            }
            
            // Annotations
            IconButton(
                onClick = onAnnotationsToggle,
                modifier = Modifier.background(
                    if (showAnnotations) AccentTeal.copy(alpha = 0.2f) else Color.Transparent,
                    CircleShape
                )
            ) {
                Icon(
                    Icons.Default.Visibility,
                    "Annotations",
                    tint = if (showAnnotations) AccentTeal else TextSecondary
                )
            }
        }
    }
}

@Composable
fun RecordingButton(
    isRecording: Boolean,
    recordingProgress: Float,
    countdown: Int,
    permissionGranted: Boolean,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) Color(0xFFF44336).copy(alpha = 0.2f) else Color(0xFF1E1E2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Button(
            onClick = onRecordClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) ErrorRed else AccentTeal
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isRecording) "Stop Recording" else "Record New Analysis",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}