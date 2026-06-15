package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CosmicColorScheme = darkColorScheme(
    primary = VibrantBlue,
    secondary = LaserCrimson,
    tertiary = ElectroCyan,
    background = SpaceBlack,
    surface = TechnoBlue,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicColorScheme,
        typography = Typography,
        content = content
    )
}
