package com.runanywhere.startup_hackathon20

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.ui.theme.*

/**
 * Comprehensive Help & Tutorial System
 * Shows all features with tutorials and support contact
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpBottomSheet(
    onDismiss: () -> Unit,
    context: Context
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "How to Use SpeechTwin",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Master all features step-by-step",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                }
            }

            // Feature 1: Voice Recording
            item {
                FeatureCard(
                    icon = Icons.Default.Mic,
                    iconColor = Color(0xFFE91E63),
                    title = "Voice Recording",
                    description = "Record your voice for 10 seconds. Speak naturally in a quiet environment. The app captures high-quality audio for analysis.",
                    details = listOf(
                        "Find a quiet room with minimal background noise",
                        "Hold phone 6-8 inches from your mouth",
                        "Speak at your normal volume and pace",
                        "Try reading a sentence or counting numbers"
                    )
                )
            }

            // Feature 2: Voice Analysis
            item {
                FeatureCard(
                    icon = Icons.Default.Analytics,
                    iconColor = PrimaryBlue,
                    title = "Voice Analysis",
                    description = "Get detailed voice health metrics after each recording:",
                    details = listOf(
                        "• Pitch: Your fundamental frequency (Hz)",
                        "• Loudness: Volume level in decibels",
                        "• Jitter: Pitch variation (lower is better)",
                        "• Shimmer: Amplitude variation (lower is better)",
                        "• Health Score: Overall vocal health (0-100)"
                    )
                )
            }

            // Feature 3: Real-time Waveform
            item {
                FeatureCard(
                    icon = Icons.Default.GraphicEq,
                    iconColor = AccentTeal,
                    title = "Real-time Waveform",
                    description = "See live audio visualization during recording. Colored bars show your voice amplitude and help you speak at optimal volume.",
                    details = listOf(
                        "Blue bars: Quiet/normal volume",
                        "Cyan bars: Good volume level",
                        "Orange/Red bars: Loud voice (reduce volume)"
                    )
                )
            }

            // Feature 4: Smart Insights
            item {
                FeatureCard(
                    icon = Icons.Default.Lightbulb,
                    iconColor = Color(0xFFFFC107),
                    title = "Smart Insights",
                    description = "Personalized recommendations based on your results, tips to improve vocal health, and trend analysis.",
                    details = listOf(
                        "Context-aware suggestions for your voice type",
                        "Daily tips based on time of day",
                        "Progress tracking over time"
                    )
                )
            }

            // Feature 5: Pitch Matching Game
            item {
                FeatureCard(
                    icon = Icons.Default.VideogameAsset,
                    iconColor = Color(0xFFFF5252),
                    title = "Pitch Matching Game",
                    description = "Fun interactive exercise to improve pitch control. Follow the moving line with your voice and get scored on accuracy.",
                    details = listOf(
                        "Choose difficulty: Easy, Medium, or Hard",
                        "Pick a song pattern or melody",
                        "Sing/hum to match the target pitch",
                        "Stay in the green zone for perfect scores!"
                    )
                )
            }

            // Feature 6: Voice Exercises
            item {
                FeatureCard(
                    icon = Icons.Default.FitnessCenter,
                    iconColor = Color(0xFF00695C),
                    title = "Voice Exercises",
                    description = "Guided vocal exercises for different goals. Includes humming, sustained vowels, lip trills, and more.",
                    details = listOf(
                        "5 professional exercises for vocal health",
                        "Visual countdown and progress tracking",
                        "Completion scoring and feedback",
                        "Daily practice recommendations"
                    )
                )
            }

            // Feature 7: Progress Dashboard
            item {
                FeatureCard(
                    icon = Icons.Default.Dashboard,
                    iconColor = Color(0xFF6200EA),
                    title = "Progress Dashboard",
                    description = "View your voice health trends over time, track improvements and patterns, see statistics and best scores.",
                    details = listOf(
                        "7-day health trend chart",
                        "Recording and exercise counters",
                        "Best score tracking",
                        "Set custom health goals"
                    )
                )
            }

            // Feature 8: 3D Waveform Visualization
            item {
                FeatureCard(
                    icon = Icons.Default.ViewInAr,
                    iconColor = Color(0xFF00BCD4),
                    title = "3D Waveform Visualization",
                    description = "MATLAB-style 3D graph of your voice signal. Interactive: Rotate, zoom, and explore your pitch in 3D space.",
                    details = listOf(
                        "Real-time 3D sine/cosine waveform",
                        "Drag to rotate, pinch to zoom",
                        "Record new audio to update visualization",
                        "Playback your recording with ▶️ button"
                    )
                )
            }

            // Feature 9: AI Healthy Voice
            item {
                FeatureCard(
                    icon = Icons.Default.AutoAwesome,
                    iconColor = Color(0xFF9C27B0),
                    title = "AI Healthy Voice Suggestion",
                    description = "When your voice shows strain, AI generates a 'healthier' version so you can hear your goal!",
                    details = listOf(
                        "Automatically triggers for low health scores",
                        "Before/After playback comparison",
                        "Shows what healthy voice sounds like",
                        "Motivational goal setting"
                    )
                )
            }

            // Quick Tips Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9C4)
                    ),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TipsAndUpdates,
                                contentDescription = null,
                                tint = Color(0xFFFF6F00),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Quick Tips",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TipItem("💧 Stay hydrated for best vocal health")
                        TipItem("🌅 Record in the morning for consistent results")
                        TipItem("🔇 Find a quiet place for accurate analysis")
                        TipItem("🎯 Practice daily for best improvement")
                        TipItem("🎤 Hold phone 6-8 inches from mouth")
                    }
                }
            }

            // Support Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryBlue.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Need Help?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Having issues or suggestions?",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Contact our support team:",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("yashrajpala8@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, "SpeechTwin Support Request")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Hi SpeechTwin Team,\n\n" +
                                                "I need help with:\n\n" +
                                                "[Describe your issue or feedback here]\n\n" +
                                                "---\n" +
                                                "App Version: 1.0\n" +
                                                "Device: Android"
                                    )
                                }
                                try {
                                    context.startActivity(
                                        Intent.createChooser(
                                            intent,
                                            "Send Email"
                                        )
                                    )
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "No email app found",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("yashrajpala8@gmail.com", fontSize = 14.sp)
                        }
                    }
                }
            }

            // App Info Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "SpeechTwin v1.0",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Made with ❤️ for RunAnywhere Hackathon",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "All data stored locally on your device",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    details: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = iconColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                description,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Divider(modifier = Modifier.padding(bottom = 12.dp))
                    details.forEach { detail ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            if (!detail.startsWith("•")) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                detail,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TipItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            color = Color(0xFF5D4037),
            lineHeight = 18.sp
        )
    }
}