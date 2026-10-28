package com.phonehardware.diagnostic.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val dark = darkColorScheme(
    primary = Color(0xFF5DA0FF), onPrimary = Color(0xFF07101D),
    secondary = Color(0xFF39DFB9),background = Color(0xFF0A1020),
    onBackground = Color(0xFFEAF1FC),surface = Color(0xFF101A2F),
    onSurface = Color(0xFFEAF1FC), surfaceVariant = Color(0xFF1B2943),
    outline = Color(0xFF64728B),error = Color(0xFFFF6B7B)
)
private val light = lightColorScheme(
    primary = Color(0xFF145BBB), secondary = Color(0xFF087C65),
    background = Color(0xFFF7F9FD), surface = Color.White,
    surfaceVariant = Color(0xFFE9EEF8)
)
@Composable fun DiagnosticTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if(darkTheme) dark else light, content = content)
}
