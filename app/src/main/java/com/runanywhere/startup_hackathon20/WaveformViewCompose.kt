package com.runanywhere.startup_hackathon20

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WaveformView(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    onWaveformReady: (WaveformSurfaceView) -> Unit = {}
) {
    var waveformView by remember { mutableStateOf<WaveformSurfaceView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WaveformSurfaceView(context).also { view ->
                waveformView = view
                onWaveformReady(view)
            }
        },
        update = { view ->
            if (isRecording) {
                view.startDrawing()
            } else {
                view.stopDrawing()
                view.clearWaveform()
            }
        }
    )
}