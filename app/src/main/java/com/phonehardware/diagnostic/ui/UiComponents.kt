package com.phonehardware.diagnostic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal val NeonCyan = Color(0xFF00D7F5)
internal val NeonBlue = Color(0xFF168BFF)
internal val NeonGreen = Color(0xFF25DCA8)
internal val DeepNavy = Color(0xFF050D1B)
internal val PanelNavy = Color(0xFF0B1A2D)

/** Preserve the neon palette in dark mode and use higher-contrast accents in light mode. */
@Composable
internal fun themedAccent(color: Color): Color {
    val colors = MaterialTheme.colorScheme
    val isLight = colors.background.luminance() > 0.5f
    if (!isLight) return color
    return when (color) {
        NeonCyan, NeonBlue -> colors.primary
        NeonGreen -> Color(0xFF087A5B)
        Color(0xFFFFB34D), Color(0xFFFFA34A), Color(0xFFFFC45C) -> Color(0xFF8A5100)
        Color(0xFFFF647C) -> Color(0xFFB32645)
        Color(0xFFB69CFF), Color(0xFFB79BFF) -> Color(0xFF6545A3)
        Color(0xFF9CB5CA) -> colors.onSurfaceVariant
        else -> color
    }
}

/** Theme-aware screen background; never forces dark pixels in light mode. */
@Composable
internal fun screenBackgroundBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        listOf(colors.background, colors.surface, colors.background)
    )
}

@Composable
fun DiagnosticPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundBrush()),
        content = content
    )
}

@Composable
fun NeonPanel(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val resolvedAccent = themedAccent(accent ?: colors.primary)
    Surface(
        modifier = modifier.border(1.dp, resolvedAccent.copy(alpha = 0.30f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(colors.surfaceVariant, colors.surface)
                    )
                )
                .padding(18.dp),
            content = content
        )
    }
}

@Composable
fun PageHeading(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            eyebrow.uppercase(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AccentActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceVariant,
            disabledContentColor = colors.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp, pressedElevation = 1.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OutlineActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FeatureIcon(icon: ImageVector, accent: Color? = null, modifier: Modifier = Modifier) {
    val resolvedAccent = themedAccent(accent ?: MaterialTheme.colorScheme.primary)
    Box(
        modifier = modifier
            .size(48.dp)
            .background(resolvedAccent.copy(alpha = 0.11f), RoundedCornerShape(15.dp))
            .border(1.dp, resolvedAccent.copy(alpha = 0.35f), RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = resolvedAccent, modifier = Modifier.size(25.dp))
    }
}

@Composable
fun StatusPill(text: String, color: Color = NeonGreen, modifier: Modifier = Modifier) {
    val resolvedColor = themedAccent(color)
    Row(
        modifier = modifier
            .background(resolvedColor.copy(alpha = 0.12f), CircleShape)
            .border(1.dp, resolvedColor.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = resolvedColor, modifier = Modifier.size(14.dp))
        Text(text, color = resolvedColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun InfoCallout(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceVariant, RoundedCornerShape(16.dp))
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.size(19.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}
