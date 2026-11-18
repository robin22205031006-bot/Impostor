package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalImpostorPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ImpostorAnimatedBackdrop(
    modifier: Modifier = Modifier,
    primaryOrbColor: Color = LocalImpostorPalette.current.cyanAccent,
    secondaryOrbColor: Color = LocalImpostorPalette.current.normalAccent,
    tertiaryOrbColor: Color = LocalImpostorPalette.current.impostorAccent,
    content: @Composable BoxScope.() -> Unit
) {
    val bgTop = MaterialTheme.colorScheme.background
    val bgBottom = MaterialTheme.colorScheme.surface
    val transition = rememberInfiniteTransition(label = "backdrop_ambient")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_phase"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_breathe"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgTop, bgBottom, bgTop)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-right drifting cyan aurora
            val c1 = Offset(
                x = w * (0.78f + 0.12f * cos(phase)),
                y = h * (0.14f + 0.06f * sin(phase))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryOrbColor.copy(alpha = 0.16f * breathe),
                        primaryOrbColor.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = c1,
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = c1
            )

            // Bottom-left drifting green/emerald orb
            val c2 = Offset(
                x = w * (0.18f + 0.10f * sin(phase * 0.8f)),
                y = h * (0.78f + 0.08f * cos(phase * 0.8f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryOrbColor.copy(alpha = 0.13f * breathe),
                        secondaryOrbColor.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = c2,
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = c2
            )

            // Subtle mid-right crimson tension glow
            val c3 = Offset(
                x = w * (0.85f - 0.10f * sin(phase)),
                y = h * (0.52f + 0.07f * cos(phase))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiaryOrbColor.copy(alpha = 0.09f * breathe),
                        Color.Transparent
                    ),
                    center = c3,
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = c3
            )

            // Tactical subtle dot matrix texture
            val spacing = 30.dp.toPx()
            val dotRadius = 1.dp.toPx()
            var y = spacing * 0.5f
            while (y < h) {
                var x = spacing * 0.5f
                while (x < w) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.035f),
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    x += spacing
                }
                y += spacing
            }
        }

        content()
    }
}

@Composable
fun GlassSectionCard(
    title: String,
    icon: ImageVector? = null,
    accentColor: Color = LocalImpostorPalette.current.cyanAccent,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                    )
                ),
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (icon != null) {
                        Surface(
                            color = accentColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailing()
                }
            }
            content()
        }
    }
}

@Composable
fun PulsingStatusDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "status_dot")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}
