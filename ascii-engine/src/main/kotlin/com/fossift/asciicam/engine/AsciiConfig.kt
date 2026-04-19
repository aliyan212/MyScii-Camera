package com.fossift.asciicam.engine

data class AsciiConfig(
    val width: Int,
    val height: Int,
    val contrast: Float = 1.0f,
    val gamma: Float = 1.0f,
    val charset: AsciiCharset = AsciiCharset.CLASSIC,
    val temporalSmoothing: Float = 0.15f,
    val edgeEnhancement: Float = 0.25f,
    val autoToneMapping: Boolean = false,
    val toneClipLowPercent: Float = 0.03f,
    val toneClipHighPercent: Float = 0.02f,
    val centerFocusBoost: Float = 0f,
    val centerFocusRadius: Float = 0.7f,
) {
    init {
        require(width > 0) { "width must be > 0" }
        require(height > 0) { "height must be > 0" }
        require(contrast in 0.5f..2.5f) { "contrast must be in 0.5..2.5" }
        require(gamma in 0.4f..2.4f) { "gamma must be in 0.4..2.4" }
        require(temporalSmoothing in 0f..1f) { "temporalSmoothing must be in 0..1" }
        require(edgeEnhancement in 0f..0.7f) { "edgeEnhancement must be in 0..0.7" }
        require(toneClipLowPercent in 0f..0.2f) { "toneClipLowPercent must be in 0..0.2" }
        require(toneClipHighPercent in 0f..0.2f) { "toneClipHighPercent must be in 0..0.2" }
        require(centerFocusBoost in 0f..1f) { "centerFocusBoost must be in 0..1" }
        require(centerFocusRadius in 0.2f..1.5f) { "centerFocusRadius must be in 0.2..1.5" }
        require(toneClipLowPercent + toneClipHighPercent <= 0.4f) {
            "tone clip low+high must be <= 0.4"
        }
    }
}
