package com.fossift.asciicam.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AsciiUiDefaultsTest {
    @Test
    fun gridForDensity_returnsExpectedBaseGrid() {
        val (width, height) = AsciiUiDefaults.gridForDensity(1.0f)
        assertEquals(128, width)
        assertEquals(72, height)
    }

    @Test
    fun gridForDensity_clampsAtLowerBounds() {
        val (width, height) = AsciiUiDefaults.gridForDensity(0.1f)
        assertEquals(AsciiUiDefaults.minWidth, width)
        assertEquals(AsciiUiDefaults.minHeight, height)
    }

    @Test
    fun gridForDensity_clampsAtUpperBounds() {
        val (width, height) = AsciiUiDefaults.gridForDensity(5.0f)
        assertEquals(AsciiUiDefaults.maxWidth, width)
        assertEquals(AsciiUiDefaults.maxHeight, height)
    }

    @Test
    fun gridForDensity_isMonotonicForNormalRange() {
        val (smallW, smallH) = AsciiUiDefaults.gridForDensity(0.8f)
        val (bigW, bigH) = AsciiUiDefaults.gridForDensity(1.2f)
        assertTrue(bigW >= smallW)
        assertTrue(bigH >= smallH)
    }
}
