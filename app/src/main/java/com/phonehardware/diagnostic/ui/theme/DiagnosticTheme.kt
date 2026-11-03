package com.phonehardware.diagnostic.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phonehardware.diagnostic.R

/**
 * Dana Fanum font family used across the complete Compose UI.
 * Regular is used for normal text and Bold for emphasis/headings.
 */
private val DanaFanum = FontFamily(
    Font(R.font.dana_fanum_regular, FontWeight.Normal),
    Font(R.font.dana_fanum_bold, FontWeight.Bold)
)

private val baseTypography = Typography()
private val diagnosticTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = DanaFanum),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = DanaFanum),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = DanaFanum),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = DanaFanum),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = DanaFanum),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = DanaFanum),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = DanaFanum),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = DanaFanum),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = DanaFanum),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = DanaFanum),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = DanaFanum),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = DanaFanum),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = DanaFanum),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = DanaFanum),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = DanaFanum)
)

private val diagnosticDark = darkColorScheme(
    primary = Color(0xFF00CFFF),
    onPrimary = Color(0xFF002033),
    primaryContainer = Color(0xFF073B5C),
    onPrimaryContainer = Color(0xFFBDEEFF),
    secondary = Color(0xFF28E0C0),
    onSecondary = Color(0xFF002019),
    secondaryContainer = Color(0xFF073E3A),
    onSecondaryContainer = Color(0xFFB9FFF0),
    tertiary = Color(0xFFFFB34D),
    onTertiary = Color(0xFF3B2100),
    background = Color(0xFF050D1B),
    onBackground = Color(0xFFF2F7FF),
    surface = Color(0xFF0A1728),
    onSurface = Color(0xFFF2F7FF),
    surfaceVariant = Color(0xFF10243A),
    onSurfaceVariant = Color(0xFFB7C7DA),
    outline = Color(0xFF55718B),
    outlineVariant = Color(0xFF29445E),
    error = Color(0xFFFF647C),
    onError = Color(0xFF3D0010),
    errorContainer = Color(0xFF4A1725),
    onErrorContainer = Color(0xFFFFD9DE)
)

private val diagnosticLight = lightColorScheme(
    primary = Color(0xFF006D91),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F1FC),
    onPrimaryContainer = Color(0xFF003448),
    secondary = Color(0xFF007A68),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0F3EA),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = Color(0xFF8A5100),
    onTertiary = Color.White,
    background = Color(0xFFF3F7FB),
    onBackground = Color(0xFF14283B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14283B),
    surfaceVariant = Color(0xFFE7F0F7),
    onSurfaceVariant = Color(0xFF4C6276),
    outline = Color(0xFF91A9BC),
    outlineVariant = Color(0xFFD0DDE7),
    error = Color(0xFFB32645),
    onError = Color.White,
    errorContainer = Color(0xFFFFDADF),
    onErrorContainer = Color(0xFF41000D)
)

private val diagnosticShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DiagnosticTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) diagnosticDark else diagnosticLight,
        typography = diagnosticTypography,
        shapes = diagnosticShapes,
        content = content
    )
}
