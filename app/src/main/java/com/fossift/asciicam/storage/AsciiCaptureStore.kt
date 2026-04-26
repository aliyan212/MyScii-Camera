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
    val originalPngPath: String? = null,
)

data class CaptureRecord(
    val timestampUtc: String,
    val textFileName: String,
    val pngFileName: String,
    val originalPngFileName: String? = null,
)

class AsciiCaptureStore(private val context: Context) {
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").withZone(ZoneOffset.UTC)

    fun save(asciiText: String, originalBitmap: Bitmap? = null): AsciiCaptureResult {
        val capturesDir = capturesDir()
        val stamp = timestampFormat.format(Instant.now())

        val txtFile = File(capturesDir, "capture_${stamp}.txt")
        txtFile.writeText(asciiText)

        val pngFile = File(capturesDir, "capture_${stamp}.png")
        FileOutputStream(pngFile).buffered().use { output ->
            renderTextToBitmap(asciiText).compress(Bitmap.CompressFormat.PNG, 100, output)
        }

        val originalPngFile = originalBitmap?.let {
            File(capturesDir, "capture_${stamp}_original.png").also { file ->
                FileOutputStream(file).buffered().use { output ->
                    it.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
            }
        }

        appendToIndex(
            CaptureRecord(
                timestampUtc = Instant.now().toString(),
                textFileName = txtFile.name,
                pngFileName = pngFile.name,
                originalPngFileName = originalPngFile?.name,
            ),
        )

        return AsciiCaptureResult(
            textPath = txtFile.absolutePath,
            pngPath = pngFile.absolutePath,
            originalPngPath = originalPngFile?.absolutePath,
        )
    }

    fun deleteCapture(record: CaptureRecord): Boolean {
        val capturesDir = capturesDir()
        var deleted = false

        deleted = File(capturesDir, record.textFileName).delete() || deleted
        deleted = File(capturesDir, record.pngFileName).delete() || deleted
        if (record.originalPngFileName != null) {
            deleted = File(capturesDir, record.originalPngFileName).delete() || deleted
        }

        val indexFile = captureIndexFile()
        if (!indexFile.exists()) {
            return deleted
        }

        val remainingEntries = indexFile.readLines().filterNot { line ->
            val parsed = parseRecord(line) ?: return@filterNot false
            parsed.textFileName == record.textFileName ||
                parsed.pngFileName == record.pngFileName ||
                (record.originalPngFileName != null && parsed.originalPngFileName == record.originalPngFileName)
        }

        if (remainingEntries.isEmpty()) {
            indexFile.delete()
        } else {
            indexFile.writeText(remainingEntries.joinToString(separator = "\n", postfix = "\n"))
        }

        return deleted
    }

    fun listRecent(limit: Int = 12): List<CaptureRecord> {
        if (limit <= 0) {
            return emptyList()
        }

        val indexFile = captureIndexFile()
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
        val file = File(capturesDir(), record.textFileName)
        if (!file.exists()) {
            return null
        }

        return runCatching { file.readText() }.getOrNull()
    }

    fun resolveCapturePngFile(record: CaptureRecord): File? {
        val file = File(capturesDir(), record.pngFileName)
        return if (file.exists()) file else null
    }

    fun resolveCaptureOriginalPngFile(record: CaptureRecord): File? {
        val fileName = record.originalPngFileName ?: return null
        val file = File(capturesDir(), fileName)
        return if (file.exists()) file else null
    }

    private fun appendToIndex(record: CaptureRecord) {
        val indexFile = captureIndexFile()
        val originalField = record.originalPngFileName ?: ""
        indexFile.appendText("${record.timestampUtc}\t${record.textFileName}\t${record.pngFileName}\t${originalField}\n")
    }

    private fun capturesDir(): File {
        return File(context.filesDir, "captures").apply { mkdirs() }
    }

    private fun captureIndexFile(): File {
        return File(capturesDir(), "index.tsv")
    }

    private fun parseRecord(line: String): CaptureRecord? {
        val parts = line.split('\t')
        if (parts.size < 3) {
            return null
        }

        val originalFileName = parts.getOrNull(3)?.takeIf { it.isNotBlank() }

        return CaptureRecord(
            timestampUtc = parts[0],
            textFileName = parts[1],
            pngFileName = parts[2],
            originalPngFileName = originalFileName,
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
