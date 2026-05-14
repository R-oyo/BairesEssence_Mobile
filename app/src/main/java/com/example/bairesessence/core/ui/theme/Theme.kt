package com.example.bairesessence.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BEColorScheme = lightColorScheme(
    primary            = BEPrimary,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFB7F5DC),
    onPrimaryContainer = BEPrimaryDark,
    secondary          = BEDark,
    onSecondary        = Color.White,
    tertiary           = BEPrimaryLight,
    onTertiary         = BEDark,
    background         = BEBackground,
    onBackground       = BETextPrimary,
    surface            = BESurface,
    onSurface          = BETextPrimary,
    surfaceVariant     = BESurfaceVar,
    onSurfaceVariant   = BETextSecond,
    outline            = BEBorder,
    error              = BEError,
    onError            = Color.White,
)

@Composable
fun BairesEssenceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BEColorScheme, typography = Typography, content = content)
}
