package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PicoCyan,
    onPrimary = PicoNavyDarkest,
    primaryContainer = PicoNavyCard,
    onPrimaryContainer = PicoCyan,
    secondary = PicoViolet,
    onSecondary = PicoNavyDarkest,
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = PicoViolet,
    tertiary = PicoIndigo,
    onTertiary = Color.White,
    background = PicoNavyDarkest,
    onBackground = PicoTextPrimary,
    surface = PicoNavySurface,
    onSurface = PicoTextPrimary,
    surfaceVariant = PicoNavyCard,
    onSurfaceVariant = PicoTextSecondary,
    outline = PicoNavyBorder,
    error = PicoError,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = PicoLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = PicoLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = Color(0xFF4F46E5),
    onTertiary = Color.White,
    background = PicoLightBg,
    onBackground = PicoLightTextPrimary,
    surface = PicoLightSurface,
    onSurface = PicoLightTextPrimary,
    surfaceVariant = PicoLightCard,
    onSurfaceVariant = PicoLightTextSecondary,
    outline = PicoLightBorder,
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun PicoAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Default false to preserve Pico AI's signature deep navy aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = PicoAiTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
