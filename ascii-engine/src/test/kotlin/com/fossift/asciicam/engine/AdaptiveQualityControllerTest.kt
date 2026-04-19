package com.fossift.asciicam.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class AdaptiveQualityControllerTest {
    @Test
    fun `controller scales down when over budget`() {
        val controller = AdaptiveQualityController(
            minWidth = 40,
            minHeight = 24,
            maxWidth = 200,
            maxHeight = 112,
            frameBudgetMs = 33.0,
        )

        val current = AsciiConfig(width = 120, height = 68)
        val next = controller.update(current, measuredFrameTimeMs = 50.0)

        assertTrue(next.width <= current.width)
        assertTrue(next.height <= current.height)
    }

    @Test
    fun `controller scales up when under budget`() {
        val controller = AdaptiveQualityController(
            minWidth = 40,
            minHeight = 24,
            maxWidth = 200,
            maxHeight = 112,
            frameBudgetMs = 33.0,
        )

        val current = AsciiConfig(width = 80, height = 45)

        var next = current
        repeat(4) {
            next = controller.update(next, measuredFrameTimeMs = 10.0)
        }

        assertTrue(next.width >= current.width)
        assertTrue(next.height >= current.height)
    }
}
