package com.studyflow.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = AccentViolet,
    onPrimary = TextLight,
    primaryContainer = AccentViolet.copy(alpha = 0.15f),
    onPrimaryContainer = AccentViolet,
    secondary = AccentCyan,
    onSecondary = TextLight,
    secondaryContainer = AccentCyan.copy(alpha = 0.15f),
    onSecondaryContainer = AccentCyan,
    tertiary = AccentAmber,
    onTertiary = TextDark,
    background = DarkBackground,
    onBackground = TextLight,
    surface = DarkSurface,
    onSurface = TextLight,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextMutedLight,
    error = VibrantRed,
    onError = TextLight,
    outline = DarkCard
)

private val LightColorScheme = lightColorScheme(
    primary = AccentVioletLight,
    onPrimary = TextLight,
    primaryContainer = AccentVioletLight.copy(alpha = 0.1f),
    onPrimaryContainer = AccentVioletLight,
    secondary = AccentCyanLight,
    onSecondary = TextLight,
    secondaryContainer = AccentCyanLight.copy(alpha = 0.1f),
    onSecondaryContainer = AccentCyanLight,
    tertiary = AccentAmberLight,
    onTertiary = TextDark,
    background = LightBackground,
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextMutedDark,
    error = VibrantRedLight,
    onError = TextLight,
    outline = LightCard
)

private val DarkRecoveryColorScheme = darkColorScheme(
    primary = SageGreenDark,
    onPrimary = TextLight,
    primaryContainer = SageGreenDark.copy(alpha = 0.15f),
    onPrimaryContainer = SageGreenDark,
    secondary = LavenderDark,
    onSecondary = TextLight,
    secondaryContainer = LavenderDark.copy(alpha = 0.15f),
    onSecondaryContainer = LavenderDark,
    tertiary = EmeraldGreen,
    onTertiary = TextLight,
    background = RecoveryBgDark,
    onBackground = TextLight,
    surface = RecoverySurfaceDark,
    onSurface = TextLight,
    surfaceVariant = RecoveryCardDark,
    onSurfaceVariant = TextMutedLight,
    error = VibrantRed,
    onError = TextLight,
    outline = RecoveryCardDark
)

private val LightRecoveryColorScheme = lightColorScheme(
    primary = LavenderLight,
    onPrimary = TextLight,
    primaryContainer = LavenderLight.copy(alpha = 0.1f),
    onPrimaryContainer = LavenderLight,
    secondary = SageGreenLight,
    onSecondary = TextLight,
    secondaryContainer = SageGreenLight.copy(alpha = 0.1f),
    onSecondaryContainer = SageGreenLight,
    tertiary = EmeraldGreenLight,
    onTertiary = TextLight,
    background = RecoveryBgLight,
    onBackground = TextDark,
    surface = RecoverySurfaceLight,
    onSurface = TextDark,
    surfaceVariant = RecoveryCardLight,
    onSurfaceVariant = TextMutedDark,
    error = VibrantRedLight,
    onError = TextLight,
    outline = RecoveryCardLight
)

private val DarkBurnoutColorScheme = darkColorScheme(
    primary = BurnoutPrimary,
    onPrimary = TextLight,
    primaryContainer = BurnoutPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = BurnoutPrimary,
    secondary = BurnoutSecondary,
    onSecondary = TextLight,
    secondaryContainer = BurnoutSecondary.copy(alpha = 0.15f),
    onSecondaryContainer = BurnoutSecondary,
    tertiary = AccentViolet,
    onTertiary = TextLight,
    background = BurnoutBgDark,
    onBackground = TextLight,
    surface = BurnoutSurfaceDark,
    onSurface = TextLight,
    surfaceVariant = BurnoutCardDark,
    onSurfaceVariant = TextMutedLight,
    error = VibrantRed,
    onError = TextLight,
    outline = BurnoutCardDark
)

private val LightBurnoutColorScheme = lightColorScheme(
    primary = BurnoutPrimary,
    onPrimary = TextLight,
    primaryContainer = BurnoutPrimary.copy(alpha = 0.1f),
    onPrimaryContainer = BurnoutPrimary,
    secondary = BurnoutSecondary,
    onSecondary = TextLight,
    secondaryContainer = BurnoutSecondary.copy(alpha = 0.1f),
    onSecondaryContainer = BurnoutSecondary,
    tertiary = AccentVioletLight,
    onTertiary = TextLight,
    background = BurnoutBgLight,
    onBackground = TextDark,
    surface = BurnoutSurfaceLight,
    onSurface = TextDark,
    surfaceVariant = BurnoutCardLight,
    onSurfaceVariant = TextMutedDark,
    error = VibrantRedLight,
    onError = TextLight,
    outline = BurnoutCardLight
)

private val DarkPeakColorScheme = darkColorScheme(
    primary = PeakPrimary,
    onPrimary = TextLight,
    primaryContainer = PeakPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = PeakPrimary,
    secondary = PeakSecondary,
    onSecondary = TextLight,
    secondaryContainer = PeakSecondary.copy(alpha = 0.15f),
    onSecondaryContainer = PeakSecondary,
    tertiary = EmeraldGreen,
    onTertiary = TextLight,
    background = PeakBgDark,
    onBackground = TextLight,
    surface = PeakSurfaceDark,
    onSurface = TextLight,
    surfaceVariant = PeakCardDark,
    onSurfaceVariant = TextMutedLight,
    error = VibrantRed,
    onError = TextLight,
    outline = PeakCardDark
)

private val LightPeakColorScheme = lightColorScheme(
    primary = PeakPrimary,
    onPrimary = TextDark,
    primaryContainer = PeakPrimary.copy(alpha = 0.1f),
    onPrimaryContainer = PeakPrimary,
    secondary = PeakSecondary,
    onSecondary = TextDark,
    secondaryContainer = PeakSecondary.copy(alpha = 0.1f),
    onSecondaryContainer = PeakSecondary,
    tertiary = EmeraldGreenLight,
    onTertiary = TextLight,
    background = PeakBgLight,
    onBackground = TextDark,
    surface = PeakSurfaceLight,
    onSurface = TextDark,
    surfaceVariant = PeakCardLight,
    onSurfaceVariant = TextMutedDark,
    error = VibrantRedLight,
    onError = TextLight,
    outline = PeakCardLight
)

// Premium rounded corner system for a clean card workspace aesthetic
private val NeoMinimalShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StudyFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isRecoveryMode: Boolean = false,
    studyState: String = "FOCUS",
    content: @Composable () -> Unit
) {
    val activeState = studyState
    val colorScheme = when (activeState) {
        "RECOVERY" -> if (darkTheme) DarkRecoveryColorScheme else LightRecoveryColorScheme
        "BURNOUT" -> if (darkTheme) DarkBurnoutColorScheme else LightBurnoutColorScheme
        "PEAK" -> if (darkTheme) DarkPeakColorScheme else LightPeakColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = NeoMinimalShapes,
        typography = Typography,
        content = content
    )
}
