package com.fossift.asciicam.engine

data class AsciiFrame(
    val width: Int,
    val height: Int,
    val chars: CharArray,
) {
    init {
        require(width > 0 && height > 0) { "Frame dimensions must be > 0" }
        require(chars.size == width * height) { "Frame char buffer size mismatch" }
    }

    val lines: List<String> by lazy(LazyThreadSafetyMode.NONE) {
        List(height) { row ->
            val start = row * width
            String(chars, start, width)
        }
    }

    fun asText(): String {
        val out = CharArray((width * height) + (height - 1))
        var outIndex = 0
        var srcIndex = 0
        for (row in 0 until height) {
            for (x in 0 until width) {
                out[outIndex++] = chars[srcIndex++]
            }
            if (row < height - 1) {
                out[outIndex++] = '\n'
            }
        }
        return String(out)
    }
}
