package com.example.hdcfuncap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HdcBlue,
    onPrimary = Color.White,
    primaryContainer = HdcBlueDark,
    onPrimaryContainer = Color.White,
    secondary = HdcGreen,
    onSecondary = HdcText,
    tertiary = HdcOrange,
    onTertiary = HdcText,
    background = Color(0xFF111827),
    onBackground = Color.White,
    surface = Color(0xFF1F2937),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF253044),
    onSurfaceVariant = Color(0xFFE5E7EB),
    error = HdcRed,
    onError = Color.White,
    outline = Color(0xFF64748B)
)

private val LightColorScheme = lightColorScheme(
    primary = HdcBlue,
    onPrimary = Color.White,
    primaryContainer = HdcBlueLight,
    onPrimaryContainer = HdcText,
    secondary = HdcGreen,
    onSecondary = HdcText,
    secondaryContainer = Color(0xFFE8F7ED),
    onSecondaryContainer = HdcText,
    tertiary = HdcOrange,
    onTertiary = HdcText,
    tertiaryContainer = Color(0xFFFFF2E8),
    onTertiaryContainer = HdcText,
    background = HdcBackground,
    onBackground = HdcText,
    surface = HdcSurface,
    onSurface = HdcText,
    surfaceVariant = HdcBlueLight,
    onSurfaceVariant = HdcTextSecondary,
    error = HdcRed,
    onError = Color.White,
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFEAF1FF)
)

@Composable
fun HdcFuncapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
