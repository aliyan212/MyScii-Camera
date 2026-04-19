package com.fossift.asciicam.engine

class AdaptiveQualityController(
    private val minWidth: Int,
    private val minHeight: Int,
    private val maxWidth: Int,
    private val maxHeight: Int,
    private val frameBudgetMs: Double = 33.0,
) {
    init {
        require(minWidth in 1..maxWidth)
        require(minHeight in 1..maxHeight)
    }

    private var avgFrameTimeMs: Double = frameBudgetMs
    private var updateCounter: Int = 0
    private var lastScaleUpCounter: Int = -1000
    private var lastScaleDownCounter: Int = -1000

    fun update(current: AsciiConfig, measuredFrameTimeMs: Double): AsciiConfig {
        val alpha = 0.12
        avgFrameTimeMs = (alpha * measuredFrameTimeMs) + ((1.0 - alpha) * avgFrameTimeMs)
        updateCounter++

        return when {
            avgFrameTimeMs > frameBudgetMs * 1.25 && (updateCounter - lastScaleDownCounter) >= 6 -> {
                lastScaleDownCounter = updateCounter
                scale(current, 0.90f)
            }
            avgFrameTimeMs < frameBudgetMs * 0.60 && (updateCounter - lastScaleUpCounter) >= 12 -> {
                lastScaleUpCounter = updateCounter
                scale(current, 1.04f)
            }
            else -> current
        }
    }

    private fun scale(current: AsciiConfig, factor: Float): AsciiConfig {
        val nextWidth = (current.width * factor).toInt().coerceIn(minWidth, maxWidth)
        val nextHeight = (current.height * factor).toInt().coerceIn(minHeight, maxHeight)

        if (nextWidth == current.width && nextHeight == current.height) {
            return current
        }

        return current.copy(width = nextWidth, height = nextHeight)
    }
}
