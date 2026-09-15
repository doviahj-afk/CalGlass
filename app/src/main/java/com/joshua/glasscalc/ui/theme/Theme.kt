package com.joshua.glasscalc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GlassColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentPurple,
    background = BgBottom,
    surface = BgMid,
    onPrimary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun GlassCalcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GlassColorScheme,
        typography = GlassTypography,
        content = content
    )
}
