package com.yoyuzh.cliplink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GraphiteLight = lightColorScheme(
    primary = Color(0xFF30343B),
    onPrimary = Color.White,
    secondary = Color(0xFF58606B),
    background = Color(0xFFFAFAFB),
    surface = Color.White,
    onSurface = Color(0xFF2B3037),
    surfaceVariant = Color(0xFFE8EAED),
    outline = Color(0xFFD7DADE)
)

private val GraphiteDark = darkColorScheme(
    primary = Color(0xFFDDE1E7),
    onPrimary = Color(0xFF20242A),
    secondary = Color(0xFFB5BDC8),
    background = Color(0xFF1F2329),
    surface = Color(0xFF282D34),
    onSurface = Color(0xFFEDEFF2),
    surfaceVariant = Color(0xFF373D46),
    outline = Color(0xFF4A515C)
)

@Composable
fun ClipLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors: ColorScheme = if (darkTheme) GraphiteDark else GraphiteLight
    MaterialTheme(colorScheme = colors, content = content)
}
