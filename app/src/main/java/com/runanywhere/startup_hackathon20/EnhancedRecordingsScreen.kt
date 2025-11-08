package com.runanywhere.startup_hackathon20

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.text.format.DateUtils.isToday
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlue
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlueLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedRecordingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recordings by remember { mutableStateOf<List<RecordingItem>>(emptyList()) }
    var filteredRecordings by remember { mutableStateOf<List<RecordingItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var currentSortOption by remember { mutableStateOf(SortOption.DATE_NEWEST) }
    var currentFilterOption by remember { mutableStateOf(FilterOption.ALL) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var isCompareModeActive by remember { mutableStateOf(false) }
    var selectedForComparison by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showComparisonDialog by remember { mutableStateOf(false) }
    var expandedPlayerId by remember { mutableStateOf<String?>(null) }
    var expandedAnalysisId by remember { mutableStateOf<String?>(null) }
    var currentlyPlaying by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playbackProgress by remember { mutableStateOf(0) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var showDeleteSnackbar by remember { mutableStateOf<RecordingItem?>(null) }
    var recentlyDeleted by remember { mutableStateOf<RecordingItem?>(null) }
    var showClearOldDialog by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<VoiceAnalyzer.AnalysisResult?>(null) }
    var showAnalysisDialog by remember { mutableStateOf(false) }
    var currentAnalyzingRecording by remember { mutableStateOf<RecordingItem?>(null) }

    // Hide navigation bar and make app fullscreen
    val activity = context as? ComponentActivity
    LaunchedEffect(Unit) {
        activity?.let { act ->
            act.window.statusBarColor = android.graphics.Color.TRANSPARENT
            act.window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                act.window.setDecorFitsSystemWindows(false)
                act.window.insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                act.window.decorView.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }
        }
    }

    // Cleanup MediaPlayer on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Load recordings - simplified without analysis data
    LaunchedEffect(Unit) {
        recordings = loadRecordingsFromCache(context)
        filteredRecordings = recordings
        isLoading = false
    }

    // Apply filters and search
    LaunchedEffect(recordings, searchQuery, currentSortOption, currentFilterOption) {
        filteredRecordings = recordings
            .filter { recording ->
                // Enhanced search filter - search in recording name, original filename, timestamp, and notes
                if (searchQuery.isNotBlank()) {
                    val query = searchQuery.lowercase().trim()
                    // Search in multiple fields for better discoverability
                    recording.name.lowercase().contains(query) ||
                            recording.originalName.lowercase().contains(query) ||
                            recording.formattedTimestamp.lowercase().contains(query) ||
                            recording.notes.lowercase().contains(query) ||
                            // Also search in formatted duration (e.g., "2m 15s")
                            recording.formattedDuration.lowercase().contains(query) ||
                            // Search in vocal health score
                            recording.vocalHealthScore.toString().contains(query)
                } else true
            }
            .filter { recording ->
                // Apply category filter
                when (currentFilterOption) {
                    FilterOption.ALL -> true
                    FilterOption.TODAY -> isToday(recording.timestamp)
                    FilterOption.THIS_WEEK -> isThisWeek(recording.timestamp)
                    FilterOption.THIS_MONTH -> isThisMonth(recording.timestamp)
                    FilterOption.FAVORITES -> recording.isFavorite
                    FilterOption.HIGH_SCORES -> recording.vocalHealthScore >= 80
                    FilterOption.BASELINES -> recording.isBaseline
                }
            }
            .sortedWith { a, b ->
                // Apply sorting
                when (currentSortOption) {
                    SortOption.DATE_NEWEST -> b.timestamp.compareTo(a.timestamp)
                    SortOption.DATE_OLDEST -> a.timestamp.compareTo(b.timestamp)
                    SortOption.HEALTH_SCORE_BEST -> b.vocalHealthScore.compareTo(a.vocalHealthScore)
                    SortOption.HEALTH_SCORE_WORST -> a.vocalHealthScore.compareTo(b.vocalHealthScore)
                    SortOption.DURATION_LONGEST -> b.duration.compareTo(a.duration)
                    SortOption.DURATION_SHORTEST -> a.duration.compareTo(b.duration)
                }
            }
    }

    // Playback progress tracking
    LaunchedEffect(currentlyPlaying, mediaPlayer) {
        if (currentlyPlaying != null && mediaPlayer?.isPlaying == true) {
            while (mediaPlayer?.isPlaying == true) {
                playbackProgress = mediaPlayer?.currentPosition ?: 0
                delay(100)
            }
            // Auto-reset when playback completes
            if (mediaPlayer?.isPlaying == false) {
                currentlyPlaying = null
                playbackProgress = 0
            }
        }
    }

    // Auto-dismiss snackbar
    LaunchedEffect(showDeleteSnackbar) {
        if (showDeleteSnackbar != null) {
            delay(5000) // 5 seconds
            showDeleteSnackbar = null
            recentlyDeleted = null
        }
    }

    // Function to handle play/pause with proper state management
    fun handlePlayPause(recording: RecordingItem) {
        if (currentlyPlaying == recording.id) {
            // Pause/Resume current playback
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.start()
                }
            }
        } else {
            // Stop any existing playback first
            mediaPlayer?.release()
            mediaPlayer = null
            currentlyPlaying = null
            playbackProgress = 0

            // Start new playback
            try {
                val newMediaPlayer = MediaPlayer().apply {
                    setDataSource(recording.file.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        currentlyPlaying = null
                        playbackProgress = 0
                        release()
                        mediaPlayer = null
                    }
                }
                mediaPlayer = newMediaPlayer
                currentlyPlaying = recording.id
            } catch (e: Exception) {
                Toast.makeText(context, "Error playing audio: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Function to clear old recordings
    fun clearOldRecordings() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7) // Older than 7 days
        val cutoffTime = calendar.timeInMillis

        val oldRecordings = recordings.filter { it.timestamp < cutoffTime }

        if (oldRecordings.isNotEmpty()) {
            // Delete files
            oldRecordings.forEach { recording ->
                try {
                    recording.file.delete()
                } catch (e: Exception) {
                    Log.e("ClearOld", "Error deleting file: ${e.message}")
                }
            }

            // Update recordings list
            recordings = recordings.filter { it.timestamp >= cutoffTime }

            Toast.makeText(
                context,
                "Deleted ${oldRecordings.size} old recordings",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "No old recordings found (older than 7 days)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Main UI with proper padding for system bars and fixed bottom suggestion
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isCompareModeActive) {
                                "Compare Mode (${selectedForComparison.size}/2)"
                            } else {
                                "My Recordings"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        if (isCompareModeActive) {
                            TextButton(
                                onClick = {
                                    isCompareModeActive = false
                                    selectedForComparison = emptySet()
                                }
                            ) {
                                Text("Cancel", color = VocalBlue)
                            }
                        } else {
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.Default.List, "Sort")
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    SortOption.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.displayName,
                                                    color = if (option == currentSortOption) VocalBlue else Color.Unspecified
                                                )
                                            },
                                            onClick = {
                                                currentSortOption = option
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Box {
                                IconButton(onClick = { showFilterMenu = true }) {
                                    Icon(Icons.Default.Settings, "Filter")
                                }

                                DropdownMenu(
                                    expanded = showFilterMenu,
                                    onDismissRequest = { showFilterMenu = false }
                                ) {
                                    FilterOption.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.displayName,
                                                    color = if (option == currentFilterOption) VocalBlue else Color.Unspecified
                                                )
                                            },
                                            onClick = {
                                                currentFilterOption = option
                                                showFilterMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { isCompareModeActive = true }) {
                                Icon(Icons.Default.CheckCircle, "Compare")
                            }
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search Bar
                if (!isCompareModeActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search recordings...") },
                        placeholder = { Text("Type recording name...") },
                        leadingIcon = { Icon(Icons.Default.Search, "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, "Clear search")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true
                    )
                }

                // Content
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = VocalBlue)
                    }
                } else if (filteredRecordings.isEmpty()) {
                    EmptyStateView(
                        message = if (searchQuery.isNotBlank()) {
                            "No recordings found for \"$searchQuery\""
                        } else {
                            "No recordings match your filters"
                        },
                        onClearFilters = {
                            searchQuery = ""
                            currentFilterOption = FilterOption.ALL
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 120.dp // Add extra bottom padding for fixed suggestion card
                        )
                    ) {
                        items(
                            filteredRecordings,
                            key = { it.id }
                        ) { recording ->
                            RecordingCard(
                                recording = recording,
                                isCompareModeActive = isCompareModeActive,
                                isSelected = selectedForComparison.contains(recording.id),
                                isPlaying = currentlyPlaying == recording.id,
                                isExpanded = expandedPlayerId == recording.id,
                                playbackProgress = if (currentlyPlaying == recording.id) playbackProgress else 0,
                                playbackSpeed = playbackSpeed,
                                mediaPlayer = mediaPlayer,
                                onTap = {
                                    if (isCompareModeActive) {
                                        if (selectedForComparison.contains(recording.id)) {
                                            selectedForComparison =
                                                selectedForComparison - recording.id
                                        } else if (selectedForComparison.size < 2) {
                                            selectedForComparison =
                                                selectedForComparison + recording.id
                                            if (selectedForComparison.size == 2) {
                                                showComparisonDialog = true
                                            }
                                        }
                                    } else {
                                        handlePlayPause(recording)
                                    }
                                },
                                onLongPress = { action ->
                                    when (action) {
                                        "rename" -> {
                                            // Handle rename
                                        }

                                        "delete" -> {
                                            // Handle delete with confirmation
                                        }

                                        "share" -> {
                                            shareRecording(recording, context)
                                        }

                                        "analyze" -> {
                                            currentAnalyzingRecording = recording
                                            isAnalyzing = true
                                            scope.launch {
                                                val analyzer = VoiceAnalyzer()
                                                analysisResult = withContext(Dispatchers.IO) {
                                                    analyzer.analyze(recording.file.absolutePath)
                                                }
                                                isAnalyzing = false
                                                showAnalysisDialog = true
                                            }
                                        }

                                        "baseline" -> {
                                            recording.isBaseline = !recording.isBaseline
                                            Toast.makeText(
                                                context,
                                                if (recording.isBaseline) "Set as baseline" else "Removed from baseline",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                onSwipeLeft = {
                                    // Delete with undo
                                    showDeleteSnackbar = recording
                                    recentlyDeleted = recording
                                    recordings = recordings.filter { it.id != recording.id }
                                },
                                onSwipeRight = {
                                    // Handle swipe right
                                },
                                onExpandPlayer = {
                                    expandedPlayerId =
                                        if (expandedPlayerId == recording.id) null else recording.id
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Storage info as last item in the list
                        item {
                            StorageInfoFooter(
                                recordingCount = recordings.size,
                                totalSize = recordings.sumOf { it.sizeBytes },
                                onClearOld = {
                                    showClearOldDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Fixed Smart Suggestion at the bottom
        SmartSuggestionsCard(
            recordings = recordings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Clear Old Recordings Dialog
    if (showClearOldDialog) {
        AlertDialog(
            onDismissRequest = { showClearOldDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Warning",
                    tint = Color.Red
                )
            },
            title = { Text("Clear Old Recordings") },
            text = {
                Text("This will delete all recordings older than 7 days. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearOldRecordings()
                        showClearOldDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearOldDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Comparison Dialog
    if (showComparisonDialog) {
        val selectedRecordings = recordings.filter { selectedForComparison.contains(it.id) }
        if (selectedRecordings.size == 2) {
            ComparisonDialog(
                recording1 = selectedRecordings[0],
                recording2 = selectedRecordings[1],
                onDismiss = {
                    showComparisonDialog = false
                    isCompareModeActive = false
                    selectedForComparison = emptySet()
                }
            )
        }
    }

    // Analysis Dialog
    if (showAnalysisDialog) {
        AlertDialog(
            onDismissRequest = { showAnalysisDialog = false },
            title = {
                Text(
                    "Voice Analysis",
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
                                    "Analyzing ${currentAnalyzingRecording?.name}...",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        analysisResult?.let { result ->
                            currentAnalyzingRecording?.let { recording ->
                                // Health Score Display
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { 1f },
                                        modifier = Modifier.size(100.dp),
                                        strokeWidth = 10.dp,
                                        trackColor = Color.LightGray.copy(alpha = 0.3f),
                                        color = Color.Transparent
                                    )
                                    CircularProgressIndicator(
                                        progress = { result.healthScore / 100f },
                                        modifier = Modifier.size(100.dp),
                                        strokeWidth = 10.dp,
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
                                            fontSize = 32.sp,
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
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Interpretation
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
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Metrics
                                Text(
                                    "Voice Metrics",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pitch", fontSize = 13.sp, color = Color.Gray)
                                    Text(
                                        "${String.format("%.1f", result.pitch)} Hz",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Loudness", fontSize = 13.sp, color = Color.Gray)
                                    Text(
                                        "${String.format("%.1f", result.loudness)} dB",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Jitter", fontSize = 13.sp, color = Color.Gray)
                                    Text(
                                        "${String.format("%.2f", result.jitter)}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Shimmer", fontSize = 13.sp, color = Color.Gray)
                                    Text(
                                        "${String.format("%.2f", result.shimmer)}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
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