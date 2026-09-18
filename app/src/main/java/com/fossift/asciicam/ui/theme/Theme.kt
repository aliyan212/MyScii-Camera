package com.fossift.asciicam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = DarkBackground,
    primaryContainer = CyberCyanDark,
    onPrimaryContainer = CyberCyanMuted,
    secondary = NeonMint,
    onSecondary = DarkBackground,
    secondaryContainer = NeonMintDark,
    onSecondaryContainer = Color(0xFF73FDBE),
    tertiary = SolarAmber,
    onTertiary = SolarAmberDark,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineSubtle,
    error = AlertRed,
    onError = DarkBackground,
)

@Composable
fun MySciiTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
