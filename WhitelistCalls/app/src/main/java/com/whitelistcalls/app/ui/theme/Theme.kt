package com.whitelistcalls.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Primary = Color(0xFF1A73E8)
val PrimaryVariant = Color(0xFF1557B0)
val Secondary = Color(0xFF03DAC6)
val Background = Color(0xFFFAFAFA)
val Surface = Color(0xFFFFFFFF)
val Error = Color(0xFFB00020)
val Success = Color(0xFF34A853)
val Warning = Color(0xFFFBBC04)
val OnPrimary = Color.White
val OnSecondary = Color.Black
val OnBackground = Color(0xFF1C1B1F)
val OnSurface = Color(0xFF1C1B1F)
val OnError = Color.White

val DarkPrimary = Color(0xFF8AB4F8)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnBackground = Color(0xFFE3E3E3)
val DarkOnSurface = Color(0xFFE3E3E3)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    error = Error,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onError = OnError,
    tertiary = Success
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = Secondary,
    background = DarkBackground,
    surface = DarkSurface,
    error = Error,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onError = OnError,
    tertiary = Success
)

@Composable
fun WhitelistCallsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
