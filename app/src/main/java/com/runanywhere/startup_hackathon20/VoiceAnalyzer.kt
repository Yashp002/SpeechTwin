package com.runanywhere.startup_hackathon20

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * Comprehensive Voice Analyzer
 * Analyzes WAV files and returns detailed voice metrics
 */
class VoiceAnalyzer {
    companion object {
        private const val TAG = "VoiceAnalyzer"
        private const val SAMPLE_RATE = 44100
        private const val FRAME_SIZE_MS = 50 // 50ms frames
        private const val FRAME_SIZE = (SAMPLE_RATE * FRAME_SIZE_MS) / 1000 // samples per frame
    }

    data class AnalysisResult(
        val pitch: Float,           // Fundamental frequency in Hz
        val loudness: Float,        // RMS loudness in dB
        val jitter: Float,          // Pitch variation percentage
        val shimmer: Float,         // Amplitude variation percentage
        val healthScore: Int,       // 0-100 health score
        val duration: Int = 10      // Duration in seconds
    )

    /**
     * Analyze a WAV audio file
     * @param filePath Path to the WAV file
     * @return AnalysisResult with all voice metrics
     */
    fun analyze(filePath: String): AnalysisResult {
        Log.d(TAG, "=== Starting Voice Analysis ===")
        Log.d(TAG, "File: $filePath")

        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "File does not exist or cannot be read: $filePath")
            return createDefaultResult()
        }

        try {
            // Load audio data from WAV file
            val audioData = loadWavFile(file)

            if (audioData.isEmpty()) {
                Log.e(TAG, "No audio data loaded from file")
                return createDefaultResult()
            }

            Log.d(TAG, "Loaded ${audioData.size} audio samples")

            // Normalize audio data
            val normalizedAudio = normalizeAudio(audioData)

            // Calculate all metrics
            val pitchValues = calculatePitchPerFrame(normalizedAudio)
            val amplitudeValues = calculateAmplitudePerFrame(normalizedAudio)

            // Get statistics
            val pitch = calculateMedianPitch(pitchValues)
            val loudness = calculateLoudness(normalizedAudio)
            val jitter = calculateJitter(pitchValues)
            val shimmer = calculateShimmer(amplitudeValues)

            Log.d(TAG, "=== Final Results ===")
            Log.d(TAG, "Pitch: $pitch Hz")
            Log.d(TAG, "Loudness: $loudness dB")
            Log.d(TAG, "Jitter: $jitter %")
            Log.d(TAG, "Shimmer: $shimmer %")

            // Calculate health score based on all metrics
            val healthScore = calculateHealthScore(pitch, loudness, jitter, shimmer)
            Log.d(TAG, "Health Score: $healthScore")

            return AnalysisResult(pitch, loudness, jitter, shimmer, healthScore)

        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing audio: ${e.message}", e)
            return createDefaultResult()
        }
    }

    private fun createDefaultResult(): AnalysisResult {
        return AnalysisResult(0f, -60f, 0f, 0f, 0)
    }

    /**
     * Load audio data from WAV file
     * Handles variable header sizes by finding the "data" chunk
     */
    private fun loadWavFile(file: File): ShortArray {
        try {
            val inputStream = FileInputStream(file)
            val allBytes = inputStream.readBytes()
            inputStream.close()

            Log.d(TAG, "File size: ${allBytes.size} bytes")

            // Find "data" chunk in WAV file
            var dataOffset = 44 // Default offset
            for (i in 0 until minOf(200, allBytes.size - 4)) {
                if (allBytes[i] == 'd'.code.toByte() &&
                    allBytes[i + 1] == 'a'.code.toByte() &&
                    allBytes[i + 2] == 't'.code.toByte() &&
                    allBytes[i + 3] == 'a'.code.toByte()
                ) {
                    dataOffset = i + 8 // Skip "data" and size (4 bytes each)
                    Log.d(TAG, "Found data chunk at offset: $dataOffset")
                    break
                }
            }

            // Extract audio bytes
            val audioBytes = allBytes.copyOfRange(dataOffset, allBytes.size)
            Log.d(TAG, "Audio data size: ${audioBytes.size} bytes")

            // Convert 16-bit PCM to ShortArray
            val buffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN)
            val samples = ShortArray(audioBytes.size / 2)

            for (i in samples.indices) {
                if (i * 2 + 1 < audioBytes.size) {
                    samples[i] = buffer.getShort(i * 2)
                }
            }

            Log.d(TAG, "Converted to ${samples.size} samples")
            return samples

        } catch (e: Exception) {
            Log.e(TAG, "Error loading WAV file: ${e.message}", e)
            return shortArrayOf()
        }
    }

    /**
     * Normalize audio to use full dynamic range
     */
    private fun normalizeAudio(audio: ShortArray): FloatArray {
        val maxAmplitude = audio.maxOfOrNull { abs(it.toFloat()) } ?: 0f

        if (maxAmplitude < 100f) {
            Log.w(TAG, "Audio is too quiet to normalize (max: $maxAmplitude)")
            return FloatArray(audio.size) { audio[it].toFloat() / 32768f }
        }

        val normalized = FloatArray(audio.size)
        val scaleFactor = 32000f / maxAmplitude // Scale to near max to avoid clipping

        for (i in audio.indices) {
            normalized[i] = (audio[i].toFloat() * scaleFactor) / 32768f
        }

        Log.d(TAG, "Audio normalized (max amplitude: $maxAmplitude -> 32000)")
        return normalized
    }

    /**
     * Calculate pitch for each frame using autocorrelation
     * Returns array of pitch values (0 for unvoiced frames)
     */
    private fun calculatePitchPerFrame(audio: FloatArray): FloatArray {
        val numFrames = (audio.size / FRAME_SIZE).coerceAtLeast(1)
        val pitchValues = FloatArray(numFrames)

        Log.d(TAG, "Calculating pitch for $numFrames frames (frame size: $FRAME_SIZE samples)")

        for (frameIdx in 0 until numFrames) {
            val start = frameIdx * FRAME_SIZE
            val end = minOf(start + FRAME_SIZE, audio.size)

            if (end - start < FRAME_SIZE / 2) break

            val frame = audio.copyOfRange(start, end)
            pitchValues[frameIdx] = calculatePitchForFrame(frame)
        }

        val validPitches = pitchValues.filter { it > 0 }
        Log.d(TAG, "Detected ${validPitches.size} voiced frames out of $numFrames total frames")

        return pitchValues
    }

    /**
     * Calculate pitch for a single frame using autocorrelation
     */
    private fun calculatePitchForFrame(frame: FloatArray): Float {
        val minPeriod = SAMPLE_RATE / 400  // 400 Hz max
        val maxPeriod = SAMPLE_RATE / 80   // 80 Hz min

        // Calculate energy to detect silence
        val energy = frame.map { it * it }.average()
        if (energy < 0.001) {
            return 0f // Silence or very quiet, skip pitch detection
        }

        // Calculate autocorrelation
        var maxCorr = 0f
        var bestPeriod = 0

        for (period in minPeriod..minOf(maxPeriod, frame.size / 2)) {
            var correlation = 0f
            var count = 0

            for (i in 0 until frame.size - period) {
                correlation += frame[i] * frame[i + period]
                count++
            }

            if (count > 0) {
                correlation /= count

                if (correlation > maxCorr) {
                    maxCorr = correlation
                    bestPeriod = period
                }
            }
        }

        // Convert period to frequency
        val pitch = if (bestPeriod > 0 && maxCorr > 0.3f) {
            SAMPLE_RATE.toFloat() / bestPeriod
        } else {
            0f // Unvoiced or unreliable
        }

        return pitch
    }

    /**
     * Calculate median pitch from all frames (ignoring unvoiced frames)
     */
    private fun calculateMedianPitch(pitchValues: FloatArray): Float {
        val validPitches = pitchValues.filter { it > 0 && it in 80f..400f }

        if (validPitches.isEmpty()) {
            Log.w(TAG, "No valid pitch values detected, using default 180 Hz")
            return 180f
        }

        val sorted = validPitches.sorted()
        val median = sorted[sorted.size / 2]

        Log.d(
            TAG,
            "Pitch statistics: min=${sorted.first()}, max=${sorted.last()}, median=$median, count=${sorted.size}"
        )
        return median
    }

    /**
     * Calculate amplitude for each frame
     */
    private fun calculateAmplitudePerFrame(audio: FloatArray): FloatArray {
        val numFrames = (audio.size / FRAME_SIZE).coerceAtLeast(1)
        val amplitudeValues = FloatArray(numFrames)

        Log.d(TAG, "Calculating amplitude for $numFrames frames")

        for (frameIdx in 0 until numFrames) {
            val start = frameIdx * FRAME_SIZE
            val end = minOf(start + FRAME_SIZE, audio.size)

            if (end - start < FRAME_SIZE / 2) break

            val frame = audio.copyOfRange(start, end)

            // Calculate RMS amplitude for this frame
            val rms = sqrt(frame.map { it * it.toDouble() }.average())
            amplitudeValues[frameIdx] = rms.toFloat()
        }

        return amplitudeValues
    }

    /**
     * Calculate overall loudness (RMS in dB)
     */
    private fun calculateLoudness(audio: FloatArray): Float {
        // Calculate RMS (Root Mean Square)
        var sumSquares = 0.0
        for (sample in audio) {
            sumSquares += (sample * sample).toDouble()
        }
        val rms = sqrt(sumSquares / audio.size)

        // Convert to dB (reference: 1.0 = 0 dB)
        val db = if (rms > 0.0) {
            20 * log10(rms)
        } else {
            -60.0 // Minimum threshold
        }

        Log.d(TAG, "RMS: $rms, Loudness: $db dB")
        return db.toFloat()
    }

    /**
     * Calculate jitter (pitch variation percentage)
     * Jitter = (standard deviation of pitches / mean pitch) * 100
     */
    private fun calculateJitter(pitchValues: FloatArray): Float {
        val validPitches = pitchValues.filter { it > 0 && it in 80f..400f }

        if (validPitches.size < 3) {
            Log.w(
                TAG,
                "Not enough pitch values for jitter calculation (need 3+, have ${validPitches.size})"
            )
            return 0f
        }

        val mean = validPitches.average().toFloat()
        val variance = validPitches.map { (it - mean) * (it - mean) }.average()
        val stddev = sqrt(variance).toFloat()

        val jitter = (stddev / mean) * 100f

        Log.d(TAG, "Jitter calculation: mean=$mean Hz, stddev=$stddev, jitter=$jitter%")
        return jitter
    }

    /**
     * Calculate shimmer (amplitude variation percentage)
     * Shimmer = (standard deviation of amplitudes / mean amplitude) * 100
     */
    private fun calculateShimmer(amplitudeValues: FloatArray): Float {
        val validAmplitudes = amplitudeValues.filter { it > 0.001f }

        if (validAmplitudes.size < 3) {
            Log.w(
                TAG,
                "Not enough amplitude values for shimmer calculation (need 3+, have ${validAmplitudes.size})"
            )
            return 0f
        }

        val mean = validAmplitudes.average().toFloat()
        val variance = validAmplitudes.map { (it - mean) * (it - mean) }.average()
        val stddev = sqrt(variance).toFloat()

        val shimmer = (stddev / mean) * 100f

        Log.d(TAG, "Shimmer calculation: mean=$mean, stddev=$stddev, shimmer=$shimmer%")
        return shimmer
    }

    /**
     * Calculate health score (0-100) based on all metrics
     */
    private fun calculateHealthScore(
        pitch: Float,
        loudness: Float,
        jitter: Float,
        shimmer: Float
    ): Int {
        var score = 100

        Log.d(TAG, "=== Health Score Calculation ===")
        Log.d(TAG, "Starting score: $score")

        // Pitch scoring (healthy range: 120-250 Hz)
        when {
            pitch < 80 || pitch > 400 -> {
                score -= 20
                Log.d(TAG, "Pitch way out of range ($pitch Hz): -20 points")
            }

            pitch < 100 || pitch > 300 -> {
                score -= 15
                Log.d(TAG, "Pitch outside normal range ($pitch Hz): -15 points")
            }

            pitch < 120 || pitch > 250 -> {
                score -= 5
                Log.d(TAG, "Pitch outside optimal range ($pitch Hz): -5 points")
            }

            else -> {
                Log.d(TAG, "Pitch in optimal range ($pitch Hz): no deduction")
            }
        }

        // Loudness scoring (healthy range: -30 to -10 dB)
        when {
            loudness < -50 -> {
                score -= 20
                Log.d(TAG, "Loudness too quiet ($loudness dB): -20 points")
            }

            loudness < -40 -> {
                score -= 15
                Log.d(TAG, "Loudness quiet ($loudness dB): -15 points")
            }

            loudness < -30 -> {
                score -= 5
                Log.d(TAG, "Loudness slightly quiet ($loudness dB): -5 points")
            }

            loudness > -5 -> {
                score -= 10
                Log.d(TAG, "Loudness too loud ($loudness dB, possible clipping): -10 points")
            }

            else -> {
                Log.d(TAG, "Loudness in healthy range ($loudness dB): no deduction")
            }
        }

        // Jitter scoring (healthy range: <2%)
        when {
            jitter > 5.0f -> {
                val deduction = ((jitter - 2f) * 5).toInt().coerceAtMost(30)
                score -= deduction
                Log.d(TAG, "High jitter ($jitter%): -$deduction points")
            }

            jitter > 2.0f -> {
                val deduction = ((jitter - 2f) * 10).toInt()
                score -= deduction
                Log.d(TAG, "Elevated jitter ($jitter%): -$deduction points")
            }

            else -> {
                Log.d(TAG, "Jitter in healthy range ($jitter%): no deduction")
            }
        }

        // Shimmer scoring (healthy range: <5%)
        when {
            shimmer > 10.0f -> {
                val deduction = ((shimmer - 5f) * 3).toInt().coerceAtMost(25)
                score -= deduction
                Log.d(TAG, "High shimmer ($shimmer%): -$deduction points")
            }

            shimmer > 5.0f -> {
                val deduction = ((shimmer - 5f) * 10).toInt()
                score -= deduction
                Log.d(TAG, "Elevated shimmer ($shimmer%): -$deduction points")
            }

            else -> {
                Log.d(TAG, "Shimmer in healthy range ($shimmer%): no deduction")
            }
        }

        val finalScore = score.coerceIn(0, 100)
        Log.d(TAG, "Final health score: $finalScore")

        return finalScore
    }
}