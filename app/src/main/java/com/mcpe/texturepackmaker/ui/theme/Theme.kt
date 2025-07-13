package com.mcpe.texturepackmaker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB6C1), // Light pink
    secondary = Color(0xFFFFC0CB), // Lighter pink
    tertiary = Color(0xFFFF69B4), // Hot pink
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF2D2D2D),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    primaryContainer = Color(0xFFFFB6C1).copy(alpha = 0.2f),
    onPrimaryContainer = Color.Black,
    secondaryContainer = Color(0xFFFFC0CB).copy(alpha = 0.2f),
    onSecondaryContainer = Color.Black,
    surfaceVariant = Color(0xFF404040),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF79747E),
    inverseOnSurface = Color(0xFF1A1A1A),
    inverseSurface = Color(0xFFE0E0E0),
    inversePrimary = Color(0xFFFFB6C1),
    surfaceTint = Color(0xFFFFB6C1),
    outlineVariant = Color(0xFF49454F),
    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFFB6C1), // Light pink
    secondary = Color(0xFFFFC0CB), // Lighter pink
    tertiary = Color(0xFFFF69B4), // Hot pink
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    primaryContainer = Color(0xFFFFB6C1).copy(alpha = 0.2f),
    onPrimaryContainer = Color.Black,
    secondaryContainer = Color(0xFFFFC0CB).copy(alpha = 0.2f),
    onSecondaryContainer = Color.Black,
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    inverseOnSurface = Color(0xFFF4EFF4),
    inverseSurface = Color(0xFF313033),
    inversePrimary = Color(0xFFFFB6C1),
    surfaceTint = Color(0xFFFFB6C1),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color.Black
)

@Composable
fun MCPETexturePackMakerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}