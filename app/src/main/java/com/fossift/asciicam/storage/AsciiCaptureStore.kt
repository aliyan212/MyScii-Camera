package com.fossift.asciicam.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class AsciiCaptureResult(
    val textPath: String,
    val pngPath: String,
)

data class CaptureRecord(
    val timestampUtc: String,
    val textFileName: String,
    val pngFileName: String,
)

class AsciiCaptureStore(private val context: Context) {
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").withZone(ZoneOffset.UTC)

    fun save(asciiText: String): AsciiCaptureResult {
        val capturesDir = File(context.filesDir, "captures").apply { mkdirs() }
        val stamp = timestampFormat.format(Instant.now())

        val txtFile = File(capturesDir, "capture_${stamp}.txt")
        txtFile.writeText(asciiText)

        val pngFile = File(capturesDir, "capture_${stamp}.png")
        FileOutputStream(pngFile).buffered().use { output ->
            renderTextToBitmap(asciiText).compress(Bitmap.CompressFormat.PNG, 100, output)
        }

        appendToIndex(
            CaptureRecord(
                timestampUtc = Instant.now().toString(),
                textFileName = txtFile.name,
                pngFileName = pngFile.name,
            ),
        )

        return AsciiCaptureResult(
            textPath = txtFile.absolutePath,
            pngPath = pngFile.absolutePath,
        )
    }

    fun listRecent(limit: Int = 12): List<CaptureRecord> {
        if (limit <= 0) {
            return emptyList()
        }

        val capturesDir = File(context.filesDir, "captures")
        val indexFile = File(capturesDir, "index.tsv")
        if (!indexFile.exists()) {
            return emptyList()
        }

        return indexFile.readLines()
            .asReversed()
            .asSequence()
            .mapNotNull { parseRecord(it) }
            .take(limit)
            .toList()
    }

    fun readCaptureText(record: CaptureRecord): String? {
        val file = File(File(context.filesDir, "captures"), record.textFileName)
        if (!file.exists()) {
            return null
        }

        return runCatching { file.readText() }.getOrNull()
    }

    fun resolveCapturePngFile(record: CaptureRecord): File? {
        val file = File(File(context.filesDir, "captures"), record.pngFileName)
        return if (file.exists()) file else null
    }

    private fun appendToIndex(record: CaptureRecord) {
        val capturesDir = File(context.filesDir, "captures").apply { mkdirs() }
        val indexFile = File(capturesDir, "index.tsv")
        indexFile.appendText("${record.timestampUtc}\t${record.textFileName}\t${record.pngFileName}\n")
    }

    private fun parseRecord(line: String): CaptureRecord? {
        val parts = line.split('\t')
        if (parts.size != 3) {
            return null
        }

        return CaptureRecord(
            timestampUtc = parts[0],
            textFileName = parts[1],
            pngFileName = parts[2],
        )
    }

    private fun renderTextToBitmap(asciiText: String): Bitmap {
        val lines = asciiText.split('\n')
        val maxChars = lines.maxOfOrNull { it.length } ?: 1
        val lineCount = lines.size.coerceAtLeast(1)

        val textSize = 18f
        val horizontalPadding = 24
        val verticalPadding = 24

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 247, 255)
            this.textSize = textSize
            typeface = android.graphics.Typeface.MONOSPACE
        }

        val fontMetrics = paint.fontMetrics
        val lineHeight = (fontMetrics.bottom - fontMetrics.top + 2f).toInt().coerceAtLeast(1)

        val width = (maxChars * textSize * 0.62f).toInt() + (horizontalPadding * 2)
        val height = (lineCount * lineHeight) + (verticalPadding * 2)

        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(15, 18, 24))

        var y = verticalPadding - fontMetrics.top
        for (line in lines) {
            canvas.drawText(line, horizontalPadding.toFloat(), y, paint)
            y += lineHeight
        }

        return bitmap
    }
}
