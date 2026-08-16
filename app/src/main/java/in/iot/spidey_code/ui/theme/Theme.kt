package `in`.iot.spidey_code.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SpideyRed,
    onPrimary = Color.White,
    secondary = SpideyBlue,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = OnDarkText,
    surface = DarkSurface,
    onSurface = OnDarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkTextSecondary
)

@Composable
fun SpideyCodeTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}