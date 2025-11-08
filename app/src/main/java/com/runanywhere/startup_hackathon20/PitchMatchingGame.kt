package com.runanywhere.startup_hackathon20

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlue
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Interactive Pitch Matching Game
 * Real-time pitch detection game where users match target pitches
 */

enum class GameDifficulty(val displayName: String, val speed: Float, val tolerance: Float) {
    EASY("Easy - Slow Notes", 1.5f, 30f),
    MEDIUM("Medium - Moving", 2.5f, 20f),
    HARD("Hard - Fast Changes", 4.0f, 15f)
}

enum class GameSong(val displayName: String, val notes: List<Float>) {
    SCALE("Simple Scale", listOf(130f, 146f, 164f, 174f, 196f, 220f, 246f, 261f)),
    HAPPY_BIRTHDAY(
        "Happy Birthday",
        listOf(196f, 196f, 220f, 196f, 261f, 246f, 196f, 196f, 220f, 196f, 293f, 261f)
    ),
    TWINKLE(
        "Twinkle Twinkle",
        listOf(261f, 261f, 392f, 392f, 440f, 440f, 392f, 349f, 349f, 329f, 329f, 293f, 293f, 261f)
    ),
    WORKOUT("Vocal Workout", listOf(150f, 200f, 150f, 250f, 150f, 200f, 300f, 200f, 150f))
}

data class GameScore(
    val accuracy: Float,
    val perfectHits: Int,
    val goodHits: Int,
    val missedHits: Int,
    val totalNotes: Int
)

data class GameResult(
    val difficulty: GameDifficulty,
    val song: Any, // Can be GameSong or CustomSong
    val score: GameScore,
    val timestamp: Long = System.currentTimeMillis()
)

