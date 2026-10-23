package com.appvin.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val Burgundy = Color(0xFF6B1E2E)

private val LightColors = lightColorScheme(
    primary = Burgundy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DD),
    onPrimaryContainer = Color(0xFF3F0010),
    secondary = Color(0xFF775656),
    secondaryContainer = Color(0xFFFFDAD9),
    tertiary = Color(0xFF6E5D2E),
    tertiaryContainer = Color(0xFFF9E1A6),
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFF8F5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB2BB),
    onPrimary = Color(0xFF5F1124),
    primaryContainer = Color(0xFF7D2939),
    onPrimaryContainer = Color(0xFFFFD9DD),
    secondary = Color(0xFFE6BDBC),
    tertiary = Color(0xFFDCC58C),
)

/** Matches the Pixel's wallpaper colours (Material You) when available, otherwise a burgundy palette. */
@Composable
fun AppVinTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> DarkColors
        else -> LightColors
    }
    val base = Typography()
    val typography = base.copy(
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
