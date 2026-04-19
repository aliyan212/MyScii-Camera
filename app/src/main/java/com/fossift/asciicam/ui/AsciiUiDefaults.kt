package com.fossift.asciicam.ui

object AsciiUiDefaults {
    const val baseWidth = 128
    const val baseHeight = 72
    const val minWidth = 40
    const val maxWidth = 180
    const val minHeight = 24
    const val maxHeight = 100

    fun gridForDensity(densityScale: Float): Pair<Int, Int> {
        val width = (baseWidth * densityScale).toInt().coerceIn(minWidth, maxWidth)
        val height = (baseHeight * densityScale).toInt().coerceIn(minHeight, maxHeight)
        return width to height
    }
}
