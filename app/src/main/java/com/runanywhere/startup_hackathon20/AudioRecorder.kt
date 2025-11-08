package com.runanywhere.startup_hackathon20

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

class AudioRecorder {
    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val RECORDING_TIME_MS = 10000 // 10 seconds
        private const val BUFFER_SIZE_FACTOR = 4 // Increased buffer size
    }

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var amplitudeCallback: ((Float) -> Unit)? = null
    private val audioData = mutableListOf<Short>()
    private val dataLock = Any() // Thread safety for audioData

    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
    ) * BUFFER_SIZE_FACTOR

    fun setAmplitudeCallback(callback: (Float) -> Unit) {
        amplitudeCallback = callback
    }

    fun startRecording(): Boolean {
        return try {
            Log.d(TAG, "Starting recording...")

            if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                Log.e(TAG, "Invalid buffer size: $bufferSize")
                return false
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized properly")
                cleanup()
                return false
            }

            synchronized(dataLock) {
                audioData.clear()
            }

            isRecording = true
            audioRecord?.startRecording()

            if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                Log.e(TAG, "AudioRecord failed to start recording")
                cleanup()
                return false
            }

            recordingThread = Thread(::recordingLoop, "AudioRecordingThread").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }

            Log.d(TAG, "Recording started successfully")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
            cleanup()
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting recording: ${e.message}")
            cleanup()
            false
        }
    }

    private fun recordingLoop() {
        val buffer = ShortArray(bufferSize / 2)
        val startTime = System.currentTimeMillis()

        try {
            while (isRecording && (System.currentTimeMillis() - startTime) < RECORDING_TIME_MS) {
                val audioRecord = this.audioRecord ?: break

                val bytesRead = audioRecord.read(buffer, 0, buffer.size)

                if (bytesRead > 0) {
                    // Store audio data thread-safely
                    synchronized(dataLock) {
                        // Limit memory usage - only keep last 10 seconds of data
                        if (audioData.size > SAMPLE_RATE * 10) {
                            val removeCount = audioData.size - SAMPLE_RATE * 10
                            repeat(removeCount) { audioData.removeAt(0) }
                        }
                        audioData.addAll(buffer.take(bytesRead))
                    }

                    // Calculate amplitude for visualization (safely)
                    try {
                        val amplitude = calculateAmplitude(buffer, bytesRead)
                        amplitudeCallback?.invoke(amplitude)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error calculating amplitude: ${e.message}")
                    }
                } else if (bytesRead < 0) {
                    Log.w(TAG, "Audio read error: $bytesRead")
                    break
                }

                // Small delay to prevent overwhelming the system
                Thread.sleep(10)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in recording loop: ${e.message}")
        } finally {
            Log.d(TAG, "Recording loop ended")
        }

        // Auto-stop if we reached the time limit
        if (isRecording && (System.currentTimeMillis() - startTime) >= RECORDING_TIME_MS) {
            Log.d(TAG, "Auto-stopping recording after 10 seconds")
            stopRecording()
        }
    }

    fun stopRecording() {
        Log.d(TAG, "Stopping recording...")
        isRecording = false

        try {
            recordingThread?.join(2000) // Wait max 2 seconds for thread to finish
            recordingThread = null
        } catch (e: InterruptedException) {
            Log.w(TAG, "Recording thread join interrupted")
        }

        cleanup()
        Log.d(TAG, "Recording stopped")
    }

    private fun cleanup() {
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord: ${e.message}")
        }

        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord: ${e.message}")
        }

        audioRecord = null
    }

    fun saveToWavFile(file: File): Boolean {
        return try {
            val audioDataCopy: ShortArray
            synchronized(dataLock) {
                audioDataCopy = audioData.toShortArray()
            }

            if (audioDataCopy.isEmpty()) {
                Log.w(TAG, "No audio data to save")
                return false
            }

            writeWavFile(file, audioDataCopy)
            Log.d(TAG, "WAV file saved: ${file.absolutePath}, size: ${file.length()} bytes")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving WAV file: ${e.message}")
            false
        }
    }

    private fun calculateAmplitude(buffer: ShortArray, length: Int): Float {
        var sum = 0.0
        for (i in 0 until length) {
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        val rms = sqrt(sum / length)
        return (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }

    private fun writeWavFile(file: File, audioData: ShortArray) {
        val fileOutputStream = FileOutputStream(file)
        val dataOutputStream = DataOutputStream(fileOutputStream)

        val totalAudioLen = audioData.size * 2L // 16-bit = 2 bytes per sample
        val totalDataLen = totalAudioLen + 36L
        val channels = 1
        val byteRate = SAMPLE_RATE * channels * 2L

        try {
            // WAV file header
            dataOutputStream.writeBytes("RIFF")
            dataOutputStream.write(intToByteArray(totalDataLen.toInt()), 0, 4)
            dataOutputStream.writeBytes("WAVE")
            dataOutputStream.writeBytes("fmt ")
            dataOutputStream.write(intToByteArray(16), 0, 4) // Sub-chunk size
            dataOutputStream.write(shortToByteArray(1), 0, 2) // Audio format (PCM)
            dataOutputStream.write(shortToByteArray(channels.toShort()), 0, 2) // Number of channels
            dataOutputStream.write(intToByteArray(SAMPLE_RATE), 0, 4) // Sample rate
            dataOutputStream.write(intToByteArray(byteRate.toInt()), 0, 4) // Byte rate
            dataOutputStream.write(shortToByteArray((channels * 2).toShort()), 0, 2) // Block align
            dataOutputStream.write(shortToByteArray(16), 0, 2) // Bits per sample
            dataOutputStream.writeBytes("data")
            dataOutputStream.write(intToByteArray(totalAudioLen.toInt()), 0, 4)

            // Audio data
            val byteBuffer = ByteBuffer.allocate(audioData.size * 2)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            for (sample in audioData) {
                byteBuffer.putShort(sample)
            }
            dataOutputStream.write(byteBuffer.array())
        } finally {
            dataOutputStream.close()
            fileOutputStream.close()
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}