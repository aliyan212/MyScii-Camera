package com.fossift.asciicam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = DarkBackground,
    primaryContainer = CyberCyanDark,
    onPrimaryContainer = CyberCyanMuted,
    secondary = NeonMint,
    onSecondary = DarkBackground,
    secondaryContainer = NeonMintDark,
    onSecondaryContainer = NeonMintLight,
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
