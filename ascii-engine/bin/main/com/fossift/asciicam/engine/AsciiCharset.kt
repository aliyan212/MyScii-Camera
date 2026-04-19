package com.fossift.asciicam.engine

enum class AsciiCharset(val glyphs: CharArray) {
    CLASSIC("@%#*+=-:. ".toCharArray()),
    MINIMAL("#*:. ".toCharArray()),
    PORTRAIT("MWNXK0Okxdolc:;..  ".toCharArray());

    init {
        require(glyphs.isNotEmpty()) { "Charset must contain at least one glyph" }
    }
}
