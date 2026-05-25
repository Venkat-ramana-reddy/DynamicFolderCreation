package com.factoryattendance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary          = Blue40,
    onPrimary        = Color.White,
    primaryContainer = Blue10,
    secondary        = Green40,
    onSecondary      = Color.White,
    secondaryContainer = Green10,
    error            = Red40,
    errorContainer   = Red10,
    background       = Bg,
    surface          = Surface,
    surfaceVariant   = Surface2,
    onBackground     = Text1,
    onSurface        = Text1,
    onSurfaceVariant = Text2,
    outline          = Border
)

private val DarkColors = darkColorScheme(
    primary          = DkBlue,
    onPrimary        = DkBg,
    primaryContainer = Color(0xFF1A2A3A),
    secondary        = DkGreen,
    onSecondary      = DkBg,
    secondaryContainer = Color(0xFF162410),
    error            = DkRed,
    errorContainer   = Color(0xFF2E1414),
    background       = DkBg,
    surface          = DkSurface,
    surfaceVariant   = DkSurface2,
    onBackground     = DkText1,
    onSurface        = DkText1,
    onSurfaceVariant = DkText2,
    outline          = DkBorder
)

@Composable
fun AttendanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
