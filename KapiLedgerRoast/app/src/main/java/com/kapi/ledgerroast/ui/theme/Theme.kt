package com.kapi.ledgerroast.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF2D55),
    secondary = Color(0xFF6B7280),
    tertiary = Color(0xFF10B981)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF4D74),
    secondary = Color(0xFF9CA3AF),
    tertiary = Color(0xFF34D399)
)

@Composable
fun KapiLedgerRoastTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
