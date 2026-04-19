package com.fossift.asciicam.engine

import kotlin.math.pow

class AsciiConverter {
    private var previousSamples: IntArray? = null
    private var sampledLuma: IntArray? = null
    private var lutSignature: Long = Long.MIN_VALUE
    private var glyphLut: CharArray = CharArray(256)

    fun convert(luma: ByteArray, sourceWidth: Int, sourceHeight: Int, config: AsciiConfig): AsciiFrame {
        require(sourceWidth > 0 && sourceHeight > 0) { "Source dimensions must be > 0" }
        require(luma.size >= sourceWidth * sourceHeight) { "Luma buffer is too small" }

        val frameSize = config.width * config.height
        val outputChars = CharArray(frameSize)
        val previous = ensurePreviousSamples(frameSize)
        val sampled = ensureSampleBuffer(frameSize)
        val useAdvancedPath = config.autoToneMapping || config.centerFocusBoost > 0f
        val lut = if (!useAdvancedPath) updateGlyphLut(config) else null

        val smoothPrevWeight = ((config.temporalSmoothing.coerceIn(0f, 0.92f)) * 256f).toInt()
        val smoothCurWeight = 256 - smoothPrevWeight

        var outIndex = 0
        for (y in 0 until config.height) {
            val srcY = ((y * sourceHeight) + (config.height / 2)) / config.height
            val clampedY = srcY.coerceIn(0, sourceHeight - 1)
            val rowBase = clampedY * sourceWidth
            for (x in 0 until config.width) {
                val srcX = ((x * sourceWidth) + (config.width / 2)) / config.width
                val clampedX = srcX.coerceIn(0, sourceWidth - 1)
                var sample = luma[rowBase + clampedX].toInt() and 0xFF

                if (smoothPrevWeight > 0) {
                    sample = ((previous[outIndex] * smoothPrevWeight) + (sample * smoothCurWeight)) shr 8
                }
                previous[outIndex] = sample
                sampled[outIndex] = sample

                if (lut != null) {
                    outputChars[outIndex] = lut[sample]
                }
                outIndex++
            }
        }

        if (useAdvancedPath) {
            mapAdvanced(
                sampled = sampled,
                outputChars = outputChars,
                width = config.width,
                height = config.height,
                config = config,
            )
        }

        return AsciiFrame(config.width, config.height, outputChars)
    }

    private fun ensurePreviousSamples(size: Int): IntArray {
        val existing = previousSamples
        if (existing != null && existing.size == size) {
            return existing
        }
        return IntArray(size).also { previousSamples = it }
    }

    private fun ensureSampleBuffer(size: Int): IntArray {
        val existing = sampledLuma
        if (existing != null && existing.size == size) {
            return existing
        }
        return IntArray(size).also { sampledLuma = it }
    }

    private fun updateGlyphLut(config: AsciiConfig): CharArray {
        val signature = buildLutSignature(config)
        if (signature == lutSignature) {
            return glyphLut
        }

        val glyphs = config.charset.glyphs
        val maxIndex = glyphs.size - 1
        val invGamma = (1f / config.gamma.coerceIn(0.6f, 2.2f)).toDouble()
        val contrast = config.contrast.coerceIn(0.7f, 2.0f)

        for (v in 0..255) {
            val normalized = v / 255f
            val contrasted = ((normalized - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
            val corrected = contrasted.toDouble().pow(invGamma).toFloat().coerceIn(0f, 1f)
            val glyphIndex = ((1f - corrected) * maxIndex).toInt().coerceIn(0, maxIndex)
            glyphLut[v] = glyphs[glyphIndex]
        }

        lutSignature = signature
        return glyphLut
    }

    private fun buildLutSignature(config: AsciiConfig): Long {
        var signature = 17L
        signature = (signature * 31L) + config.charset.hashCode().toLong()
        signature = (signature * 31L) + config.contrast.toBits().toLong()
        signature = (signature * 31L) + config.gamma.toBits().toLong()
        return signature
    }

    private fun mapAdvanced(
        sampled: IntArray,
        outputChars: CharArray,
        width: Int,
        height: Int,
        config: AsciiConfig,
    ) {
        val glyphs = config.charset.glyphs
        val maxIndex = glyphs.size - 1

        var inMin = 0
        var inMax = 255
        if (config.autoToneMapping) {
            val bins = IntArray(256)
            for (value in sampled) {
                bins[value.coerceIn(0, 255)]++
            }
            val lowTarget = (sampled.size * config.toneClipLowPercent.coerceIn(0f, 0.2f)).toInt()
            val highTarget = (sampled.size * config.toneClipHighPercent.coerceIn(0f, 0.2f)).toInt()

            var cumulative = 0
            var low = 0
            while (low < 255 && cumulative < lowTarget) {
                cumulative += bins[low]
                low++
            }

            cumulative = 0
            var high = 255
            while (high > 0 && cumulative < highTarget) {
                cumulative += bins[high]
                high--
            }
            inMin = low.coerceIn(0, 255)
            inMax = high.coerceIn(0, 255)
            if (inMax <= inMin) {
                inMin = sampled.minOrNull() ?: 0
                inMax = sampled.maxOrNull() ?: 255
            }
        }

        val inputRange = (inMax - inMin).coerceAtLeast(8)
        val contrast = config.contrast.coerceIn(0.7f, 2.0f)
        val invGamma = (1f / config.gamma.coerceIn(0.6f, 2.2f)).toDouble()
        val focus = config.centerFocusBoost.coerceIn(0f, 1f)
        val focusRadius = config.centerFocusRadius.coerceIn(0.2f, 1.2f)

        for (i in sampled.indices) {
            val x = i % width
            val y = i / width

            var normalized = ((sampled[i] - inMin).toFloat() / inputRange.toFloat()).coerceIn(0f, 1f)
            if (focus > 0f) {
                val w = centerFocusWeight(x, y, width, height, focusRadius)
                val localContrast = 1f + (0.85f * focus * w)
                val localLift = 0.12f * focus * w
                normalized = ((normalized - 0.5f) * localContrast + 0.5f + localLift).coerceIn(0f, 1f)
            }

            val contrasted = ((normalized - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
            val corrected = contrasted.toDouble().pow(invGamma).toFloat().coerceIn(0f, 1f)
            val glyphIndex = ((1f - corrected) * maxIndex).toInt().coerceIn(0, maxIndex)
            outputChars[i] = glyphs[glyphIndex]
        }
    }

    private fun centerFocusWeight(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        focusRadius: Float,
    ): Float {
        if (width <= 1 || height <= 1) return 0f
        val nx = ((x + 0.5f) / width.toFloat()) - 0.5f
        val ny = ((y + 0.5f) / height.toFloat()) - 0.5f
        val aspect = width.toFloat() / height.toFloat()
        val distance = kotlin.math.sqrt((nx * nx * aspect * aspect) + (ny * ny))
        val normalized = (distance / focusRadius).coerceIn(0f, 1f)
        val inv = 1f - normalized
        return inv * inv
    }
}