data class CustomSong(
    val id: String,
    val displayName: String,
    val notes: List<Float>,
    val uri: Uri
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PitchMatchingGameScreen(
    onBackClick: () -> Unit,
    context: Context
) {
    var gameState by remember { mutableStateOf<GameState>(GameState.Setup) }
    var selectedDifficulty by remember { mutableStateOf(GameDifficulty.EASY) }
    var selectedSong by remember { mutableStateOf(GameSong.SCALE) }
    var selectedCustomSong by remember { mutableStateOf<CustomSong?>(null) }
    var customSongs by remember { mutableStateOf<List<CustomSong>>(emptyList()) }
    var gameHistory by remember { mutableStateOf<List<GameResult>>(emptyList()) }
    var highScores by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var isProcessingAudio by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(
                context,
                "Permission granted! Please select audio file again.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(context, "Permission denied. Cannot import audio.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingAudio = true
            scope.launch {
                try {
                    val pitches =
                        AudioPitchExtractor.extractPitchSequence(context, uri, maxNotes = 30)

                    if (pitches != null && pitches.isNotEmpty()) {
                        val fileName = AudioPitchExtractor.getFileNameFromUri(context, uri)
                        val customSong = CustomSong(
                            id = "custom_${System.currentTimeMillis()}",
                            displayName = fileName,
                            notes = pitches,
                            uri = uri
                        )
                        customSongs = customSongs + customSong
                        selectedCustomSong = customSong

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "✓ Imported: $fileName (${pitches.size} notes)",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "✗ Failed to extract pitches from audio",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "✗ Error processing audio: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } finally {
                    isProcessingAudio = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎵 Pitch Matching Game") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE91E63),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF1A1A2E))
        ) {
            when (gameState) {
                is GameState.Setup -> {
                    SetupScreen(
                        selectedDifficulty = selectedDifficulty,
                        selectedSong = selectedSong,
                        selectedCustomSong = selectedCustomSong,
                        customSongs = customSongs,
                        lastResult = gameHistory.lastOrNull(),
                        isProcessingAudio = isProcessingAudio,
                        onDifficultyChange = { selectedDifficulty = it },
                        onSongChange = {
                            selectedSong = it
                            selectedCustomSong = null
                        },
                        onCustomSongChange = {
                            selectedCustomSong = it
                        },
                        onStartGame = {
                            gameState = if (selectedCustomSong != null) GameState.Playing(
                                selectedDifficulty,
                                selectedCustomSong!!
                            ) else GameState.Playing(selectedDifficulty, selectedSong)
                        },
                        onViewLastResult = {
                            gameHistory.lastOrNull()?.let { lastResult ->
                                gameState = GameState.Results(lastResult.score, fromHistory = true)
                            }
                        },
                        onImportCustomAudio = {
                            // Check permissions based on Android version
                            val permission =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Manifest.permission.READ_MEDIA_AUDIO
                                } else {
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                }

                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    permission
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                audioPickerLauncher.launch("audio/*")
                            } else {
                                permissionLauncher.launch(permission)
                            }
                        }
                    )
                }

                is GameState.Playing -> {
                    if ((gameState as GameState.Playing).song is CustomSong) {
                        GamePlayScreen(
                            difficulty = (gameState as GameState.Playing).difficulty,
                            song = (gameState as GameState.Playing).song as CustomSong,
                            context = context,
                            onGameComplete = { score ->
                                // Store in history
                                val result = GameResult(
                                    difficulty = selectedDifficulty,
                                    song = (gameState as GameState.Playing).song as CustomSong,
                                    score = score
                                )
                                gameHistory = gameHistory + result

                                // Update high score
                                val key = when (val s = (gameState as GameState.Playing).song) {
                                    is GameSong -> "${selectedDifficulty.name}_${s.name}"
                                    is CustomSong -> "${selectedDifficulty.name}_${s.id}"
                                    else -> "${selectedDifficulty.name}_unknown"
                                }
                                val current = highScores[key] ?: 0f
                                if (score.accuracy > current) {
                                    highScores = highScores + (key to score.accuracy)
                                }
                                gameState = GameState.Results(score, fromHistory = false)
                            }
                        )
                    } else {
                        GamePlayScreen(
                            difficulty = (gameState as GameState.Playing).difficulty,
                            song = (gameState as GameState.Playing).song as GameSong,
                            context = context,
                            onGameComplete = { score ->
                                // Store in history
                                val result = GameResult(
                                    difficulty = selectedDifficulty,
                                    song = (gameState as GameState.Playing).song as GameSong,
                                    score = score
                                )
                                gameHistory = gameHistory + result

                                // Update high score
                                val key = when (val s = (gameState as GameState.Playing).song) {
                                    is GameSong -> "${selectedDifficulty.name}_${s.name}"
                                    is CustomSong -> "${selectedDifficulty.name}_${s.id}"
                                    else -> "${selectedDifficulty.name}_unknown"
                                }
                                val current = highScores[key] ?: 0f
                                if (score.accuracy > current) {
                                    highScores = highScores + (key to score.accuracy)
                                }
                                gameState = GameState.Results(score, fromHistory = false)
                            }
                        )
                    }
                }

                is GameState.Results -> {
                    val resultsState = gameState as GameState.Results
                    ResultsScreen(
                        score = resultsState.score,
                        highScore = highScores["${selectedDifficulty.name}_${selectedSong.name}"]
                            ?: 0f,
                        fromHistory = resultsState.fromHistory,
                        onPlayAgain = {
                            gameState = GameState.Playing(selectedDifficulty, selectedSong)
                        },
                        onMainMenu = { gameState = GameState.Setup }
                    )
                }
            }
        }
    }
}

sealed class GameState {
    object Setup : GameState()
    data class Playing(val difficulty: GameDifficulty, val song: Any) :
        GameState() // Can be GameSong or CustomSong
    data class Results(val score: GameScore, val fromHistory: Boolean = false) : GameState()
}

@Composable
fun SetupScreen(
    selectedDifficulty: GameDifficulty,
    selectedSong: GameSong,
    selectedCustomSong: CustomSong?,
    customSongs: List<CustomSong>,
    lastResult: GameResult?,
    isProcessingAudio: Boolean,
    onDifficultyChange: (GameDifficulty) -> Unit,
    onSongChange: (GameSong) -> Unit,
    onCustomSongChange: (CustomSong?) -> Unit,
    onStartGame: () -> Unit,
    onViewLastResult: () -> Unit,
    onImportCustomAudio: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "🎤 Match the Pitch!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            "Sing or hum to match the moving target pitch.\nStay in the green zone for points!",
            fontSize = 14.sp,
            color = Color(0xFFB0B0B0),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable content using LazyColumn
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp) // Add bottom padding for scrollable content
        ) {
            // Difficulty Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Difficulty",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        GameDifficulty.values().forEach { difficulty ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedDifficulty == difficulty,
                                    onClick = { onDifficultyChange(difficulty) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFE91E63),
                                        unselectedColor = Color(0xFF7F8C8D)
                                    )
                                )
                                Text(
                                    difficulty.displayName,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Song Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Song / Pattern",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        GameSong.values().forEach { song ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = if (selectedSong == song && selectedCustomSong == null) Color(
                                    0xFFE91E63
                                ).copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                onClick = {
                                    onSongChange(song)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (selectedSong == song && selectedCustomSong == null) Icons.Default.CheckCircle else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (selectedSong == song && selectedCustomSong == null) Color(
                                            0xFFE91E63
                                        ) else Color(
                                            0xFF7F8C8D
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            song.displayName,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = if (selectedSong == song && selectedCustomSong == null) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            "${song.notes.size} notes",
                                            color = Color(0xFF7F8C8D),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        customSongs.forEach { customSong ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = if (selectedCustomSong == customSong) Color(0xFFE91E63).copy(
                                    alpha = 0.2f
                                ) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                onClick = { onCustomSongChange(customSong) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (selectedCustomSong == customSong) Icons.Default.CheckCircle else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (selectedCustomSong == customSong) Color(
                                            0xFFE91E63
                                        ) else Color(
                                            0xFF7F8C8D
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            customSong.displayName,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = if (selectedCustomSong == customSong) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            "${customSong.notes.size} notes",
                                            color = Color(0xFF7F8C8D),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Import Custom Audio
            item {
                Button(
                    onClick = onImportCustomAudio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE91E63)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessingAudio
                ) {
                    if (isProcessingAudio) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PROCESSING AUDIO...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Upload, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("IMPORT CUSTOM AUDIO", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start button - always visible at bottom
        Button(
            onClick = onStartGame,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE91E63)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("START GAME", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (lastResult != null) {
            OutlinedButton(
                onClick = onViewLastResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("VIEW LAST RESULT")
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun GamePlayScreen(
    difficulty: GameDifficulty,
    song: Any, // Can be GameSong or CustomSong
    context: Context,
    onGameComplete: (GameScore) -> Unit
) {
    var currentNoteIndex by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }
    var userPitch by remember { mutableStateOf(0f) }
    var isListening by remember { mutableStateOf(false) }
    var perfectHits by remember { mutableStateOf(0) }
    var goodHits by remember { mutableStateOf(0) }
    var missedHits by remember { mutableStateOf(0) }
    var pitchHistory by remember { mutableStateOf(listOf<Float>()) }

    val targetPitch = when (song) {
        is GameSong -> song.notes[currentNoteIndex]
        is CustomSong -> song.notes[currentNoteIndex]
        else -> 0f
    }
    val pitchDifference = abs(userPitch - targetPitch)
    val isMatchingPerfect = pitchDifference < difficulty.tolerance / 2f
    val isMatchingGood = pitchDifference < difficulty.tolerance

    // Pitch detection
    LaunchedEffect(Unit) {
        isListening = true

        withContext(Dispatchers.IO) {
            try {
                val bufferSize = AudioRecord.getMinBufferSize(
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                audioRecord.startRecording()
                val buffer = ShortArray(bufferSize)

                while (isListening && currentNoteIndex < when (song) {
                        is GameSong -> song.notes.size
                        is CustomSong -> song.notes.size
                        else -> 0
                    }
                ) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val detectedPitch = detectPitch(buffer, 44100)
                        if (detectedPitch > 80f && detectedPitch < 500f) {
                            withContext(Dispatchers.Main) {
                                userPitch = detectedPitch
                                pitchHistory = (pitchHistory + detectedPitch).takeLast(50)
                            }
                        }
                    }
                    delay(50)
                }

                audioRecord.stop()
                audioRecord.release()
            } catch (e: Exception) {
                Log.e("PitchGame", "Audio error: ${e.message}")
            }
        }
    }

    // Game progress
    LaunchedEffect(currentNoteIndex) {
        progress = 0f

        while (progress < 1f && currentNoteIndex < when (song) {
                is GameSong -> song.notes.size
                is CustomSong -> song.notes.size
                else -> 0
            }
        ) {
            delay(16) // 60 FPS
            progress += (difficulty.speed * 0.016f) / 2f // Each note lasts ~2 seconds / speed

            // Score the note when it's complete
            if (progress >= 0.95f && currentNoteIndex < when (song) {
                    is GameSong -> song.notes.size
                    is CustomSong -> song.notes.size
                    else -> 0
                }
            ) {
                when {
                    isMatchingPerfect -> perfectHits++
                    isMatchingGood -> goodHits++
                    else -> missedHits++
                }
            }
        }

        if (currentNoteIndex < when (song) {
                is GameSong -> song.notes.size - 1
                is CustomSong -> song.notes.size - 1
                else -> -1
            }
        ) {
            currentNoteIndex++
        } else {
            // Game complete
            isListening = false
            val totalNotes = when (song) {
                is GameSong -> song.notes.size
                is CustomSong -> song.notes.size
                else -> 0
            }
            val accuracy = ((perfectHits * 100f + goodHits * 70f) / totalNotes).coerceIn(0f, 100f)

            onGameComplete(
                GameScore(
                    accuracy = accuracy,
                    perfectHits = perfectHits,
                    goodHits = goodHits,
                    missedHits = missedHits,
                    totalNotes = totalNotes
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Score display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScoreChip("Perfect: $perfectHits", Color(0xFF4CAF50))
            ScoreChip("Good: $goodHits", Color(0xFFFFC107))
            ScoreChip("Miss: $missedHits", Color(0xFFF44336))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Note progress
        Text(
            "Note ${currentNoteIndex + 1} / ${
                when (song) {
                    is GameSong -> song.notes.size
                    is CustomSong -> song.notes.size
                    else -> 0
                }
            }",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        LinearProgressIndicator(
            progress = {
                (currentNoteIndex + 1).toFloat() / when (song) {
                    is GameSong -> song.notes.size
                    is CustomSong -> song.notes.size
                    else -> 1
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Color(0xFFE91E63),
            trackColor = Color(0xFF34495E)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main game canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PitchVisualization(
                    targetPitch = targetPitch,
                    userPitch = userPitch,
                    isMatching = isMatchingGood,
                    isPerfect = isMatchingPerfect,
                    progress = progress,
                    pitchHistory = pitchHistory
                )

                // Matching indicator
                if (isMatchingPerfect || isMatchingGood) {
                    Text(
                        if (isMatchingPerfect) "★ PERFECT! ★" else "✓ Good!",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 32.dp),
                        color = if (isMatchingPerfect) Color(0xFF4CAF50) else Color(0xFFFFC107),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pitch info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            PitchInfoCard("Target", "${targetPitch.toInt()} Hz", Color(0xFFE91E63))
            PitchInfoCard(
                "Your Voice",
                "${userPitch.toInt()} Hz",
                if (isMatchingGood) Color(0xFF4CAF50) else Color(0xFF7F8C8D)
            )
            PitchInfoCard("Difference", "${pitchDifference.toInt()} Hz", Color(0xFF3498DB))
        }
    }
}

@Composable
fun PitchVisualization(
    targetPitch: Float,
    userPitch: Float,
    isMatching: Boolean,
    isPerfect: Boolean,
    progress: Float,
    pitchHistory: List<Float>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Draw pitch range (80-400 Hz)
        val minPitch = 80f
        val maxPitch = 400f

        // Helper to convert pitch to Y position
        fun pitchToY(pitch: Float): Float {
            return centerY - ((pitch - minPitch) / (maxPitch - minPitch) - 0.5f) * height * 0.8f
        }

        // Draw grid lines
        for (i in 0..4) {
            val y = height * i / 4
            drawLine(
                color = Color(0xFF34495E),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Draw matching zone (green zone)
        val targetY = pitchToY(targetPitch)
        val zoneHeight = 40f

        drawRect(
            color = if (isPerfect) Color(0xFF4CAF50).copy(alpha = 0.3f)
            else Color(0xFFFFC107).copy(alpha = 0.2f),
            topLeft = Offset(0f, targetY - zoneHeight),
            size = androidx.compose.ui.geometry.Size(width, zoneHeight * 2)
        )

        // Draw target pitch line (moving)
        val targetX = width * progress
        drawCircle(
            color = Color(0xFFE91E63),
            radius = 20f,
            center = Offset(targetX, targetY)
        )

        // Draw target trail
        for (i in 0..10) {
            val trailX = targetX - i * 30f
            if (trailX > 0) {
                drawCircle(
                    color = Color(0xFFE91E63).copy(alpha = (1f - i / 10f) * 0.3f),
                    radius = 15f,
                    center = Offset(trailX, targetY)
                )
            }
        }

        // Draw user pitch line
        if (userPitch > 80f) {
            val userY = pitchToY(userPitch)

            // Draw history trail
            if (pitchHistory.size > 1) {
                val path = Path()
                val step = width / 50f
                path.moveTo(width - step * (pitchHistory.size - 1), pitchToY(pitchHistory.first()))

                pitchHistory.forEachIndexed { index, pitch ->
                    val x = width - step * (pitchHistory.size - 1 - index)
                    val y = pitchToY(pitch)
                    path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = if (isMatching) Color(0xFF4CAF50) else Color(0xFF3498DB),
                    style = Stroke(width = 4f)
                )
            }

            // Draw current position
            drawCircle(
                color = if (isMatching) Color(0xFF4CAF50) else Color(0xFF3498DB),
                radius = 16f,
                center = Offset(width * 0.9f, userY)
            )
        }

        // Draw pitch labels
        val labelPitches = listOf(100f, 150f, 200f, 250f, 300f, 350f)
        labelPitches.forEach { pitch ->
            val y = pitchToY(pitch)
            if (y > 20f && y < height - 20f) {
                drawLine(
                    color = Color(0xFF7F8C8D).copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(40f, y),
                    strokeWidth = 2f
                )
            }
        }
    }
}

@Composable
fun ScoreChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PitchInfoCard(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF7F8C8D), fontSize = 12.sp)
        Text(
            value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ResultsScreen(
    score: GameScore,
    highScore: Float,
    fromHistory: Boolean,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit
) {
    val isNewHighScore = score.accuracy >= highScore && highScore > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isNewHighScore) {
            Text(
                "🏆 NEW HIGH SCORE! 🏆",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            "Game Complete!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Big score
        Text(
            "${score.accuracy.toInt()}%",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                score.accuracy >= 90f -> Color(0xFF4CAF50)
                score.accuracy >= 70f -> Color(0xFFFFC107)
                else -> Color(0xFFFF9800)
            }
        )

        Text(
            "Accuracy",
            fontSize = 18.sp,
            color = Color(0xFFB0B0B0)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatRow("Perfect Hits", "${score.perfectHits}", Color(0xFF4CAF50))
                StatRow("Good Hits", "${score.goodHits}", Color(0xFFFFC107))
                StatRow("Missed", "${score.missedHits}", Color(0xFFF44336))
                Divider(color = Color(0xFF34495E))
                StatRow("Total Notes", "${score.totalNotes}", Color(0xFF3498DB))
                if (highScore > 0) {
                    StatRow("High Score", "${highScore.toInt()}%", Color(0xFFFFD700))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Feedback message
        Text(
            when {
                score.accuracy >= 95f -> "Outstanding! Perfect pitch control! 🌟"
                score.accuracy >= 85f -> "Excellent! Great pitch accuracy!"
                score.accuracy >= 70f -> "Good job! Keep practicing!"
                score.accuracy >= 50f -> "Nice try! Practice makes perfect!"
                else -> "Keep going! You'll improve with practice!"
            },
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Buttons
        if (!fromHistory) {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PLAY AGAIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = onMainMenu,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text("MAIN MENU")
        }
    }
}

@Composable
fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFB0B0B0), fontSize = 14.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// Simple pitch detection using autocorrelation
private fun detectPitch(audioData: ShortArray, sampleRate: Int): Float {
    val size = minOf(audioData.size, 2048)
    val buffer = audioData.take(size).map { it.toFloat() }.toFloatArray()

    // Autocorrelation
    val minLag = (sampleRate / 400f).toInt() // 400 Hz max
    val maxLag = (sampleRate / 80f).toInt()  // 80 Hz min

    var bestLag = 0
    var maxCorr = 0f

    for (lag in minLag..maxLag.coerceAtMost(size / 2)) {
        var corr = 0f
        for (i in 0 until size - lag) {
            corr += buffer[i] * buffer[i + lag]
        }
        if (corr > maxCorr) {
            maxCorr = corr
            bestLag = lag
        }
    }

    return if (bestLag > 0) sampleRate.toFloat() / bestLag else 0f
}

