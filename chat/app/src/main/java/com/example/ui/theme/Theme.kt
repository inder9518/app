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
    primary = Color(0xFF86C3FF),
    secondary = Color(0xFFC3C7D2),
    tertiary = Color(0xFFFFB3B3),
    background = Color(0xFF131316),
    surface = Color(0xFF1D2024),
    onPrimary = Color(0xFF003254),
    onSecondary = Color(0xFF2C313B),
    onTertiary = Color(0xFF681515),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7D2)
)

private val LightColorScheme = lightColorScheme(
    primary = ProfessionalPrimary,
    secondary = ProfessionalSecondary,
    tertiary = Color(0xFF904B4B),
    background = ProfessionalBackground,
    surface = ProfessionalSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = ProfessionalSurfaceVariant,
    onSurfaceVariant = ProfessionalSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We default to false to showcase the authentic News Bharat red/yellow theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
