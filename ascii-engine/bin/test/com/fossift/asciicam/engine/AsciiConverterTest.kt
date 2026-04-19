package com.fossift.asciicam.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AsciiConverterTest {
    @Test
    fun `convert creates expected dimensions`() {
        val converter = AsciiConverter()
        val sourceWidth = 4
        val sourceHeight = 4
        val luma = ByteArray(sourceWidth * sourceHeight) { 127.toByte() }

        val frame = converter.convert(
            luma = luma,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            config = AsciiConfig(width = 2, height = 2),
        )

        assertEquals(2, frame.width)
        assertEquals(2, frame.height)
        assertEquals(2, frame.lines.size)
        assertTrue(frame.lines.all { it.length == 2 })
    }

    @Test
    fun `convert maps bright and dark pixels differently`() {
        val converter = AsciiConverter()
        val luma = byteArrayOf(
            0, -1,
            0, -1,
        )

        val frame = converter.convert(
            luma = luma,
            sourceWidth = 2,
            sourceHeight = 2,
            config = AsciiConfig(width = 2, height = 2, charset = AsciiCharset.CLASSIC),
        )

        assertNotEquals(frame.lines[0][0], frame.lines[0][1])
    }

    @Test
    fun `temporal smoothing changes second frame output`() {
        val converter = AsciiConverter()
        val dark = ByteArray(16) { 0 }
        val bright = ByteArray(16) { (-1).toByte() }
        val config = AsciiConfig(width = 4, height = 4, temporalSmoothing = 0.8f)

        val first = converter.convert(dark, 4, 4, config)
        val second = converter.convert(bright, 4, 4, config)

        assertNotEquals(first.asText(), second.asText())
    }

    @Test
    fun `auto tone mapping increases visible contrast in narrow range image`() {
        val converter = AsciiConverter()
        val luma = byteArrayOf(
            102.toByte(), 104.toByte(), 106.toByte(), 108.toByte(),
            102.toByte(), 104.toByte(), 106.toByte(), 108.toByte(),
            102.toByte(), 104.toByte(), 106.toByte(), 108.toByte(),
            102.toByte(), 104.toByte(), 106.toByte(), 108.toByte(),
        )

        val withoutToneMap = converter.convert(
            luma = luma,
            sourceWidth = 4,
            sourceHeight = 4,
            config = AsciiConfig(
                width = 4,
                height = 4,
                contrast = 1f,
                gamma = 1f,
                temporalSmoothing = 0f,
                autoToneMapping = false,
                charset = AsciiCharset.CLASSIC,
            ),
        ).asText()

        val withToneMap = converter.convert(
            luma = luma,
            sourceWidth = 4,
            sourceHeight = 4,
            config = AsciiConfig(
                width = 4,
                height = 4,
                contrast = 1f,
                gamma = 1f,
                temporalSmoothing = 0f,
                autoToneMapping = true,
                toneClipLowPercent = 0.2f,
                toneClipHighPercent = 0.2f,
                charset = AsciiCharset.CLASSIC,
            ),
        ).asText()

        assertNotEquals(withoutToneMap, withToneMap)
    }

    @Test
    fun `center focus boost changes output for centered subject pattern`() {
        val converter = AsciiConverter()
        val size = 28
        val luma = ByteArray(size * size) { idx ->
            val x = idx % size
            val y = idx / size
            val nx = (x.toFloat() / (size - 1).toFloat()) - 0.5f
            val ny = (y.toFloat() / (size - 1).toFloat()) - 0.5f
            val radius = kotlin.math.sqrt((nx * nx) + (ny * ny)).coerceIn(0f, 0.72f)
            val lumaValue = 122f + ((radius / 0.72f) * 10f)
            lumaValue.toInt().toByte()
        }

        val baseline = converter.convert(
            luma = luma,
            sourceWidth = size,
            sourceHeight = size,
            config = AsciiConfig(
                width = 28,
                height = 28,
                temporalSmoothing = 0f,
                autoToneMapping = true,
                charset = AsciiCharset.PORTRAIT,
                edgeEnhancement = 0.35f,
                centerFocusBoost = 0f,
            ),
        ).asText()

        val focused = converter.convert(
            luma = luma,
            sourceWidth = size,
            sourceHeight = size,
            config = AsciiConfig(
                width = 28,
                height = 28,
                temporalSmoothing = 0f,
                autoToneMapping = true,
                charset = AsciiCharset.PORTRAIT,
                edgeEnhancement = 0.35f,
                centerFocusBoost = 0.95f,
                centerFocusRadius = 0.45f,
            ),
        ).asText()

        assertNotEquals(baseline, focused)
    }
}
