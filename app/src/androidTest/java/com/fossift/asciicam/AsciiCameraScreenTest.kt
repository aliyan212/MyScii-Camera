package com.fossift.asciicam

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test

class AsciiCameraScreenTest {
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun cameraScreen_switchesToFullScreenGallery() {
        composeRule.onNodeWithText("Gallery").assertIsDisplayed()
        composeRule.onNodeWithText("Menu").assertIsDisplayed()

        composeRule.onNodeWithText("Gallery").performClick()

        composeRule.onNodeWithText("MySCII Gallery").assertIsDisplayed()
        composeRule.onNodeWithText("Back to camera").assertIsDisplayed()
    }
}
