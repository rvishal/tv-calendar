package com.rvishal.tvcalendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// iPhone-calendar-inspired palette: red accent, neutral greys.
val IosRed = Color(0xFFFA233B)
val IosBgDark = Color(0xFF1C1C1E)
val IosCardDark = Color(0xFF2C2C2E)
val IosBgLight = Color(0xFFFFFFFF)
val IosCardLight = Color(0xFFF2F2F7)

@OptIn(ExperimentalTvMaterial3Api::class)
private val DarkScheme = darkColorScheme(
    primary = IosRed,
    background = IosBgDark,
    surface = IosCardDark,
    onPrimary = Color.White,
    onBackground = Color(0xFFF2F2F7),
    onSurface = Color(0xFFF2F2F7),
)

@OptIn(ExperimentalTvMaterial3Api::class)
private val LightScheme = lightColorScheme(
    primary = IosRed,
    background = IosBgLight,
    surface = IosCardLight,
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content,
    )
}
