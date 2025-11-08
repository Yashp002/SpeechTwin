package com.runanywhere.startup_hackathon20

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.*

class WaveformSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    companion object {
        private const val TAG = "WaveformSurfaceView"
    }

    private var drawingThread: Thread? = null
    @Volatile
    private var isDrawing = false
    private val amplitudeBuffer = mutableListOf<Float>()
    private val maxBufferSize = 300 // Number of amplitude points to keep
    private val bufferLock = Any() // Thread safety for amplitudeBuffer

    private val waveformPaint = Paint().apply {
        color = Color.parseColor("#2196F3") // VocalBlue
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#F5F5F5")
        style = Paint.Style.FILL
    }

    private val centerLinePaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "Surface created")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "Surface changed: ${width}x${height}")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "Surface destroyed")
        stopDrawing()
    }

    fun startDrawing() {
        if (!isDrawing && holder.surface.isValid) {
            Log.d(TAG, "Starting drawing")
            isDrawing = true
            drawingThread = Thread(::drawingLoop, "WaveformDrawingThread").apply {
                priority = Thread.NORM_PRIORITY
                start()
            }
        }
    }

    private fun drawingLoop() {
        try {
            while (isDrawing && holder.surface.isValid) {
                try {
                    val canvas = holder.lockCanvas()
                    if (canvas != null) {
                        drawWaveform(canvas)
                        holder.unlockCanvasAndPost(canvas)
                    }
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Canvas drawing error: ${e.message}")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected drawing error: ${e.message}")
                }

                try {
                    Thread.sleep(33) // ~30 FPS
                } catch (e: InterruptedException) {
                    Log.d(TAG, "Drawing thread interrupted")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in drawing loop: ${e.message}")
        } finally {
            Log.d(TAG, "Drawing loop ended")
        }
    }

    fun stopDrawing() {
        if (isDrawing) {
            Log.d(TAG, "Stopping drawing")
            isDrawing = false

            try {
                drawingThread?.join(1000) // Wait max 1 second
                drawingThread = null
            } catch (e: InterruptedException) {
                Log.w(TAG, "Drawing thread join interrupted")
                drawingThread?.interrupt()
                drawingThread = null
            }
        }
    }

    fun updateAmplitude(amplitude: Float) {
        if (!isDrawing) return

        try {
            synchronized(bufferLock) {
                amplitudeBuffer.add(amplitude.coerceIn(0f, 1f))
                if (amplitudeBuffer.size > maxBufferSize) {
                    amplitudeBuffer.removeAt(0)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating amplitude: ${e.message}")
        }
    }

    fun clearWaveform() {
        try {
            synchronized(bufferLock) {
                amplitudeBuffer.clear()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing waveform: ${e.message}")
        }
    }

    private fun drawWaveform(canvas: Canvas) {
        try {
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()
            val centerY = height / 2f

            // Clear background
            canvas.drawRect(0f, 0f, width, height, backgroundPaint)

            // Draw center line
            canvas.drawLine(0f, centerY, width, centerY, centerLinePaint)

            val bufferCopy = mutableListOf<Float>()
            synchronized(bufferLock) {
                bufferCopy.addAll(amplitudeBuffer)
            }

            if (bufferCopy.size < 2) return

            val path = Path()
            val stepX = width / maxBufferSize.toFloat()
            var started = false

            for (i in bufferCopy.indices) {
                val x = i * stepX
                val amplitude = bufferCopy[i]

                // Create smooth sine-wave style visualization
                val smoothedAmplitude = sin(amplitude * PI / 2).toFloat()
                val y = centerY - (smoothedAmplitude * centerY * 0.8f)

                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }

            canvas.drawPath(path, waveformPaint)
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing waveform: ${e.message}")
        }
    }
}