package com.runanywhere.startup_hackathon20

import java.io.File
import java.util.*

data class RecordingItem(
    val id: String,
    val file: File,
    var name: String,
    val originalName: String,
    val timestamp: Long,
    val duration: Int, // in seconds
    val sizeBytes: Long,
    val sampleRate: Int = 44100,
    val format: String = "16-bit PCM WAV",
    var vocalHealthScore: Int = (70..95).random(), // Mock score for now
    var isFavorite: Boolean = false,
    var notes: String = "",
    var waveformData: List<Float> = emptyList(), // For thumbnail generation
    var isBaseline: Boolean = false
) {
    // Use the health score directly
    val actualHealthScore: Int
        get() = vocalHealthScore

    val clinicalInterpretation: String
        get() = when (actualHealthScore) {
            in 90..100 -> "Excellent voice quality"
            in 80..89 -> "Good voice quality"
            in 70..79 -> "Fair voice quality - minor irregularities"
            in 60..69 -> "Concerning voice quality - consider professional assessment"
            else -> "Poor voice quality - recommend professional evaluation"
        }

    val formattedTimestamp: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val calendar = Calendar.getInstance()
            val recordingCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }

            return when {
                diff < 24 * 60 * 60 * 1000 &&
                        calendar.get(Calendar.DAY_OF_YEAR) == recordingCalendar.get(Calendar.DAY_OF_YEAR) -> {
                    "Today, ${formatTime(timestamp)}"
                }

                diff < 48 * 60 * 60 * 1000 -> "Yesterday, ${formatTime(timestamp)}"
                diff < 7 * 24 * 60 * 60 * 1000 -> "${(diff / (24 * 60 * 60 * 1000)).toInt()} days ago"
                else -> formatDate(timestamp)
            }
        }

    val formattedDuration: String
        get() {
            val minutes = duration / 60
            val seconds = duration % 60
            return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
        }

    val formattedSize: String
        get() = String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0))

    val healthScoreColor: androidx.compose.ui.graphics.Color
        get() = when {
            actualHealthScore >= 90 -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
            actualHealthScore >= 80 -> androidx.compose.ui.graphics.Color(0xFF8BC34A) // Light Green
            actualHealthScore >= 70 -> androidx.compose.ui.graphics.Color(0xFFFFC107) // Amber
            actualHealthScore >= 60 -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
            else -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
        }

    private fun formatTime(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        return String.format("%d:%02d %s", displayHour, minute, amPm)
    }

    private fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format(
            "%s %d, %d",
            calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.YEAR)
        )
    }
}

enum class SortOption(val displayName: String) {
    DATE_NEWEST("Date (Newest)"),
    DATE_OLDEST("Date (Oldest)"),
    HEALTH_SCORE_BEST("Health Score (Best)"),
    HEALTH_SCORE_WORST("Health Score (Worst)"),
    DURATION_LONGEST("Duration (Longest)"),
    DURATION_SHORTEST("Duration (Shortest)")
}

enum class FilterOption(val displayName: String) {
    ALL("All Recordings"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    FAVORITES("Favorites Only"),
    HIGH_SCORES("High Scores (80+)"),
    BASELINES("Baseline Recordings")
}

data class RecordingComparison(
    val recording1: RecordingItem,
    val recording2: RecordingItem,
    val scoreDifference: Int = recording2.vocalHealthScore - recording1.vocalHealthScore,
    val durationDifference: Int = recording2.duration - recording1.duration,
    val timeDifference: Long = recording2.timestamp - recording1.timestamp
)

