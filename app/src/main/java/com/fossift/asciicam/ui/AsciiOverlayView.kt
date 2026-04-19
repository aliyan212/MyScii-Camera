package com.fossift.asciicam.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Shader
import android.util.TypedValue
import android.view.View
import com.fossift.asciicam.engine.AsciiFrame

class AsciiOverlayView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = false
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.LEFT
        color = 0xFFFFFFFF.toInt()
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            7f,
            resources.displayMetrics,
        )
    }

    private var lineHeight: Float = paint.fontSpacing
    private var frame: AsciiFrame? = null
    private var solidColorInt: Int = 0xFFFFFFFF.toInt()
    private var neonMode: Boolean = false
    private var neonShader: Shader? = null

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
        frame = nextFrame
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

        val chars = current.chars
        val rowWidth = current.width
        var srcIndex = 0
        var baselineY = lineHeight
        for (row in 0 until current.height) {
            canvas.drawText(chars, srcIndex, rowWidth, 0f, baselineY, paint)
            srcIndex += rowWidth
            baselineY += lineHeight
        }
    }
}
