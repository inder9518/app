package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Violet400,
    onPrimary = SophisticatedBg,
    primaryContainer = Violet900_20,
    onPrimaryContainer = Violet100,
    secondary = Emerald400,
    onSecondary = SophisticatedBg,
    secondaryContainer = Emerald900_20,
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Amber400,
    onTertiary = SophisticatedBg,
    tertiaryContainer = Color(0x3378350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = SophisticatedBg,
    onBackground = SophisticatedText,
    surface = Zinc900,
    onSurface = SophisticatedText,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc400,
    outline = Zinc800
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // We enforce our premium "Sophisticated Dark" ColorScheme to style the app identically across all platforms
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
