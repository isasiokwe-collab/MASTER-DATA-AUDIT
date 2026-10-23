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

private val HighContrastLightColorScheme = lightColorScheme(
    primary = Navy800,
    onPrimary = PureWhite,
    primaryContainer = Blue100,
    onPrimaryContainer = Navy900,
    secondary = Blue600,
    onSecondary = PureWhite,
    secondaryContainer = Blue50,
    onSecondaryContainer = Navy800,
    tertiary = Amber700,
    onTertiary = PureWhite,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber700,
    background = Color(0xFFF8FAFC),
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate800,
    outline = Slate600,
    outlineVariant = Slate400,
    error = Red700,
    onError = PureWhite,
    errorContainer = Red100,
    onErrorContainer = Red700
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Navy900,
    primaryContainer = Navy800,
    onPrimaryContainer = Blue100,
    secondary = Color(0xFF38BDF8),
    onSecondary = Slate900,
    secondaryContainer = Slate800,
    onSecondaryContainer = PureWhite,
    tertiary = Color(0xFFFBBF24),
    onTertiary = Slate900,
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Amber100,
    background = Slate900,
    onBackground = PureWhite,
    surface = Slate800,
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Slate200,
    outline = Slate400,
    outlineVariant = Slate600,
    error = Color(0xFFF87171),
    onError = Slate900,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Red100
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Forced off by default to maintain strict outdoor high contrast
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> HighContrastDarkColorScheme
        else -> HighContrastLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
