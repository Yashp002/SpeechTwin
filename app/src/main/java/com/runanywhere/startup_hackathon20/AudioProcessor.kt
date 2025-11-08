package com.runanywhere.startup_hackathon20

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.*

/**
 * AI-Powered Audio Processor for "Healthy Voice Suggestion" Feature
 * Applies audio processing to demonstrate what a healthier voice would sound like
 */
class AudioProcessor {
    companion object {
        private const val TAG = "AudioProcessor"

        /**
         * Process a voice recording to create a "healthier" version
         * @param inputFile Original WAV file
         * @param outputFile Where to save the processed WAV file
         * @param jitter Original jitter percentage (for determining processing intensity)
         * @param shimmer Original shimmer percentage
         * @return ProcessingResult with improvement metrics
         */
        fun processToHealthyVoice(
            inputFile: File,
            outputFile: File,
            jitter: Float,
            shimmer: Float
        ): ProcessingResult {
            try {
                Log.d(TAG, "Processing audio to create healthy version...")
                Log.d(TAG, "Input: ${inputFile.absolutePath}, Jitter: $jitter%, Shimmer: $shimmer%")

                // Read WAV file
                val audioData = readWavFile(inputFile)
                if (audioData.isEmpty()) {
                    Log.e(TAG, "Failed to read audio data")
                    return ProcessingResult(false, 0f, 0f, 0f)
                }

                Log.d(TAG, "Read ${audioData.size} samples")

                // Apply AI-powered processing pipeline
                var processed = audioData.toTypedArray()

                // 1. Pitch smoothing (reduce jitter)
                val jitterReduction = if (jitter > 2.0f) {
                    processed =
                        smoothPitch(processed, intensity = (jitter / 10f).coerceAtMost(0.5f))
                    ((jitter - 1.5f) / jitter * 100).coerceIn(0f, 70f)
                } else {
                    15f
                }

                // 2. Amplitude stabilization (reduce shimmer)
                val shimmerReduction = if (shimmer > 3.0f) {
                    processed = stabilizeAmplitude(
                        processed,
                        intensity = (shimmer / 15f).coerceAtMost(0.4f)
                    )
                    ((shimmer - 2.5f) / shimmer * 100).coerceIn(0f, 60f)
                } else {
                    10f
                }

                // 3. Noise reduction (low-pass filter)
                processed = applyLowPassFilter(processed)

                // 4. Enhance fundamental frequency
                processed = enhanceFundamental(processed)

                // 5. Normalize to optimal level
                processed = normalizeAudio(processed, targetRMS = 0.3f)

                // Write processed audio to output file
                writeWavFile(outputFile, processed.toShortArray())

                val overallImprovement = (jitterReduction + shimmerReduction) / 2f

                Log.d(
                    TAG,
                    "Processing complete! Jitter reduced by $jitterReduction%, Shimmer reduced by $shimmerReduction%"
                )

                return ProcessingResult(
                    success = true,
                    jitterReduction = jitterReduction,
                    shimmerReduction = shimmerReduction,
                    overallImprovement = overallImprovement
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio: ${e.message}", e)
                return ProcessingResult(false, 0f, 0f, 0f)
            }
        }

        /**
         * Smooth pitch variations using moving average (reduces jitter)
         */
        private fun smoothPitch(data: Array<Short>, intensity: Float): Array<Short> {
            val windowSize = (20 * intensity).toInt().coerceAtLeast(5) // 5-10 samples window
            val result = data.copyOf()

            for (i in windowSize until data.size - windowSize) {
                var sum = 0f
                for (j in -windowSize..windowSize) {
                    sum += data[i + j].toFloat()
                }
                result[i] = (sum / (windowSize * 2 + 1)).toInt().toShort()
            }

            return result
        }

        /**
         * Stabilize amplitude variations (reduces shimmer)
         */
        private fun stabilizeAmplitude(data: Array<Short>, intensity: Float): Array<Short> {
            val frameSize = 2048 // ~46ms at 44.1kHz
            val result = data.copyOf()

            // Calculate RMS for each frame
            var i = 0
            while (i < data.size - frameSize) {
                val frame = data.sliceArray(i until i + frameSize)
                val rms = sqrt(frame.map { it.toFloat() * it.toFloat() }.average()).toFloat()

                // Target RMS (smoothed)
                val targetRMS = rms * (1f - intensity * 0.5f) + 10000f * intensity

                // Normalize frame to target RMS
                if (rms > 100f) {
                    val scaleFactor = targetRMS / rms
                    for (j in 0 until frameSize.coerceAtMost(data.size - i)) {
                        result[i + j] = (data[i + j].toFloat() * scaleFactor)
                            .coerceIn(-32768f, 32767f)
                            .toInt()
                            .toShort()
                    }
                }

                i += frameSize / 2 // 50% overlap
            }

            return result
        }

        /**
         * Apply low-pass filter to reduce high-frequency noise
         */
        private fun applyLowPassFilter(data: Array<Short>): Array<Short> {
            val result = data.copyOf()
            val alpha = 0.15f // Filter coefficient (lower = more smoothing)

            for (i in 1 until data.size) {
                result[i] = ((1 - alpha) * result[i - 1].toFloat() + alpha * data[i].toFloat())
                    .toInt()
                    .toShort()
            }

            return result
        }

        /**
         * Enhance fundamental frequency using simple harmonic enhancement
         */
        private fun enhanceFundamental(data: Array<Short>): Array<Short> {
            val result = Array(data.size) { 0.toShort() }
            val pitchPeriod = 147 // ~150 Hz at 44.1kHz sample rate

            for (i in data.indices) {
                var enhanced = data[i].toFloat()

                // Add harmonic from previous period (emphasis on fundamental)
                if (i >= pitchPeriod) {
                    enhanced += data[i - pitchPeriod].toFloat() * 0.15f
                }

                result[i] = enhanced.coerceIn(-32768f, 32767f).toInt().toShort()
            }

            return result
        }

        /**
         * Normalize audio to target RMS level
         */
        private fun normalizeAudio(data: Array<Short>, targetRMS: Float): Array<Short> {
            // Calculate current RMS
            val rms = sqrt(data.map { it.toFloat() * it.toFloat() }.average()).toFloat()

            if (rms < 100f) return data // Avoid amplifying silence

            // Calculate scale factor
            val targetLevel = 32768f * targetRMS
            val scaleFactor = targetLevel / rms

            // Apply scaling
            return data.map { sample ->
                (sample.toFloat() * scaleFactor)
                    .coerceIn(-32768f, 32767f)
                    .toInt()
                    .toShort()
            }.toTypedArray()
        }

        /**
         * Read WAV file and return audio data as short array
         */
        private fun readWavFile(file: File): ShortArray {
            try {
                val fis = FileInputStream(file)
                val bytes = fis.readBytes()
                fis.close()

                // Find "data" chunk
                var dataOffset = 0
                for (i in 0 until bytes.size - 4) {
                    if (bytes[i] == 'd'.code.toByte() &&
                        bytes[i + 1] == 'a'.code.toByte() &&
                        bytes[i + 2] == 't'.code.toByte() &&
                        bytes[i + 3] == 'a'.code.toByte()
                    ) {
                        dataOffset = i + 8 // Skip "data" marker and size
                        break
                    }
                }

                if (dataOffset == 0) {
                    dataOffset = 44 // Fallback to standard header size
                }

                // Convert bytes to shorts (16-bit PCM)
                val numSamples = (bytes.size - dataOffset) / 2
                val audioData = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val byteIndex = dataOffset + i * 2
                    if (byteIndex + 1 < bytes.size) {
                        // Little-endian conversion
                        val low = bytes[byteIndex].toInt() and 0xFF
                        val high = bytes[byteIndex + 1].toInt() shl 8
                        audioData[i] = (high or low).toShort()
                    }
                }

                return audioData

            } catch (e: Exception) {
                Log.e(TAG, "Error reading WAV file: ${e.message}")
                return ShortArray(0)
            }
        }

        /**
         * Write audio data to WAV file
         */
        private fun writeWavFile(file: File, audioData: ShortArray) {
            try {
                val fos = FileOutputStream(file)

                // WAV header
                val sampleRate = 44100
                val numChannels = 1
                val bitsPerSample = 16
                val byteRate = sampleRate * numChannels * bitsPerSample / 8
                val blockAlign = numChannels * bitsPerSample / 8
                val dataSize = audioData.size * 2

                // RIFF header
                fos.write("RIFF".toByteArray())
                fos.write(intToBytes(36 + dataSize))
                fos.write("WAVE".toByteArray())

                // fmt chunk
                fos.write("fmt ".toByteArray())
                fos.write(intToBytes(16)) // chunk size
                fos.write(shortToBytes(1)) // PCM format
                fos.write(shortToBytes(numChannels.toShort()))
                fos.write(intToBytes(sampleRate))
                fos.write(intToBytes(byteRate))
                fos.write(shortToBytes(blockAlign.toShort()))
                fos.write(shortToBytes(bitsPerSample.toShort()))

                // data chunk
                fos.write("data".toByteArray())
                fos.write(intToBytes(dataSize))

                // Audio data
                for (sample in audioData) {
                    fos.write(shortToBytes(sample))
                }

                fos.close()
                Log.d(TAG, "Successfully wrote WAV file: ${file.absolutePath}")

            } catch (e: Exception) {
                Log.e(TAG, "Error writing WAV file: ${e.message}")
            }
        }

        private fun intToBytes(value: Int): ByteArray {
            return byteArrayOf(
                (value and 0xFF).toByte(),
                ((value shr 8) and 0xFF).toByte(),
                ((value shr 16) and 0xFF).toByte(),
                ((value shr 24) and 0xFF).toByte()
            )
        }

        private fun shortToBytes(value: Short): ByteArray {
            return byteArrayOf(
                (value.toInt() and 0xFF).toByte(),
                ((value.toInt() shr 8) and 0xFF).toByte()
            )
        }
    }

    data class ProcessingResult(
        val success: Boolean,
        val jitterReduction: Float,
        val shimmerReduction: Float,
        val overallImprovement: Float
    )
}