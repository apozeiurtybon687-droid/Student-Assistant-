package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.local.ThemeMode
import com.example.data.local.UserGender

// CompositionLocal for convenient access to gender and theme mode in UI components
val LocalUserGender = staticCompositionLocalOf { UserGender.MALE }
val LocalIsDarkMode = staticCompositionLocalOf { false }

// =====================================
// Male Color Schemes (Blue)
// =====================================
private val MaleLightColorScheme = lightColorScheme(
    primary = MaleBluePrimary,
    onPrimary = Color.White,
    primaryContainer = MaleBlueContainerLight,
    onPrimaryContainer = MaleBlueOnContainerLight,
    secondary = MaleBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = AmberAccent,
    background = MaleLightBackground,
    onBackground = LightTextPrimary,
    surface = MaleLightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = MaleLightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = ErrorRed,
    onError = Color.White
)

private val MaleDarkColorScheme = darkColorScheme(
    primary = MaleBluePrimaryDark,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = MaleBlueContainerDark,
    onPrimaryContainer = MaleBlueOnContainerDark,
    secondary = MaleBlueSecondaryDark,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = AmberAccent,
    background = MaleDarkBackground,
    onBackground = DarkTextPrimary,
    surface = MaleDarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = MaleDarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    error = Color(0xFFF87171),
    onError = Color.White
)

// =====================================
// Female Color Schemes (Pink)
// =====================================
private val FemaleLightColorScheme = lightColorScheme(
    primary = FemalePinkPrimary,
    onPrimary = Color.White,
    primaryContainer = FemalePinkContainerLight,
    onPrimaryContainer = FemalePinkOnContainerLight,
    secondary = FemalePinkSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4E6),
    onSecondaryContainer = Color(0xFF9F1239),
    tertiary = AmberAccent,
    background = FemaleLightBackground,
    onBackground = LightTextPrimary,
    surface = FemaleLightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = FemaleLightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = ErrorRed,
    onError = Color.White
)

private val FemaleDarkColorScheme = darkColorScheme(
    primary = FemalePinkPrimaryDark,
    onPrimary = Color(0xFF261522),
    primaryContainer = FemalePinkContainerDark,
    onPrimaryContainer = FemalePinkOnContainerDark,
    secondary = FemalePinkSecondaryDark,
    onSecondary = Color(0xFF261522),
    secondaryContainer = Color(0xFF881337),
    onSecondaryContainer = Color(0xFFFECDD3),
    tertiary = AmberAccent,
    background = FemaleDarkBackground,
    onBackground = DarkTextPrimary,
    surface = FemaleDarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = FemaleDarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    error = Color(0xFFF87171),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    gender: UserGender = UserGender.MALE,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDark
    }

    val colorScheme: ColorScheme = when (gender) {
        UserGender.MALE -> if (isDark) MaleDarkColorScheme else MaleLightColorScheme
        UserGender.FEMALE -> if (isDark) FemaleDarkColorScheme else FemaleLightColorScheme
    }

    CompositionLocalProvider(
        LocalUserGender provides gender,
        LocalIsDarkMode provides isDark
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
