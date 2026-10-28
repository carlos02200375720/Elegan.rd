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
    primary = DarkBurgundy,
    onPrimary = EleganBurgundyDark,
    primaryContainer = DarkBurgundyContainer,
    onPrimaryContainer = Color(0xFFFFDADA),
    secondary = EleganGoldLight,
    onSecondary = Color(0xFF3F2E00),
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = Color(0xFFEDE0DF),
    onSurface = Color(0xFFEDE0DF),
    surfaceVariant = Color(0xFF332D2E),
    onSurfaceVariant = Color(0xFFD4C2C3)
)

private val LightColorScheme = lightColorScheme(
    primary = EleganBurgundy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADA),
    onPrimaryContainer = EleganBurgundyDark,
    secondary = EleganCharcoal,
    onSecondary = Color.White,
    tertiary = EleganGold,
    onTertiary = Color.White,
    background = EleganIvory,
    onBackground = EleganCharcoal,
    surface = EleganWarmWhite,
    onSurface = EleganCharcoal,
    surfaceVariant = EleganSurfaceContainer,
    onSurfaceVariant = EleganCharcoalLight,
    outline = EleganBorder
)

@Composable
fun EleganTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    EleganTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
