package com.fossift.asciicam.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.fossift.asciicam.engine.AsciiConfig
import com.fossift.asciicam.engine.AsciiConverter
import com.fossift.asciicam.engine.AsciiFrame

class AsciiFrameAnalyzer(
    private val converter: AsciiConverter,
    private val configProvider: () -> AsciiConfig,
    private val mirrorHorizontallyProvider: () -> Boolean = { false },
    private val maxAnalysisFps: Int = 20,
    private val onAsciiFrame: (frame: AsciiFrame, processingMs: Double) -> Unit,
) : ImageAnalysis.Analyzer {

    private var compactBuffer: ByteArray = ByteArray(0)
    private var rotatedBuffer: ByteArray = ByteArray(0)
    private var mirroredBuffer: ByteArray = ByteArray(0)
    private var lastAnalyzeNs: Long = 0L

    override fun analyze(image: ImageProxy) {
        try {
            if (image.planes.isEmpty()) {
                return
            }

            if (maxAnalysisFps > 0) {
                val now = System.nanoTime()
                val minIntervalNs = 1_000_000_000L / maxAnalysisFps
                if (now - lastAnalyzeNs < minIntervalNs) {
                    return
                }
                lastAnalyzeNs = now
            }

            if (image.width <= 0 || image.height <= 0) {
                return
            }

            val compactFrame = extractCompactLuma(image)
            val orientedFrame = orientLuma(
                luma = compactFrame.luma,
                width = compactFrame.width,
                height = compactFrame.height,
                rotationDegrees = image.imageInfo.rotationDegrees,
                mirrorHorizontally = mirrorHorizontallyProvider(),
            )

            val startNs = System.nanoTime()
            val frame = converter.convert(
                luma = orientedFrame.luma,
                sourceWidth = orientedFrame.width,
                sourceHeight = orientedFrame.height,
                config = configProvider(),
            )
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
            onAsciiFrame(frame, elapsedMs)
        } finally {
            image.close()
        }
    }

    private fun extractCompactLuma(image: ImageProxy): LumaFrame {
        val yPlane = image.planes[0]
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride
        val width = image.width
        val height = image.height
        val source = yPlane.buffer

        val compact = ensureBuffer(width * height, BufferKind.COMPACT)
        if (rowStride == width && pixelStride == 1) {
            source.rewind()
            source.get(compact, 0, width * height)
            return LumaFrame(compact, width, height)
        }

        var dstIndex = 0
        for (y in 0 until height) {
            val rowOffset = y * rowStride
            for (x in 0 until width) {
                compact[dstIndex++] = source.get(rowOffset + (x * pixelStride))
            }
        }

        return LumaFrame(compact, width, height)
    }

    private fun orientLuma(
        luma: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        mirrorHorizontally: Boolean,
    ): LumaFrame {
        val rotated = rotateLuma(luma, width, height, rotationDegrees)
        if (!mirrorHorizontally) {
            return rotated
        }

        val mirrored = ensureBuffer(rotated.luma.size, BufferKind.MIRRORED)
        for (y in 0 until rotated.height) {
            val rowStart = y * rotated.width
            for (x in 0 until rotated.width) {
                val srcX = rotated.width - 1 - x
                mirrored[rowStart + x] = rotated.luma[rowStart + srcX]
            }
        }
        return LumaFrame(mirrored, rotated.width, rotated.height)
    }

    private fun rotateLuma(luma: ByteArray, width: Int, height: Int, rotationDegrees: Int): LumaFrame {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        if (normalizedRotation == 0) {
            return LumaFrame(luma, width, height)
        }

        return when (normalizedRotation) {
            90 -> {
                val outWidth = height
                val outHeight = width
                val out = ensureBuffer(luma.size, BufferKind.ROTATED)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val dstX = height - 1 - y
                        val dstY = x
                        out[(dstY * outWidth) + dstX] = luma[(y * width) + x]
                    }
                }
                LumaFrame(out, outWidth, outHeight)
            }

            180 -> {
                val out = ensureBuffer(luma.size, BufferKind.ROTATED)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val dstX = width - 1 - x
                        val dstY = height - 1 - y
                        out[(dstY * width) + dstX] = luma[(y * width) + x]
                    }
                }
                LumaFrame(out, width, height)
            }

            270 -> {
                val outWidth = height
                val outHeight = width
                val out = ensureBuffer(luma.size, BufferKind.ROTATED)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val dstX = y
                        val dstY = width - 1 - x
                        out[(dstY * outWidth) + dstX] = luma[(y * width) + x]
                    }
                }
                LumaFrame(out, outWidth, outHeight)
            }

            else -> LumaFrame(luma, width, height)
        }
    }

    private fun ensureBuffer(size: Int, kind: BufferKind): ByteArray {
        return when (kind) {
            BufferKind.COMPACT -> {
                if (compactBuffer.size < size) compactBuffer = ByteArray(size)
                compactBuffer
            }

            BufferKind.ROTATED -> {
                if (rotatedBuffer.size < size) rotatedBuffer = ByteArray(size)
                rotatedBuffer
            }

            BufferKind.MIRRORED -> {
                if (mirroredBuffer.size < size) mirroredBuffer = ByteArray(size)
                mirroredBuffer
            }
        }
    }

    private enum class BufferKind {
        COMPACT,
        ROTATED,
        MIRRORED,
    }

    private data class LumaFrame(
        val luma: ByteArray,
        val width: Int,
        val height: Int,
    )
}
