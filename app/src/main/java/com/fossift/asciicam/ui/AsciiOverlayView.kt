package com.fossift.asciicam.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import com.fossift.asciicam.engine.AsciiFrame
import kotlin.math.max
import kotlin.math.min

class AsciiOverlayView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = false
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.LEFT
        color = 0xFFFFFFFF.toInt()
    }

    private var frame: AsciiFrame? = null
    private var solidColorInt: Int = 0xFFFFFFFF.toInt()
    private var neonMode: Boolean = false
    private var neonShader: Shader? = null

    // Layout metrics
    private var lastFrameWidth: Int = 0
    private var lastFrameHeight: Int = 0
    private var calculatedTextSize: Float = 14f
    private var calculatedLineHeight: Float = 16f
    private var startOffsetX: Float = 0f
    private var startOffsetY: Float = 0f

    fun setAsciiColorInt(color: Int) {
        if (solidColorInt != color) {
            solidColorInt = color
            if (!neonMode) {
                paint.color = color
            }
            invalidate()
        }
    }

    fun setNeonMode(enabled: Boolean) {
        if (neonMode != enabled) {
            neonMode = enabled
            paint.shader = if (enabled) neonShader else null
            if (!enabled) {
                paint.color = solidColorInt
            }
            invalidate()
        }
    }

    fun setAsciiFrame(nextFrame: AsciiFrame) {
        val sizeChanged = nextFrame.width != lastFrameWidth || nextFrame.height != lastFrameHeight
        frame = nextFrame
        if (sizeChanged) {
            lastFrameWidth = nextFrame.width
            lastFrameHeight = nextFrame.height
            recalculateLayoutMetrics()
        }
        postInvalidateOnAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        neonShader = if (w > 0 && h > 0) {
            LinearGradient(
                0f,
                0f,
                w.toFloat(),
                h.toFloat(),
                intArrayOf(
                    0xFFFF4D6D.toInt(),
                    0xFFFFB547.toInt(),
                    0xFF7CFF6B.toInt(),
                    0xFF56D7FF.toInt(),
                    0xFFB06DFF.toInt(),
                    0xFFFF4D6D.toInt(),
                ),
                floatArrayOf(0f, 0.2f, 0.42f, 0.66f, 0.84f, 1f),
                Shader.TileMode.CLAMP,
            )
        } else {
            null
        }
        if (neonMode) {
            paint.shader = neonShader
        }
        recalculateLayoutMetrics()
    }

    private fun recalculateLayoutMetrics() {
        val w = width
        val h = height
        val cols = lastFrameWidth
        val rows = lastFrameHeight
        if (w <= 0 || h <= 0 || cols <= 0 || rows <= 0) return

        // Measure reference monospace character ratios at reference size 100f
        val refSize = 100f
        paint.textSize = refSize
        val refCharWidth = paint.measureText("M")
        val refLineHeight = paint.fontSpacing

        val charRatioX = refCharWidth / refSize
        val lineRatioY = refLineHeight / refSize

        // Calculate max text size that fits both horizontally and vertically
        val fitX = (w.toFloat() / (cols * charRatioX))
        val fitY = (h.toFloat() / (rows * lineRatioY))
        calculatedTextSize = max(6f, min(fitX, fitY))

        paint.textSize = calculatedTextSize
        val charWidth = paint.measureText("M")
        calculatedLineHeight = paint.fontSpacing

        val totalGridWidth = cols * charWidth
        val totalGridHeight = rows * calculatedLineHeight

        startOffsetX = max(0f, (w - totalGridWidth) / 2f)
        val metrics = paint.fontMetrics
        val ascent = -metrics.ascent
        startOffsetY = max(0f, (h - totalGridHeight) / 2f) + ascent
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = frame ?: return

        if (neonMode) {
            paint.shader = neonShader
        } else {
            paint.shader = null
            paint.color = solidColorInt
        }

        paint.textSize = calculatedTextSize
        val chars = current.chars
        val rowWidth = current.width
        var srcIndex = 0
        var baselineY = startOffsetY
        val x = startOffsetX
        val lineHeight = calculatedLineHeight

        for (row in 0 until current.height) {
            canvas.drawText(chars, srcIndex, rowWidth, x, baselineY, paint)
            srcIndex += rowWidth
            baselineY += lineHeight
        }
    }
}
