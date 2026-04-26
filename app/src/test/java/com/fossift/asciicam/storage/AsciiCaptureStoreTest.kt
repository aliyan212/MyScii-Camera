package com.fossift.asciicam.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AsciiCaptureStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val capturesDir = File(context.filesDir, "captures")

    @Before
    fun setUp() {
        capturesDir.deleteRecursively()
        capturesDir.mkdirs()
    }

    @Test
    fun deleteCapture_removesFilesAndIndexEntry() {
        val textFile = File(capturesDir, "capture_test.txt")
        val pngFile = File(capturesDir, "capture_test.png")
        val indexFile = File(capturesDir, "index.tsv")
        val record = CaptureRecord(
            timestampUtc = "2026-04-26T00:00:00Z",
            textFileName = textFile.name,
            pngFileName = pngFile.name,
        )

        textFile.writeText("ascii")
        pngFile.writeBytes(byteArrayOf(1, 2, 3))
        indexFile.writeText("${record.timestampUtc}\t${record.textFileName}\t${record.pngFileName}\n")

        val deleted = AsciiCaptureStore(context).deleteCapture(record)

        assertTrue(deleted)
        assertFalse(textFile.exists())
        assertFalse(pngFile.exists())
        assertFalse(indexFile.exists())
        assertTrue(AsciiCaptureStore(context).listRecent().isEmpty())
    }
}
