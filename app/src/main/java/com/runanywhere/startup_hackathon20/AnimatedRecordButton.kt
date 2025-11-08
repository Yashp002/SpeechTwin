package com.runanywhere.startup_hackathon20

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlue

@Composable
fun AnimatedRecordButton(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    // Pulsing animation when recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Size animation
    val animatedSize by animateDpAsState(
        targetValue = if (isRecording) 140.dp else 120.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "size"
    )

    // Color animation
    val animatedColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Gray
            isRecording -> Color(0xFF4CAF50)
            else -> VocalBlue
        },
        animationSpec = tween(300),
        label = "color"
    )

    // Elevation animation
    val animatedElevation by animateDpAsState(
        targetValue = if (isRecording) 16.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "elevation"
    )

    // Icon size animation
    val animatedIconSize by animateDpAsState(
        targetValue = if (isRecording) 56.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "iconSize"
    )

    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .size(animatedSize)
            .scale(if (isRecording && enabled) pulseScale else 1f)
            .clip(CircleShape),
        containerColor = animatedColor,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = animatedElevation
        )
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
            modifier = Modifier.size(animatedIconSize)
        )
    }
}