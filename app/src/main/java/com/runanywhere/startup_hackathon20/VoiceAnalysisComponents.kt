package com.runanywhere.startup_hackathon20

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlue
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlueLight

// Progress Dialog for Analysis
@Composable
fun VoiceAnalysisProgressDialog(
    isVisible: Boolean,
    progressText: String,
    progress: Float,
    onDismiss: () -> Unit = {}
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = VocalBlue
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Analyzing your voice...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Extracting biomarkers",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = VocalBlue,
                        trackColor = VocalBlueLight.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = progressText,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Simplified Analysis Results Card - no longer uses complex analysis data
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAnalysisCard(
    recording: RecordingItem,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onReAnalyze: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = VocalBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice Analysis",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Health score badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                recording.healthScoreColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = recording.vocalHealthScore.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            // Simple analysis results
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                SimpleAnalysisResultsContent(
                    recording = recording,
                    onReAnalyze = onReAnalyze
                )
            }
        }
    }
}

@Composable
fun SimpleAnalysisResultsContent(
    recording: RecordingItem,
    onReAnalyze: () -> Unit
) {
    Column {
        // Large Health Score with Circular Progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background circle
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(100.dp),
                strokeWidth = 10.dp,
                trackColor = Color.LightGray.copy(alpha = 0.3f),
                color = Color.Transparent
            )
            // Progress circle
            CircularProgressIndicator(
                progress = { recording.vocalHealthScore / 100f },
                modifier = Modifier.size(100.dp),
                strokeWidth = 10.dp,
                color = when {
                    recording.vocalHealthScore >= 80 -> Color(0xFF4CAF50)
                    recording.vocalHealthScore >= 60 -> Color(0xFFFFC107)
                    recording.vocalHealthScore >= 40 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                trackColor = Color.Transparent
            )
            // Score text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${recording.vocalHealthScore}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        recording.vocalHealthScore >= 80 -> Color(0xFF4CAF50)
                        recording.vocalHealthScore >= 60 -> Color(0xFFFFC107)
                        recording.vocalHealthScore >= 40 -> Color(0xFFFF9800)
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

        // Clinical interpretation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (recording.vocalHealthScore) {
                    in 80..100 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    in 60..79 -> Color(0xFFFFC107).copy(alpha = 0.1f)
                    in 40..59 -> Color(0xFFFF9800).copy(alpha = 0.1f)
                    else -> Color(0xFFF44336).copy(alpha = 0.1f)
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Clinical Assessment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recording.clinicalInterpretation,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Metrics with Icons
        Text(
            text = "Voice Metrics",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // File Info Metrics (since we don't have actual analysis data in RecordingItem)
        MetricRowWithIcon(
            icon = Icons.Default.Face,
            name = "Health Score",
            value = "${recording.vocalHealthScore}%"
        )
        MetricRowWithIcon(
            icon = Icons.Default.DateRange,
            name = "Duration",
            value = recording.formattedDuration
        )
        MetricRowWithIcon(
            icon = Icons.Default.Info,
            name = "File Size",
            value = recording.formattedSize
        )
        MetricRowWithIcon(
            icon = Icons.Default.Star,
            name = "Sample Rate",
            value = "${recording.sampleRate} Hz"
        )
        MetricRowWithIcon(
            icon = Icons.Default.Info,
            name = "Format",
            value = recording.format
        )
        MetricRowWithIcon(
            icon = Icons.Default.DateRange,
            name = "Recorded",
            value = recording.formattedTimestamp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReAnalyze,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Re-analyze")
            }

            Button(
                onClick = { /* TODO: Export or share analysis */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share")
            }
        }
    }
}

@Composable
fun MetricRowWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = VocalBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MetricRow(name: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = 13.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Shimmer loading effect
@Composable
fun ShimmerAnalysisPlaceholder() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.7f at 500
            },
            repeatMode = RepeatMode.Reverse
        ), label = "shimmer_alpha"
    )

    val shimmerColorStops = arrayOf(
        0.0f to Color.LightGray.copy(alpha = alpha),
        0.5f to Color.LightGray.copy(alpha = alpha * 0.5f),
        1.0f to Color.LightGray.copy(alpha = alpha)
    )

    val brush = Brush.linearGradient(
        colorStops = shimmerColorStops,
        start = Offset(0f, 0f),
        end = Offset(300f, 300f)
    )

    Column {
        // Shimmer for metrics
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }
    }
}