package com.satwik.oodapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryMain,
    onPrimary = Color.White,
    primaryContainer = Color(0x33FF5E36),
    onPrimaryContainer = PrimaryLight,
    secondary = SecondaryMain,
    onSecondary = Color.White,
    secondaryContainer = Color(0x263B82F6),
    onSecondaryContainer = Color(0xFF93C5FD),
    tertiary = AccentColor,
    onTertiary = Color.White,
    tertiaryContainer = Color(0x268B5CF6),
    onTertiaryContainer = Color(0xFFD8B4FE),
    background = NeutralDark,
    onBackground = TextWhitePrimary,
    surface = NeutralDarkSurface,
    onSurface = TextWhitePrimary,
    surfaceVariant = Color(0xFF1E283D),
    onSurfaceVariant = TextWhiteSecondary,
    outline = Color(0xFF334155),
    outlineVariant = Color(0x3364748B),
    error = ErrorRed,
    errorContainer = Color(0x33EF4444),
    onErrorContainer = Color(0xFFFCA5A5)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryMain,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFECE5),
    onPrimaryContainer = PrimaryDark,
    secondary = SecondaryMain,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF6FF),
    onSecondaryContainer = Color(0xFF1D4ED8),
    tertiary = AccentColor,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF6B21A8),
    background = NeutralLight,
    onBackground = TextDarkPrimary,
    surface = Color.White,
    onSurface = TextDarkPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextDarkSecondary,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = ErrorRed,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFFB91C1C)
)

@Composable
fun OodapplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

