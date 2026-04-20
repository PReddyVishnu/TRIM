package com.vishnu.trim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE50914), // NeonRed
    background = Color(0xFF000000), // BackgroundBlack
    surface = Color(0xFF121212) // CardDark
)

@Composable
fun TRIMTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
