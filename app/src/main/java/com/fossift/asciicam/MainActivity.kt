package com.fossift.asciicam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fossift.asciicam.ui.AsciiCameraScreen
import com.fossift.asciicam.ui.theme.MySciiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MySciiTheme {
                AsciiCameraScreen()
            }
        }
    }
}
