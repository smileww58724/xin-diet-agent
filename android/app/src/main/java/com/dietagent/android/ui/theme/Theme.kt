package com.dietagent.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = PanelSoft,
    onPrimaryContainer = Ink,
    secondary = TextSecondary,
    onSecondary = Color.White,
    background = Background,
    onBackground = TextPrimary,
    surface = Panel,
    onSurface = TextPrimary,
    surfaceVariant = PanelSoft,
    onSurfaceVariant = TextSecondary,
    outline = Line,
    error = Danger,
    onError = Color.White,
)

@Composable
fun NutriMuseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
