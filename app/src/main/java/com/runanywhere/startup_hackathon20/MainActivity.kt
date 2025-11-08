package com.runanywhere.startup_hackathon20

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlue
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlueLight
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

// Data class for recording files
data class RecordingFile(
    val file: File,
    var name: String,
    val originalName: String,
    val date: String,
    val duration: String,
    val size: String,
    val sizeBytes: Long,
    val sampleRate: String,
    val format: String
)

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        enableEdgeToEdge()

        setContent {
            Startup_hackathon20Theme {
                SpeechTwinApp(
                    onKeepScreenOn = { keepOn ->
                        if (keepOn) {
                            acquireWakeLock()
                        } else {
                            releaseWakeLock()
                        }
                    }
                )
            }
        }
    }

    private fun acquireWakeLock() {
        try {
            Log.d(TAG, "Acquiring wake lock")
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock?.release()
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "SpeechTwin::RecordingWakeLock"
            )
            wakeLock?.acquire(15 * 60 * 1000L)
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            Log.d(TAG, "Releasing wake lock")
            wakeLock?.release()
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock: ${e.message}")
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        releaseWakeLock()
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
    }
}

@Composable
fun SpeechTwinApp(
    onKeepScreenOn: (Boolean) -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf("home") }
    var visualization3DMetrics by remember { mutableStateOf<VocalFoldMetrics?>(null) }

    when (currentScreen) {
        "home" -> SpeechTwinScreen(
            onKeepScreenOn = onKeepScreenOn,
            onRecordingsClick = { currentScreen = "recordings" },
            onExercisesClick = { currentScreen = "exercises" },
            onDashboardClick = { currentScreen = "dashboard" },
            onGameClick = { currentScreen = "game" },
            on3DVisualizationClick = { metrics ->
                visualization3DMetrics = metrics
                currentScreen = "visualization"
            }
        )

        "dashboard" -> {
            DashboardScreen(
                onBackClick = { currentScreen = "home" }
            )
        }

        "recordings" -> {
            EnhancedRecordingsScreen(
                onBackClick = { currentScreen = "home" }
            )
        }

        "exercises" -> {
            VoiceExercisesScreen(
                onBackClick = { currentScreen = "home" }
            )
        }

        "game" -> {
            val context = LocalContext.current
            PitchMatchingGameScreen(
                onBackClick = { currentScreen = "home" },
                context = context
            )
        }

        "visualization" -> {
            VocalFoldVisualizationScreen(
                metrics = visualization3DMetrics ?: VocalFoldMetrics(
                    healthScore = 75f,
                    pitch = 180f,
                    loudness = 65f,
                    jitter = 1.5f,
                    shimmer = 3.2f
                ),
                recordingName = "Latest Recording",
                onBackClick = { currentScreen = "home" }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechTwinScreen(
    onKeepScreenOn: (Boolean) -> Unit = {},
    onRecordingsClick: () -> Unit = {},
    onExercisesClick: () -> Unit = {},
    onDashboardClick: () -> Unit = {},
    onGameClick: () -> Unit = {},
    on3DVisualizationClick: (VocalFoldMetrics) -> Unit = {}
) {
    // --- Color Palette for Home Page Modern Theme ---
    val PrimaryBlue = Color(0xFF1353E4)
    val AccentTeal = Color(0xFF33DBCE)
    val ErrorRed = Color(0xFFD32F2F)
    // --------
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(false) }
    var waveformView by remember { mutableStateOf<WaveformSurfaceView?>(null) }
    var audioRecorder by remember { mutableStateOf<AudioRecorder?>(null) }
    var countdown by remember { mutableStateOf(10) }
    var recordingProgress by remember { mutableStateOf(0f) }

    // Voice analysis state
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<VoiceAnalyzer.AnalysisResult?>(null) }
    var showAnalysisDialog by remember { mutableStateOf(false) }

    // Healthy Voice Suggestion state
    var healthyVoiceFile by remember { mutableStateOf<File?>(null) }
    var processingResult by remember { mutableStateOf<AudioProcessor.ProcessingResult?>(null) }
    var isProcessingHealthyVoice by remember { mutableStateOf(false) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }

    // Help panel state
    var showHelpPanel by remember { mutableStateOf(false) }

    // Speed dial menu state (FAB in bottom-right)
    var isSpeedDialExpanded by remember { mutableStateOf(false) }

    // Real-time waveform state
    var amplitudeValues by remember { mutableStateOf(List(40) { 0f }) }
    var currentAmplitude by remember { mutableStateOf(0f) }

    // Dynamic greeting based on hour
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember {
        when (currentHour) {
            in 5..10 -> "Good Morning! ☀️"
            in 11..16 -> "Good Afternoon! 🌤️"
            in 17..21 -> "Good Evening! 🌙"
            else -> "Hello! 👋"
        }
    }

    // Quick stats for Card (load once)
    val recordings = remember { loadRecordingsFromCache(context) }
    val totalRecordings = recordings.size
    val latestScore = recordings.firstOrNull()?.vocalHealthScore ?: 0
    val streak = calculateStreakFromRecordings(recordings)

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (!granted) {
            Toast.makeText(context, "Audio recording permission is required", Toast.LENGTH_LONG)
                .show()
        }
    }

    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    Log.d("SpeechTwinScreen", "App going to background, stopping recording")
                    if (isRecording) {
                        isRecording = false
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d("SpeechTwinScreen", "Activity destroyed, cleaning up")
                    audioRecorder?.stopRecording()
                    waveformView?.stopDrawing()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                audioRecorder?.stopRecording()
                waveformView?.stopDrawing()
            } catch (e: Exception) {
                Log.e("SpeechTwinScreen", "Error during cleanup: ${e.message}")
            }
        }
    }

    // Check permission on start
    LaunchedEffect(Unit) {
        permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Handle recording lifecycle
    LaunchedEffect(isRecording) {
        try {
            onKeepScreenOn(isRecording)
            if (isRecording && audioRecorder == null) {
                Log.d("SpeechTwinScreen", "Starting new recording")
                val recorder = AudioRecorder()
                recorder.setAmplitudeCallback { amplitude ->
                    try {
                        currentAmplitude = amplitude
                        amplitudeValues = amplitudeValues.drop(1) + amplitude
                        waveformView?.updateAmplitude(amplitude)
                    } catch (e: Exception) {
                        Log.w("SpeechTwinScreen", "Error updating waveform: ${e.message}")
                    }
                }
                audioRecorder = recorder

                val success = recorder.startRecording()
                if (!success) {
                    Log.e("SpeechTwinScreen", "Failed to start recording")
                    isRecording = false
                    audioRecorder = null
                    Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show()
                }
            } else if (!isRecording && audioRecorder != null) {
                Log.d("SpeechTwinScreen", "Stopping recording")
                val recordingCompleted = countdown == 0

                audioRecorder?.stopRecording()

                if (recordingCompleted) {
                    try {
                        val cacheDir = context.cacheDir
                        val audioFile =
                            File(cacheDir, "recorded_audio_${System.currentTimeMillis()}.wav")
                        val saved = audioRecorder?.saveToWavFile(audioFile) ?: false

                        if (saved && audioFile.exists()) {
                            val fileSize = audioFile.length()
                            val fileSizeMB = String.format("%.2f", fileSize / (1024.0 * 1024.0))
                            Toast.makeText(
                                context,
                                "Recording saved: ${fileSizeMB}MB",
                                Toast.LENGTH_LONG
                            ).show()

                            currentAudioFile = audioFile

                            scope.launch {
                                isAnalyzing = true
                                showAnalysisDialog = true

                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        val analyzer = VoiceAnalyzer()
                                        analyzer.analyze(audioFile.absolutePath)
                                    }

                                    analysisResult = result
                                    isAnalyzing = false

                                    Toast.makeText(
                                        context,
                                        "Analysis complete! Pitch: ${result.pitch.toInt()} Hz, Health: ${result.healthScore}%",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    if (result.healthScore < 70) {
                                        isProcessingHealthyVoice = true

                                        try {
                                            val healthyFile = File(
                                                context.cacheDir,
                                                "healthy_${System.currentTimeMillis()}.wav"
                                            )
                                            val processing = withContext(Dispatchers.IO) {
                                                AudioProcessor.processToHealthyVoice(
                                                    audioFile,
                                                    healthyFile,
                                                    result.jitter,
                                                    result.shimmer
                                                )
                                            }
                                            if (processing.success) {
                                                healthyVoiceFile = healthyFile
                                                processingResult = processing
                                                Log.d(
                                                    "SpeechTwinScreen",
                                                    "Healthy voice generated successfully!"
                                                )
                                            }
                                        } catch (e: Exception) {
                                            Log.e(
                                                "SpeechTwinScreen",
                                                "Error generating healthy voice: ${e.message}"
                                            )
                                        } finally {
                                            isProcessingHealthyVoice = false
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("SpeechTwinScreen", "Error analyzing audio: ${e.message}")
                                    isAnalyzing = false
                                    Toast.makeText(
                                        context,
                                        "Analysis failed: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Failed to save recording", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } catch (e: Exception) {
                        Log.e("SpeechTwinScreen", "Error saving file: ${e.message}")
                        Toast.makeText(context, "Error saving recording", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("SpeechTwinScreen", "Recording stopped early - discarding")
                    Toast.makeText(context, "Recording cancelled", Toast.LENGTH_SHORT).show()
                }

                audioRecorder = null
                countdown = 10
                recordingProgress = 0f

                try {
                    waveformView?.clearWaveform()
                } catch (e: Exception) {
                    Log.w("SpeechTwinScreen", "Error clearing waveform: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("SpeechTwinScreen", "Error in recording lifecycle: ${e.message}")
            isRecording = false
            audioRecorder?.stopRecording()
            audioRecorder = null
            Toast.makeText(context, "Recording error occurred", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-stop recording after 10 seconds
    LaunchedEffect(isRecording) {
        if (isRecording) {
            try {
                for (i in 10 downTo 0) {
                    if (!isRecording) break

                    countdown = i
                    recordingProgress = (10 - i) / 10f

                    if (i == 0) {
                        Log.d("SpeechTwinScreen", "Recording time limit reached, stopping")
                        isRecording = false
                        break
                    }

                    delay(1000)
                }
            } catch (e: Exception) {
                Log.e("SpeechTwinScreen", "Error in countdown timer: ${e.message}")
                isRecording = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryBlue,
                        AccentTeal
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero section: Logo, app name, subtitle, greeting with animation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "SpeechTwin",
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "SpeechTwin",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Your Personal Voice Health Companion",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    greeting,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            // Quick Stats Card (glassmorphism, click to dashboard)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable(onClick = onDashboardClick),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickStatItem("🎯", "Score", "$latestScore")
                    QuickStatItem("📊", "Recordings", "$totalRecordings")
                    QuickStatItem("🔥", "Streak", "${streak}d")
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Waveform Visualization (shown during recording)
            if (isRecording) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        RealtimeWaveformVisualization(
                            amplitudeValues = amplitudeValues,
                            currentAmplitude = currentAmplitude,
                            isRecording = isRecording,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Main recording button, pulsing, large
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Pulse glow on idle
                if (!isRecording) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val glowScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer(scaleX = glowScale, scaleY = glowScale)
                            .background(
                                Color.White.copy(alpha = 0.1f),
                                CircleShape
                            )
                    )
                }

                // Countdown ring overlay during recording
                if (isRecording) {
                    CircularCountdownTimer(
                        modifier = Modifier.size(200.dp),
                        totalTimeSeconds = 10,
                        isRunning = isRecording,
                        onTimeUpdate = { time ->
                            countdown = time
                        },
                        onComplete = {
                            Log.d("SpeechTwinScreen", "Countdown completed")
                            isRecording = false
                        }
                    )
                }

                // Main record/stop FAB (large)
                FloatingActionButton(
                    onClick = {
                        try {
                            if (permissionGranted) {
                                Log.d(
                                    "SpeechTwinScreen",
                                    "Record button clicked, isRecording: $isRecording"
                                )
                                isRecording = !isRecording
                            } else {
                                Log.d("SpeechTwinScreen", "Requesting audio permission")
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        } catch (e: Exception) {
                            Log.e(
                                "SpeechTwinScreen",
                                "Error handling button click: ${e.message}"
                            )
                        }
                    },
                    modifier = Modifier.size(if (isRecording) 80.dp else 120.dp),
                    containerColor = if (isRecording) ErrorRed else Color.White,
                    contentColor = if (isRecording) Color.White else PrimaryBlue
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop" else "Record",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = when {
                    !permissionGranted -> "Microphone permission required"
                    isRecording -> "Recording... ${countdown}s remaining"
                    else -> "Tap to record 10 seconds"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        // Scrim overlay when speed dial is open (MUST BE BEFORE FABs in z-order)
        if (isSpeedDialExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isSpeedDialExpanded = false }
            )
        }

        // Speed Dial FAB Menu (Bottom-Right) - appears ABOVE scrim
        SpeedDialFAB(
            isExpanded = isSpeedDialExpanded,
            onToggle = { isSpeedDialExpanded = !isSpeedDialExpanded },
            onGameClick = {
                isSpeedDialExpanded = false
                onGameClick()
            },
            onExercisesClick = {
                isSpeedDialExpanded = false
                onExercisesClick()
            },
            onDashboardClick = {
                isSpeedDialExpanded = false
                onDashboardClick()
            },
            onRecordingsClick = {
                isSpeedDialExpanded = false
                onRecordingsClick()
            },
            on3DClick = {
                isSpeedDialExpanded = false
                val metrics = VocalFoldMetrics(
                    healthScore = analysisResult?.healthScore?.toFloat() ?: 75f,
                    pitch = analysisResult?.pitch?.toFloat() ?: 180f,
                    loudness = analysisResult?.loudness?.toFloat() ?: 65f,
                    jitter = analysisResult?.jitter?.toFloat() ?: 1.5f,
                    shimmer = analysisResult?.shimmer?.toFloat() ?: 3.2f
                )
                on3DVisualizationClick(metrics)
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }

    // --- Help panel as bottom sheet ---
    if (showHelpPanel) {
        HelpBottomSheet(
            onDismiss = { showHelpPanel = false },
            context = context
        )
    }

    // --- Animated Help FAB bottom-left (static, no pulse) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding(),
        contentAlignment = Alignment.BottomStart
    ) {
        FloatingActionButton(
            onClick = { showHelpPanel = true },
            modifier = Modifier.size(56.dp),
            containerColor = Color(0xFFFF6F00),
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Help,
                contentDescription = "Help & Tutorial",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // --- Show analysis dialog as usual ----
    if (showAnalysisDialog) {
        // KEEP EXISTING ANALYSIS DIALOG CODE (see original)
        AlertDialog(
            onDismissRequest = { showAnalysisDialog = false },
            title = {
                Text(
                    "Voice Analysis Results",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    if (isAnalyzing) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = VocalBlue
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Analyzing your voice...",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        analysisResult?.let { result ->
                            // Health Score with Circular Progress
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier.size(120.dp),
                                    strokeWidth = 12.dp,
                                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                                    color = Color.Transparent
                                )
                                CircularProgressIndicator(
                                    progress = { result.healthScore / 100f },
                                    modifier = Modifier.size(120.dp),
                                    strokeWidth = 12.dp,
                                    color = when {
                                        result.healthScore >= 80 -> Color(0xFF4CAF50)
                                        result.healthScore >= 60 -> Color(0xFFFFC107)
                                        result.healthScore >= 40 -> Color(0xFFFF9800)
                                        else -> Color(0xFFF44336)
                                    },
                                    trackColor = Color.Transparent
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${result.healthScore}",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            result.healthScore >= 80 -> Color(0xFF4CAF50)
                                            result.healthScore >= 60 -> Color(0xFFFFC107)
                                            result.healthScore >= 40 -> Color(0xFFFF9800)
                                            else -> Color(0xFFF44336)
                                        }
                                    )
                                    Text(
                                        text = "Health Score",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Interpretation Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        result.healthScore >= 80 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                        result.healthScore >= 60 -> Color(0xFFFFC107).copy(alpha = 0.1f)
                                        result.healthScore >= 40 -> Color(0xFFFF9800).copy(alpha = 0.1f)
                                        else -> Color(0xFFF44336).copy(alpha = 0.1f)
                                    }
                                )
                            ) {
                                Text(
                                    text = when {
                                        result.healthScore >= 85 -> "Excellent - Your voice is healthy and strong"
                                        result.healthScore >= 70 -> "Good - Voice is in good condition"
                                        result.healthScore >= 50 -> "Fair - Some vocal strain detected. Consider rest."
                                        result.healthScore >= 30 -> "Needs Attention - Moderate vocal stress"
                                        else -> "Concern - Significant strain. Consult specialist."
                                    },
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Voice Metrics
                            Text(
                                "Voice Metrics",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Pitch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Pitch",
                                        tint = VocalBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pitch", fontSize = 14.sp, color = Color.Gray)
                                }
                                Text(
                                    "${String.format("%.1f", result.pitch)} Hz",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Loudness
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "Loudness",
                                        tint = VocalBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Loudness", fontSize = 14.sp, color = Color.Gray)
                                }
                                Text(
                                    "${String.format("%.1f", result.loudness)} dB",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Jitter
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Jitter",
                                        tint = VocalBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Jitter", fontSize = 14.sp, color = Color.Gray)
                                }
                                Text(
                                    "${String.format("%.2f", result.jitter)}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Shimmer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Shimmer",
                                        tint = VocalBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Shimmer", fontSize = 14.sp, color = Color.Gray)
                                }
                                Text(
                                    "${String.format("%.2f", result.shimmer)}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))

                            // Personal Voice Insights
                            VoiceInsights(result = result)

                            // AI-Powered Healthy Voice Suggestion (if available)
                            if (result.healthScore < 70 && (isProcessingHealthyVoice || healthyVoiceFile != null)) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(16.dp))
                                HealthyVoiceSuggestion(
                                    isProcessing = isProcessingHealthyVoice,
                                    processingResult = processingResult,
                                    originalFile = currentAudioFile,
                                    healthyFile = healthyVoiceFile,
                                    context = context
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!isAnalyzing) {
                    TextButton(onClick = { showAnalysisDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

// --- Supplementary composables for quick stats & speed dial ---

@Composable
fun QuickStatItem(icon: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = icon,
            fontSize = 32.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White
        )
        Text(
            label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SpeedDialFAB(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onGameClick: () -> Unit,
    onExercisesClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onRecordingsClick: () -> Unit,
    on3DClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Floating speed dial, using animation for reveal
    val transition = updateTransition(isExpanded, label = "fab-expand")
    val fabAlpha by transition.animateFloat(
        label = "fab-alpha"
    ) { expanded -> if (expanded) 1f else 0f }
    val fabTranslate = listOf(-72.dp, -144.dp, -216.dp, -288.dp, -360.dp)
    Box(
        modifier = modifier
            .padding(bottom = 16.dp, end = 16.dp)
    ) {
        // Expand mini FABs upward when open, animate
        if (isExpanded) {
            // Positions: 1=Game, 2=Exercises, 3=Dashboard, 4=3D, 5=Recordings
            MiniSpeedDialFAB(
                icon = Icons.Default.VideogameAsset,
                label = "Game",
                onClick = onGameClick,
                color = Color(0xFFFF5252),
                modifier = Modifier
                    .graphicsLayer(
                        alpha = fabAlpha
                    )
                    .offset(y = fabTranslate[0])
            )
            MiniSpeedDialFAB(
                icon = Icons.Default.FitnessCenter,
                label = "Exercises",
                onClick = onExercisesClick,
                color = Color(0xFF00C853),
                modifier = Modifier
                    .graphicsLayer(
                        alpha = fabAlpha,
                    )
                    .offset(y = fabTranslate[1])
            )
            MiniSpeedDialFAB(
                icon = Icons.Default.Dashboard,
                label = "Dashboard",
                onClick = onDashboardClick,
                color = Color(0xFFFF9800), // Bright Orange
                modifier = Modifier
                    .graphicsLayer(
                        alpha = fabAlpha,
                    )
                    .offset(y = fabTranslate[2])
            )
            MiniSpeedDialFAB(
                icon = Icons.Default.ViewInAr,
                label = "3D View",
                onClick = on3DClick,
                color = Color(0xFF00BCD4),
                modifier = Modifier
                    .graphicsLayer(
                        alpha = fabAlpha,
                    )
                    .offset(y = fabTranslate[3])
            )
            MiniSpeedDialFAB(
                icon = Icons.Default.List,
                label = "Recordings",
                onClick = onRecordingsClick,
                color = Color(0xFF1353E4), // Blue
                modifier = Modifier
                    .graphicsLayer(
                        alpha = fabAlpha,
                    )
                    .offset(y = fabTranslate[4])
            )
        }

        // Main FAB (plus/close)
        FloatingActionButton(
            onClick = onToggle,
            containerColor = Color.White,
            contentColor = Color(0xFF1353E4),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp),
            modifier = Modifier.size(68.dp)
        ) {
            val animAngle by transition.animateFloat(label = "plus-rotate") { expanded -> if (expanded) 45f else 0f }
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isExpanded) "Close" else "Open menu",
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        rotationZ = animAngle
                    }
            )
        }
    }
}

@Composable
fun MiniSpeedDialFAB(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(bottom = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.13f)
        ) {
            Text(
                label,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = color,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        FloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 5.dp),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
        }
    }
}


// Helper function to format duration
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${remainingSeconds}s"
    }
}

/**
 * Healthy Voice Suggestion UI
 * Shows AI-generated before/after voice with playback controls
 */
@Composable
fun HealthyVoiceSuggestion(
    isProcessing: Boolean,
    processingResult: AudioProcessor.ProcessingResult?,
    originalFile: File?,
    healthyFile: File?,
    context: android.content.Context
) {
    var isPlayingOriginal by remember { mutableStateOf(false) }
    var isPlayingHealthy by remember { mutableStateOf(false) }
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }

    // Cleanup media player on screen exit
    DisposableEffect(isPlayingOriginal, isPlayingHealthy) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
            isPlayingOriginal = false
            isPlayingHealthy = false
        }
    }

    // Play audio helper
    fun playFileAudio(file: File?, onDone: () -> Unit) {
        try {
            if (file == null || !file.exists()) return
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    onDone()
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Text(
            "AI-Powered Healthy Voice Suggestion",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1976D2),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            "See and hear how your voice can sound with improved health.",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        if (isProcessing) {
            // Still generating audio
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = Color(0xFF1976D2),
                    modifier = Modifier.size(30.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Generating your healthier-sounding audio...", fontSize = 14.sp)
            }
        } else if (healthyFile != null && healthyFile.exists() && originalFile != null && originalFile.exists()) {
            // A/B comparison UI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Your Original",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    IconButton(
                        onClick = {
                            if (isPlayingOriginal) {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                isPlayingOriginal = false
                            } else {
                                isPlayingOriginal = true
                                isPlayingHealthy = false
                                playFileAudio(originalFile) {
                                    isPlayingOriginal = false
                                }
                            }
                        },
                        enabled = !isPlayingHealthy
                    ) {
                        Icon(
                            imageVector = if (isPlayingOriginal) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingOriginal) "Stop" else "Play",
                            modifier = Modifier.size(48.dp),
                            tint = if (isPlayingOriginal) Color(0xFFF44336) else Color(0xFF1976D2)
                        )
                    }
                    Text(
                        "${(originalFile.length() / 1024).coerceAtLeast(1)} KB",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "AI-Healthy Twin",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    IconButton(
                        onClick = {
                            if (isPlayingHealthy) {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                isPlayingHealthy = false
                            } else {
                                isPlayingHealthy = true
                                isPlayingOriginal = false
                                playFileAudio(healthyFile) {
                                    isPlayingHealthy = false
                                }
                            }
                        },
                        enabled = !isPlayingOriginal
                    ) {
                        Icon(
                            imageVector = if (isPlayingHealthy) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingHealthy) "Stop" else "Play",
                            modifier = Modifier.size(48.dp),
                            tint = if (isPlayingHealthy) Color(0xFF388E3C) else Color(0xFF43A047)
                        )
                    }
                    Text(
                        "${(healthyFile.length() / 1024).coerceAtLeast(1)} KB",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
            }

            if (processingResult != null && processingResult.success) {
                Text(
                    "This is what your voice could sound like if you reduced vocal strain.\nTry to match this healthy version!",
                    fontSize = 13.sp,
                    color = Color(0xFF00695C),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 0.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else if (processingResult != null && !processingResult.success) {
            Text(
                "Unable to generate healthy voice suggestion. Try recording again.",
                fontSize = 13.sp,
                color = Color.Red
            )
        }
    }
}

/**
 * Real-time Waveform Visualization
 * Shows animated amplitude bars that respond to voice input
 */
@Composable
fun RealtimeWaveformVisualization(
    amplitudeValues: List<Float>,
    currentAmplitude: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isRecording) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    Box(
        modifier = modifier
            .alpha(alpha)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Show "Speak now..." when amplitude is very low
        if (currentAmplitude < 0.05f && isRecording) {
            Text(
                text = "Speak now...",
                color = VocalBlue.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Draw waveform bars
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            val barCount = 40
            val barWidth = size.width / barCount
            val maxHeight = size.height * 0.9f

            // Draw background grid
            for (i in 1..4) {
                val y = size.height * i / 5f
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            // Draw amplitude bars
            amplitudeValues.forEachIndexed { index, amplitude ->
                val barHeight = (amplitude * maxHeight).coerceAtLeast(maxHeight * 0.05f)
                val x = index * barWidth + barWidth / 2
                val y = (size.height - barHeight) / 2

                // Color gradient based on amplitude
                val color = when {
                    amplitude < 0.3f -> VocalBlue
                    amplitude < 0.6f -> Color.Cyan
                    amplitude < 0.8f -> Color(0xFFFF9800)
                    else -> Color.Red
                }

                // Draw glow effect
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(x, size.height / 2)
                    ),
                    radius = barHeight / 2 + 4f,
                    center = Offset(x, size.height / 2)
                )

                // Draw main bar
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.6f)),
                        startY = y,
                        endY = y + barHeight
                    ),
                    topLeft = Offset(x - 1.dp.toPx(), y),
                    size = androidx.compose.ui.geometry.Size(2.dp.toPx(), barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }
        }
    }
}

enum class InsightType {
    SUCCESS,
    WARNING,
    INFO
}

/**
 * Intelligent Insights Section
 * Provides personalized recommendations based on voice analysis
 */
@Composable
fun VoiceInsights(result: VoiceAnalyzer.AnalysisResult) {
    val insights = mutableListOf<Triple<String, String, InsightType>>()

    // Analyze jitter
    if (result.jitter > 2.0f) {
        insights.add(
            Triple(
                "Pitch Fluctuation Detected",
                "Your pitch is fluctuating. Try humming exercises to stabilize your voice.",
                InsightType.WARNING
            )
        )
    } else if (result.jitter < 1.0f) {
        insights.add(
            Triple(
                "Excellent Pitch Stability",
                "Your voice shows great pitch control! Keep up the good work.",
                InsightType.SUCCESS
            )
        )
    }

    // Analyze shimmer
    if (result.shimmer > 5.0f) {
        insights.add(
            Triple(
                "Uneven Voice Amplitude",
                "Voice amplitude is uneven. Practice steady breathing exercises.",
                InsightType.WARNING
            )
        )
    } else if (result.shimmer < 3.0f) {
        insights.add(
            Triple(
                "Consistent Voice Power",
                "Your voice amplitude is nice and steady!",
                InsightType.SUCCESS
            )
        )
    }

    // Analyze loudness
    when {
        result.loudness < -40f -> insights.add(
            Triple(
                "Speak a Bit Louder",
                "Your voice is quiet. Try moving closer to the microphone.",
                InsightType.INFO
            )
        )

        result.loudness > -10f -> insights.add(
            Triple(
                "Volume is High",
                "Your volume is quite high. Don't strain your voice.",
                InsightType.INFO
            )
        )

        else -> insights.add(
            Triple(
                "Perfect Volume",
                "Your speaking volume is ideal for analysis!",
                InsightType.SUCCESS
            )
        )
    }

    // Analyze pitch range
    when {
        result.pitch < 100f -> insights.add(
            Triple(
                "Lower Pitch Range",
                "Your pitch is lower than average. This is normal for male voices.",
                InsightType.INFO
            )
        )

        result.pitch > 250f -> insights.add(
            Triple(
                "Higher Pitch Range",
                "Your pitch is higher than average. This is normal for female voices.",
                InsightType.INFO
            )
        )

        else -> insights.add(
            Triple(
                "Natural Pitch Range",
                "Your pitch is in a healthy and comfortable range.",
                InsightType.SUCCESS
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Personal Voice Insights",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Show top 3 insights
        insights.take(3).forEach { (title, description, type) ->
            InsightCard(title, description, type)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Daily tip based on time of day
        DailyTipCard()
    }
}

@Composable
fun InsightCard(title: String, description: String, type: InsightType) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (type) {
                InsightType.SUCCESS -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                InsightType.WARNING -> Color(0xFFFF9800).copy(alpha = 0.1f)
                InsightType.INFO -> VocalBlue.copy(alpha = 0.1f)
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (type) {
                    InsightType.SUCCESS -> Icons.Default.Check
                    InsightType.WARNING -> Icons.Default.Warning
                    InsightType.INFO -> Icons.Default.Info
                },
                contentDescription = null,
                tint = when (type) {
                    InsightType.SUCCESS -> Color(0xFF4CAF50)
                    InsightType.WARNING -> Color(0xFFFF9800)
                    InsightType.INFO -> VocalBlue
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DailyTipCard() {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val (emoji, title, tip) = when {
        currentHour in 5..11 -> Triple(
            "☀️",
            "Morning Voice Care",
            "Warm up your voice with gentle humming before speaking. Your vocal cords need time to wake up!"
        )

        currentHour in 12..17 -> Triple(
            "💧",
            "Stay Hydrated",
            "Drink water every hour to keep your vocal cords hydrated. Avoid caffeine which can dry them out."
        )

        currentHour in 18..21 -> Triple(
            "🌙",
            "Evening Voice Rest",
            "Avoid straining your voice before bed. Give your vocal cords time to recover overnight."
        )

        else -> Triple(
            "😴",
            "Night Time",
            "Your voice needs rest! Try to limit speaking and get good sleep for vocal recovery."
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9C4)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF795548)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tip,
                    fontSize = 12.sp,
                    color = Color(0xFF5D4037)
                )
            }
        }
    }
}

// ------------------------------------
// Dashboard Screen Implementation
// ------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var targetHealthScore by remember { mutableStateOf(85) }
    
    // Load real data
    val recordings = remember { loadRecordingsFromCache(context) }
    val recordingCount = recordings.size
    
    // Calculate today's score (from most recent recording)
    val todayScore = recordings.firstOrNull()?.vocalHealthScore ?: 0
    
    // Calculate week average
    val weekRecordings = recordings.filter { isThisWeek(it.timestamp) }
    val weekAverage = if (weekRecordings.isNotEmpty()) {
        weekRecordings.map { it.vocalHealthScore }.average().toInt()
    } else {
        0
    }
    
    // Calculate all-time best
    val allTimeBest = recordings.maxOfOrNull { it.vocalHealthScore } ?: 0
    
    // Calculate streak (consecutive days with recordings)
    val streak = calculateStreakFromRecordings(recordings)
    
    // Get exercise count from VoiceExercisesScreen state (using a shared preference or state)
    val exerciseCount = 0 // Will be 0 initially since exercises are per-session

    // Get most recent recording metrics for display
    val recentHealthScore = todayScore
    val recentDuration = recordings.firstOrNull()?.duration ?: 0

    // Get a 7-day trend (latest per day, up to 7)
    val trendData = get7DayTrendFromRecordings(recordings)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Health Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VocalBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview Cards
            item {
                Text(
                    "Overview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardStatCard(
                        label = "Today's Score",
                        value = if (todayScore > 0) todayScore.toString() else "-",
                        emoji = "🌟",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        label = "Week Average",
                        value = if (weekAverage > 0) weekAverage.toString() else "-",
                        emoji = "📈",
                        color = VocalBlue,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        label = "All-Time Best",
                        value = if (allTimeBest > 0) allTimeBest.toString() else "-",
                        emoji = "🏆",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardStatCard(
                        label = "Recordings",
                        value = recordingCount.toString(),
                        emoji = "🎤",
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        label = "Exercises",
                        value = exerciseCount.toString(),
                        emoji = "🏋️",
                        color = Color(0xFF00695C),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        label = "Streak",
                        value = if (streak > 0) "${streak}d" else "-",
                        emoji = "🔥",
                        color = Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Trend Chart
            item {
                Text(
                    "7-Day Health Trend",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Health Score Over Time",
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        SimpleTrendChart(
                            data = trendData.map { it.second.toFloat() },
                            labels = trendData.map { it.first }
                        )
                    }
                }
            }

            // Voice Metrics Breakdown
            item {
                Text(
                    "Voice Metrics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF9C27B0).copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Most Recent Health Score: $recentHealthScore",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )
                        Text(
                            "Duration: ${formatDuration(recentDuration)}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        if (recordings.isEmpty()) {
                            Text("No voice data available. Record to view your metrics.", color = Color.Gray)
                        }
                    }
                }
            }

            // Insights
            item {
                Text(
                    "Personal Insights",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                // We'll only show health score insights
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.08f)
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Overall Vocal Health",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        when {
                            todayScore >= 85 -> Text("Excellent voice health! Keep maintaining good vocal habits.")
                            todayScore >= 70 -> Text("Good voice health. Regular practice will help you improve.")
                            todayScore >= 50 -> Text("Fair voice health. Try doing exercises and rest your voice.")
                            todayScore > 0   -> Text("Needs improvement. Remember to hydrate and avoid vocal strain.")
                            else             -> Text("No recent scores. Make a recording to get your health score!")
                        }
                    }
                }
            }

            // Goals Section
            item {
                Text(
                    "Your Goal",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFC107).copy(alpha = 0.15f)
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Target Health Score",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            Text(
                                "$targetHealthScore",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFFF6F00)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = {
                                val current = todayScore.toFloat()
                                val goal = targetHealthScore.toFloat()
                                if (goal > 0f) (current / goal).coerceIn(0f, 1f) else 0f
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = Color(0xFFFF6F00),
                            trackColor = Color.LightGray.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current: $todayScore", fontSize = 12.sp, color = Color.Gray)
                            Text("Goal: $targetHealthScore", fontSize = 12.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Adjust Your Goal:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = targetHealthScore.toFloat(),
                            onValueChange = { targetHealthScore = it.toInt() },
                            valueRange = 60f..100f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF6F00),
                                activeTrackColor = Color(0xFFFF6F00)
                            )
                        )
                    }
                }
            }
        }
    }
}

// Utilities for dashboard stats


/**
 * Calculate streak: number of consecutive days with recordings, up to today, from a sorted recordings list.
 */
fun calculateStreakFromRecordings(recordings: List<RecordingItem>): Int {
    if (recordings.isEmpty()) return 0
    val dateSet = recordings.map { dayStartEpoch(it.timestamp) }.distinct()
    if (dateSet.isEmpty()) return 0
    val today = dayStartEpoch(System.currentTimeMillis())
    var streak = 0
    for (i in dateSet.indices) {
        val expected = today - 86400000L * i
        if (dateSet[i] == expected) streak++
        else break
    }
    return streak
}

/**
 * Get start of day (00:00) for a timestamp, in epoch ms
 */
fun dayStartEpoch(timeMillis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timeMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/**
 * Returns up to the last seven days of scores with formatted day labels.
 * E.g. [("Mon", 80), ("Tue", 82), ...]. Most recent day last.
 */
fun get7DayTrendFromRecordings(recordings: List<RecordingItem>): List<Pair<String, Int>> {
    val trend = mutableListOf<Pair<String, Int>>()
    val byDay = recordsByDay(recordings)
    val days = byDay.keys.sorted()
    val formatter = SimpleDateFormat("EEE", Locale.getDefault())
    for (day in days.takeLast(7)) {
        val (dateMillis, recs) = day to byDay[day]!!
        val label = formatter.format(Date(dateMillis))
        // Use highest score in day (or latest)
        val score = recs.maxByOrNull { it.timestamp }?.vocalHealthScore ?: 0
        trend.add(label to score)
    }
    return trend
}

/** Group recordings by start-of-day epoch ms. */
fun recordsByDay(recordings: List<RecordingItem>): Map<Long, List<RecordingItem>> {
    return recordings.groupBy { dayStartEpoch(it.timestamp) }
}

@Composable
fun DashboardStatCard(
    label: String,
    value: String,
    emoji: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                emoji,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SimpleTrendChart(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val padding = 40f
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding * 2

        val maxValue = data.maxOrNull() ?: 100f
        val minValue = data.minOrNull() ?: 0f
        val range = maxValue - minValue

        // Draw grid lines
        for (i in 0..4) {
            val y = padding + (chartHeight * i / 4)
            drawLine(
                color = Color.Gray.copy(alpha = 0.13f),
                start = Offset(padding, y),
                end = Offset(size.width - padding, y),
                strokeWidth = 1f
            )
        }

        // Draw line chart
        if (data.size > 1) {
            val points = data.mapIndexed { index, value ->
                val x = padding + (chartWidth * index / (data.size - 1).toFloat())
                val normalizedValue = (value - minValue) / range
                val y = padding + chartHeight * (1 - normalizedValue)
                Offset(x, y)
            }

            // Draw connecting lines
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = VocalBlue,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 4f
                )
            }

            // Draw data points
            points.forEach { point ->
                drawCircle(
                    color = VocalBlue,
                    radius = 7f,
                    center = point
                )
            }
        }

        // Draw labels
        labels.forEachIndexed { index, label ->
            val x = padding + (chartWidth * index / (labels.size - 1).toFloat())
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(label, x.toFloat(), (size.height - 10f).toFloat(), paint)
            }
        }
    }
}

@Composable
fun DashboardMetricRow(
    name: String,
    value: String,
    improvement: Float,
    higherIsBetter: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9C27B0)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val isPositive =
                if (higherIsBetter) improvement > 0 else improvement < 0
            Text(
                text = "${if (improvement > 0) "+" else ""}%.2f".format(improvement),
                fontSize = 12.sp,
                color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            Icon(
                imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ------------------------------------
// Voice Exercises Screen Implementation
// ------------------------------------

data class VoiceExercise(
    val id: Int,
    val name: String,
    val emoji: String,
    val benefit: String,
    val duration: Int,
    val instructions: String,
    val difficulty: String,
    val color: Color,
    val recommended: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceExercisesScreen(
    onBackClick: () -> Unit
) {
    var selectedExercise by remember { mutableStateOf<VoiceExercise?>(null) }
    var isExercising by remember { mutableStateOf(false) }
    var exerciseProgress by remember { mutableStateOf(0f) }
    var countdown by remember { mutableStateOf(0) }
    var completedExercises by remember { mutableStateOf(setOf<Int>()) }
    var showCountdown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableStateOf(3) }
    var exerciseScore by remember { mutableStateOf(0) }
    var showResults by remember { mutableStateOf(false) }

    val exercises = listOf(
        VoiceExercise(
            1,
            "Humming Scale",
            "🎵",
            "Improves pitch stability",
            30,
            "Hum smoothly from your lowest to highest comfortable pitch",
            "Easy",
            Color(0xFF2196F3),
            recommended = true
        ),
        VoiceExercise(
            2,
            "Sustained \"Ah\"",
            "😮",
            "Builds breath control",
            10,
            "Hold a steady 'Ahhh' sound for as long as possible",
            "Medium",
            Color(0xFF4CAF50)
        ),
        VoiceExercise(
            3,
            "Lip Trills",
            "💨",
            "Relaxes vocal cords",
            20,
            "Make a 'brrr' sound while sliding your pitch up and down",
            "Easy",
            Color(0xFFFF9800),
            recommended = true
        ),
        VoiceExercise(
            4,
            "Siren Sounds",
            "🚨",
            "Expands vocal range",
            25,
            "Slide from your lowest to highest pitch like a siren",
            "Medium",
            Color(0xFF9C27B0)
        ),
        VoiceExercise(
            5,
            "Breath Control",
            "🫁",
            "Strengthens breathing",
            15,
            "Breathe in for 4 counts, hold for 4, breathe out for 4",
            "Easy",
            Color(0xFF00BCD4)
        )
    )

    val scope = rememberCoroutineScope()

    // Exercise countdown logic
    LaunchedEffect(isExercising, selectedExercise) {
        if (isExercising && selectedExercise != null) {
            val duration = selectedExercise?.duration ?: 0
            for (i in duration downTo 0) {
                if (!isExercising) break
                countdown = i
                exerciseProgress = if (duration != 0) (duration - i).toFloat() / duration else 0f
                delay(1000)

                if (i == 0) {
                    isExercising = false
                    completedExercises =
                        selectedExercise?.id?.let { completedExercises + it } ?: completedExercises
                    exerciseScore = (70..95).random()
                    showResults = true
                }
            }
        }
    }

    // Pre-exercise countdown
    LaunchedEffect(showCountdown) {
        if (showCountdown) {
            for (i in 3 downTo 1) {
                countdownValue = i
                delay(1000)
            }
            showCountdown = false
            isExercising = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Exercises") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00695C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (selectedExercise != null && (isExercising || showCountdown || showResults)) {
            // Exercise in progress screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    if (showCountdown) {
                        // Pre-exercise countdown
                        Text(
                            countdownValue.toString(),
                            fontSize = 120.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else if (isExercising) {
                        // Exercise in progress
                        Text(
                            selectedExercise?.emoji ?: "",
                            fontSize = 80.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            selectedExercise?.name ?: "",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            selectedExercise?.instructions ?: "",
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(48.dp))

                        // Visual guide (pulsing circle)
                        val infiniteTransition = rememberInfiniteTransition()
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        Box(
                            modifier = Modifier.size(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(150.dp)) {
                                val radius = size.minDimension / 2 * scale
                                val time = System.currentTimeMillis() / 1000.0
                                val pulse = (sin(time * PI * 2) * 0.5 + 0.5).toFloat()
                                val exerciseColor = selectedExercise?.color ?: Color.Blue

                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            exerciseColor.copy(alpha = 0.6f),
                                            exerciseColor.copy(alpha = 0.1f)
                                        )
                                    ),
                                    radius = radius
                                )
                            }
                            Text(
                                countdown.toString(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { exerciseProgress },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = selectedExercise?.color ?: Color.Blue,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "${(exerciseProgress * 100).toInt()}%",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        Button(
                            onClick = {
                                isExercising = false
                                selectedExercise = null
                                exerciseProgress = 0f
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Text("Stop Exercise")
                        }
                    } else if (showResults) {
                        // Results screen
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Exercise Complete!",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Score: $exerciseScore/100",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                exerciseScore >= 85 -> Color(0xFF4CAF50)
                                exerciseScore >= 70 -> Color(0xFFFFC107)
                                else -> Color(0xFFFF9800)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            when {
                                exerciseScore >= 85 -> "Excellent! Your ${selectedExercise?.benefit?.lowercase() ?: "voice"} is improving!"
                                exerciseScore >= 70 -> "Good work! Keep practicing!"
                                else -> "Keep trying! Practice makes perfect."
                            },
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        Button(
                            onClick = {
                                showResults = false
                                selectedExercise = null
                                exerciseProgress = 0f
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00695C)
                            )
                        ) {
                            Text("Complete", fontSize = 18.sp)
                        }
                    }
                }
            }
        } else {
            // Exercise library
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress overview
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF00695C).copy(alpha = 0.1f)
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🏆",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "${completedExercises.size} Exercises",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00695C)
                                )
                                Text(
                                    "Keep up the great work!",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Exercise list
                item {
                    Text(
                        "Exercise Library",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(exercises) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        isCompleted = exercise.id in completedExercises,
                        onClick = {
                            selectedExercise = exercise
                            showCountdown = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: VoiceExercise,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = exercise.color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(exercise.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    exercise.emoji,
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Exercise info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        exercise.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (exercise.recommended) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("⭐", fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    exercise.benefit,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⏱️ ${exercise.duration}s",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        exercise.difficulty,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = exercise.color,
                        modifier = Modifier
                            .background(
                                exercise.color.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Status
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Try",
                    tint = exercise.color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}