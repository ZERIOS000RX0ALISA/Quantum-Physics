package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ScientificPrimaryDark,
    secondary = ScientificSecondaryDark,
    tertiary = ScientificTertiaryDark,
    background = ScientificBackgroundDark,
    surface = ScientificSurfaceDark,
    onPrimary = ScientificOnPrimaryDark,
    onBackground = ScientificOnBackgroundDark,
    onSurface = ScientificOnSurfaceDark,
    surfaceVariant = ScientificSurfaceDark,
    onSurfaceVariant = ScientificSecondaryTextDark,
    outline = ScientificBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = ScientificPrimaryLight,
    secondary = ScientificSecondaryLight,
    tertiary = ScientificTertiaryLight,
    background = ScientificBackgroundLight,
    surface = ScientificSurfaceLight,
    onPrimary = ScientificOnPrimaryLight,
    onBackground = ScientificOnBackgroundLight,
    onSurface = ScientificOnSurfaceLight,
    surfaceVariant = ScientificSurfaceLight,
    onSurfaceVariant = ScientificOnSurfaceLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
