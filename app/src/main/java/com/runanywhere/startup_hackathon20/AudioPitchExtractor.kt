package com.runanywhere.startup_hackathon20

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Extracts pitch sequences from audio files for the pitch matching game
 */
object AudioPitchExtractor {

    /**
     * Extract a sequence of pitches from an audio file
     * @param context Android context
     * @param uri URI of the audio file
     * @param maxNotes Maximum number of notes to extract (default 20)
     * @return List of pitch frequencies in Hz, or null if extraction failed
     */
    suspend fun extractPitchSequence(
        context: Context,
        uri: Uri,
        maxNotes: Int = 20
    ): List<Float>? = withContext(Dispatchers.IO) {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)

            // Find audio track
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = fmt
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                Log.e("AudioPitchExtractor", "No audio track found")
                return@withContext null
            }

            extractor.selectTrack(audioTrackIndex)

            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val pitches = mutableListOf<Float>()
            val buffer = ByteBuffer.allocate(8192)
            val sampleBuffer = mutableListOf<Short>()

            // Process audio in chunks
            val samplesPerChunk = sampleRate / 2 // 0.5 second chunks
            var totalSamplesRead = 0

            while (pitches.size < maxNotes) {
                buffer.clear()
                val bytesRead = extractor.readSampleData(buffer, 0)

                if (bytesRead < 0) break // End of file

                // Convert bytes to shorts (16-bit PCM)
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                buffer.rewind()

                while (buffer.hasRemaining() && buffer.remaining() >= 2) {
                    sampleBuffer.add(buffer.short)
                    totalSamplesRead++

                    // Process when we have enough samples
                    if (totalSamplesRead >= samplesPerChunk) {
                        val pitch = detectPitchFromSamples(sampleBuffer.toShortArray(), sampleRate)

                        // Only add valid pitches (80-500 Hz range for human voice)
                        if (pitch in 80f..500f) {
                            pitches.add(pitch)
                        }

                        // Clear buffer for next chunk
                        sampleBuffer.clear()
                        totalSamplesRead = 0

                        if (pitches.size >= maxNotes) break
                    }
                }

                extractor.advance()
            }

            extractor.release()

            // Return null if we didn't extract enough pitches
            if (pitches.size < 3) {
                Log.e("AudioPitchExtractor", "Not enough valid pitches extracted: ${pitches.size}")
                return@withContext null
            }

            // Smooth out the pitch sequence
            val smoothedPitches = smoothPitchSequence(pitches)

            Log.d(
                "AudioPitchExtractor",
                "Extracted ${smoothedPitches.size} pitches: $smoothedPitches"
            )
            smoothedPitches

        } catch (e: Exception) {
            Log.e("AudioPitchExtractor", "Error extracting pitch: ${e.message}", e)
            null
        }
    }

    /**
     * Detect pitch from audio samples using autocorrelation
     */
    private fun detectPitchFromSamples(audioData: ShortArray, sampleRate: Int): Float {
        val size = minOf(audioData.size, 4096)
        val buffer = audioData.take(size).map { it.toFloat() }.toFloatArray()

        // Calculate RMS to check if there's significant audio
        val rms = kotlin.math.sqrt(buffer.map { it * it }.average()).toFloat()
        if (rms < 100f) return 0f // Too quiet

        // Autocorrelation
        val minLag = (sampleRate / 500f).toInt() // 500 Hz max
        val maxLag = (sampleRate / 80f).toInt()  // 80 Hz min

        var bestLag = 0
        var maxCorr = 0f

        for (lag in minLag..maxLag.coerceAtMost(size / 2)) {
            var corr = 0f
            for (i in 0 until size - lag) {
                corr += buffer[i] * buffer[i + lag]
            }
            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }

        return if (bestLag > 0 && maxCorr > 0) {
            sampleRate.toFloat() / bestLag
        } else {
            0f
        }
    }

    /**
     * Smooth pitch sequence to remove outliers and jitter
     */
    private fun smoothPitchSequence(pitches: List<Float>): List<Float> {
        if (pitches.size < 3) return pitches

        val smoothed = mutableListOf<Float>()

        // Use median filter with window size 3
        for (i in pitches.indices) {
            val window = mutableListOf<Float>()

            if (i > 0) window.add(pitches[i - 1])
            window.add(pitches[i])
            if (i < pitches.size - 1) window.add(pitches[i + 1])

            smoothed.add(window.sorted()[window.size / 2])
        }

        // Remove consecutive duplicates (keep only note changes)
        val deduplicated = mutableListOf<Float>()
        var lastPitch = smoothed[0]
        deduplicated.add(lastPitch)

        for (i in 1 until smoothed.size) {
            val currentPitch = smoothed[i]
            // Only add if pitch changed by more than 10 Hz (significant change)
            if (abs(currentPitch - lastPitch) > 10f) {
                deduplicated.add(currentPitch)
                lastPitch = currentPitch
            }
        }

        return deduplicated
    }

    /**
     * Get a display name from URI
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var fileName = "Custom Audio"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                    // Remove extension
                    fileName = fileName.substringBeforeLast(".")
                }
            }
        }

        return fileName
    }
}