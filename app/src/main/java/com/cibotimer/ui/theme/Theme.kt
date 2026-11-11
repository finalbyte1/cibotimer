package com.cibotimer.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CiboTimerAzure,
    onPrimary = Color(0xFF0F1115),
    primaryContainer = CiboTimerAzureContainer,
    onPrimaryContainer = CiboTimerAzure,
    secondary = CiboTimerOrange,
    onSecondary = Color(0xFF0F1115),
    secondaryContainer = CiboTimerOrangeContainer,
    onSecondaryContainer = CiboTimerOrange,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark
)

@Composable
fun CiboTimerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundDark.toArgb()
            window.navigationBarColor = BackgroundDark.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Deprecated("Usa CiboTimerTheme", ReplaceWith("CiboTimerTheme(content)"))
@Composable
fun BodyFastTheme(
    content: @Composable () -> Unit
) = CiboTimerTheme(content)
