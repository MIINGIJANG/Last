package com.last.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LastPrimary,
    background = LastBackground,
    surface = Color.White,
    onBackground = LastTextPrimary,
    onSurface = LastTextPrimary,
)

@Composable
fun LastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content,
    )
}
