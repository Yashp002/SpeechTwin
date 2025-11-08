package com.runanywhere.startup_hackathon20

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlue
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlueLight
import kotlinx.coroutines.delay

@Composable
fun CircularCountdownTimer(
    modifier: Modifier = Modifier,
    totalTimeSeconds: Int = 10,
    isRunning: Boolean,
    onTimeUpdate: (Int) -> Unit = {},
    onComplete: () -> Unit = {}
) {
    var timeRemaining by remember { mutableStateOf(totalTimeSeconds) }
    var progress by remember { mutableStateOf(0f) }

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100),
        label = "progress"
    )

    LaunchedEffect(isRunning) {
        if (isRunning) {
            try {
                timeRemaining = totalTimeSeconds
                progress = 0f

                for (i in 0..totalTimeSeconds * 100) {
                    if (!isRunning) {
                        Log.d("CircularCountdownTimer", "Timer stopped by external state")
                        break
                    }

                    val currentTime = totalTimeSeconds - (i / 100f)
                    val newTimeRemaining = (currentTime.toInt() + 1).coerceAtLeast(0)

                    if (newTimeRemaining != timeRemaining) {
                        timeRemaining = newTimeRemaining
                        try {
                            onTimeUpdate(timeRemaining)
                        } catch (e: Exception) {
                            Log.e(
                                "CircularCountdownTimer",
                                "Error in onTimeUpdate callback: ${e.message}"
                            )
                        }
                    }

                    progress = (i / (totalTimeSeconds * 100f)).coerceIn(0f, 1f)

                    if (i >= totalTimeSeconds * 100) {
                        Log.d("CircularCountdownTimer", "Timer completed naturally")
                        try {
                            onComplete()
                        } catch (e: Exception) {
                            Log.e(
                                "CircularCountdownTimer",
                                "Error in onComplete callback: ${e.message}"
                            )
                        }
                        break
                    }

                    delay(10)
                }
            } catch (e: Exception) {
                Log.e("CircularCountdownTimer", "Error in countdown timer: ${e.message}")
                // Ensure we call onComplete even if there's an error
                try {
                    onComplete()
                } catch (completionError: Exception) {
                    Log.e(
                        "CircularCountdownTimer",
                        "Error in error recovery onComplete: ${completionError.message}"
                    )
                }
            }
        } else {
            // Reset when not running
            timeRemaining = totalTimeSeconds
            progress = 0f
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            try {
                drawCircularProgress(
                    progress = animatedProgress,
                    size = size
                )
            } catch (e: Exception) {
                Log.e("CircularCountdownTimer", "Error drawing progress: ${e.message}")
            }
        }

        if (isRunning && timeRemaining >= 0) {
            Text(
                text = timeRemaining.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VocalBlue
            )
        }
    }
}

private fun DrawScope.drawCircularProgress(
    progress: Float,
    size: Size
) {
    val strokeWidth = 8.dp.toPx()
    val radius = (minOf(size.width, size.height) - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)

    if (radius <= 0 || center.x <= 0 || center.y <= 0) {
        Log.w("CircularCountdownTimer", "Invalid drawing dimensions")
        return
    }

    // Background circle
    drawCircle(
        color = VocalBlueLight.copy(alpha = 0.3f),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Progress arc
    val sweepAngle = 360f * progress.coerceIn(0f, 1f)
    if (sweepAngle > 0) {
        drawArc(
            color = VocalBlue,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(
                center.x - radius,
                center.y - radius
            ),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}