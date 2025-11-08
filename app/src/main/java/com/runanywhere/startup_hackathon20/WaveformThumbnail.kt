package com.runanywhere.startup_hackathon20

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlue
import com.runanywhere.startup_hackathon20.ui.theme.VocalBlueLight
import kotlin.math.*

@Composable
fun WaveformThumbnail(
    waveformData: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = VocalBlue,
    backgroundColor: Color = VocalBlueLight.copy(alpha = 0.1f)
) {
    Box(
        modifier = modifier
            .size(width = 60.dp, height = 40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawWaveformThumbnail(
                waveformData = waveformData,
                color = color,
                size = size
            )
        }
    }
}

private fun DrawScope.drawWaveformThumbnail(
    waveformData: List<Float>,
    color: Color,
    size: androidx.compose.ui.geometry.Size
) {
    if (waveformData.isEmpty()) {
        // Draw a simple placeholder waveform
        drawPlaceholderWaveform(color, size)
        return
    }

    val width = size.width
    val height = size.height
    val centerY = height / 2f
    val maxAmplitude = height * 0.4f // Use 40% of height for amplitude

    // Sample the waveform data to fit the available width
    val sampledData = sampleWaveformData(waveformData, width.toInt())

    val path = Path()
    val stepX = width / sampledData.size.toFloat()

    // Draw the top half of the waveform
    for (i in sampledData.indices) {
        val x = i * stepX
        val amplitude = sampledData[i].coerceIn(0f, 1f)
        val y = centerY - (amplitude * maxAmplitude)

        if (i == 0) {
            path.moveTo(x, centerY)
        }
        path.lineTo(x, y)
    }

    // Draw the bottom half (mirrored)
    for (i in sampledData.indices.reversed()) {
        val x = i * stepX
        val amplitude = sampledData[i].coerceIn(0f, 1f)
        val y = centerY + (amplitude * maxAmplitude)
        path.lineTo(x, y)
    }

    path.close()

    drawPath(
        path = path,
        color = color
    )
}

private fun DrawScope.drawPlaceholderWaveform(
    color: Color,
    size: androidx.compose.ui.geometry.Size
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2f
    val maxAmplitude = height * 0.3f

    // Generate a simple sine wave pattern
    val points = 20
    val path = Path()

    for (i in 0..points) {
        val x = (i.toFloat() / points) * width
        val amplitude =
            sin(i.toFloat() * 0.5f) * maxAmplitude * (0.3f + 0.7f * sin(i.toFloat() * 0.2f))
        val y = centerY - amplitude

        if (i == 0) {
            path.moveTo(x, centerY)
        }
        path.lineTo(x, y)
    }

    // Mirror for bottom half
    for (i in points downTo 0) {
        val x = (i.toFloat() / points) * width
        val amplitude =
            sin(i.toFloat() * 0.5f) * maxAmplitude * (0.3f + 0.7f * sin(i.toFloat() * 0.2f))
        val y = centerY + amplitude
        path.lineTo(x, y)
    }

    path.close()

    drawPath(
        path = path,
        color = color.copy(alpha = 0.6f)
    )
}

private fun sampleWaveformData(data: List<Float>, targetWidth: Int): List<Float> {
    if (data.size <= targetWidth) return data

    val sampledData = mutableListOf<Float>()
    val step = data.size.toFloat() / targetWidth

    for (i in 0 until targetWidth) {
        val index = (i * step).toInt().coerceAtMost(data.size - 1)
        sampledData.add(data[index])
    }

    return sampledData
}

// Function to generate mock waveform data for existing recordings
fun generateMockWaveformData(duration: Int): List<Float> {
    val dataPoints = minOf(duration * 10, 500) // 10 points per second, max 500 points
    return (0 until dataPoints).map {
        val t = it.toFloat() / dataPoints
        // Generate a realistic-looking waveform with varying amplitudes
        val base = sin(t * PI.toFloat() * 2 * 3) * 0.5f + 0.5f // Base frequency
        val variation = sin(t * PI.toFloat() * 2 * 7) * 0.3f // Higher frequency variation
        val envelope = sin(t * PI.toFloat()) * 0.8f + 0.2f // Envelope to make it more natural
        ((base + variation) * envelope).coerceIn(0f, 1f)
    }
}