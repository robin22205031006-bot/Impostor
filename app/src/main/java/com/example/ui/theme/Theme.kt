package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class ImpostorRolePalette(
    val normalAccent: Color = NormalGreen,
    val normalGlow: Color = NormalGreenGlow,
    val normalSurface: Color = NormalGreenDeep,
    val normalBorder: Color = NormalGreenBorder,
    val impostorAccent: Color = ImpostorRed,
    val impostorGlow: Color = ImpostorRedGlow,
    val impostorSurface: Color = ImpostorRedDeep,
    val impostorBorder: Color = ImpostorRedBorder,
    val killerAccent: Color = KillerMagenta,
    val killerGlow: Color = KillerMagentaGlow,
    val killerSurface: Color = KillerMagentaDeep,
    val killerBorder: Color = KillerMagentaBorder,
    val saviorAccent: Color = SaviorAzure,
    val saviorGlow: Color = SaviorAzureGlow,
    val saviorSurface: Color = SaviorAzureDeep,
    val saviorBorder: Color = SaviorAzureBorder,
    val directorAccent: Color = DirectorGold,
    val directorGlow: Color = DirectorGoldGlow,
    val directorSurface: Color = DirectorGoldDeep,
    val directorBorder: Color = DirectorGoldBorder,
    val timerAccent: Color = AmberTimer,
    val timerSurface: Color = AmberContainer,
    val cyanAccent: Color = ElectricCyan,
    val cyanSurface: Color = ElectricCyanContainer,
    val cardElevated: Color = ObsidianSurfaceHigh,
    val cardBorder: Color = ObsidianOutline,
    val textMuted: Color = TextMuted
)

val LocalImpostorPalette = staticCompositionLocalOf { ImpostorRolePalette() }

private val ImpostorDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBg,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = TextPrimary,
    secondary = NormalGreen,
    onSecondary = ObsidianBg,
    secondaryContainer = NormalGreenDeep,
    onSecondaryContainer = NormalGreenGlow,
    tertiary = AmberTimer,
    onTertiary = ObsidianBg,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberTimer,
    error = ImpostorRed,
    onError = TextPrimary,
    errorContainer = ImpostorRedDeep,
    onErrorContainer = ImpostorRedGlow,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceBright,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF22293F)
)

private val ImpostorShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ImpostorTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalImpostorPalette provides ImpostorRolePalette()) {
        MaterialTheme(
            colorScheme = ImpostorDarkColorScheme,
            typography = ImpostorTypography,
            shapes = ImpostorShapes,
            content = content
        )
    }
}
