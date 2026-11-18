package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlayerRole
import com.example.ui.theme.LocalImpostorPalette

@Composable
fun RoleWordCard(
    role: PlayerRole,
    category: String,
    genre: String,
    secretWord: String,
    hint: String,
    showCategoryAndGenre: Boolean = true,
    showHint: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = LocalImpostorPalette.current
    val isImpostor = role == PlayerRole.IMPOSTOR
    val hasCategory = showCategoryAndGenre && (category.isNotBlank() || genre.isNotBlank())
    val hasHint = showHint && hint.isNotBlank()

    val accent = if (isImpostor) palette.impostorAccent else palette.normalAccent
    val glow = if (isImpostor) palette.impostorGlow else palette.normalGlow
    val surfaceBg = if (isImpostor) palette.impostorSurface else palette.normalSurface
    val borderColor = if (isImpostor) palette.impostorBorder else palette.normalBorder

    val pulseTransition = rememberInfiniteTransition(label = "role_glow")
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val glowSpreadDp by pulseTransition.animateFloat(
        initialValue = 10f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_spread"
    )
    val borderPulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.58f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_pulse_alpha"
    )
    val cardScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card_scale"
    )
    val sheenOffset by pulseTransition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen_offset"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (isImpostor) "role_card_impostor" else "role_card_normal"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Glowing Word Box (Red pulsing glow for Impostor, Green pulsing glow for Normal)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                }
                .drawBehind {
                    val spread = glowSpreadDp.dp.toPx()
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glow.copy(alpha = glowAlpha),
                                accent.copy(alpha = glowAlpha * 0.48f),
                                Color.Transparent
                            ),
                            radius = maxOf(size.width, size.height) * 0.72f
                        ),
                        cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                        size = size.copy(
                            width = size.width + spread,
                            height = size.height + spread
                        ),
                        topLeft = Offset(-spread / 2f, -spread / 2f)
                    )
                }
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            surfaceBg,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            glow.copy(alpha = glowAlpha * 0.22f),
                            Color.Transparent
                        )
                    )
                )
                .drawBehind {
                    val sweepX = size.width * sheenOffset
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                glow.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            start = Offset(sweepX - size.width * 0.3f, 0f),
                            end = Offset(sweepX + size.width * 0.3f, size.height)
                        )
                    )
                }
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glow.copy(alpha = borderPulseAlpha),
                            accent.copy(alpha = borderPulseAlpha),
                            borderColor.copy(alpha = borderPulseAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Upper side of the box: 'Category' label defining the subject of the word shown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (hasCategory) Arrangement.SpaceBetween else Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasCategory) {
                        Column {
                            Text(
                                text = stringResource(R.string.category_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = glow
                            )
                            Text(
                                text = category.ifEmpty { "General" },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        color = accent.copy(alpha = 0.14f + (glowAlpha * 0.12f)),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, glow.copy(alpha = borderPulseAlpha * 0.85f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PulsingStatusDot(color = glow)
                            Icon(
                                imageVector = if (isImpostor) Icons.Filled.VisibilityOff else Icons.Filled.Security,
                                contentDescription = if (isImpostor) "Impostor Role" else "Agent Role",
                                tint = glow,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (isImpostor) R.string.impostor_badge else R.string.crew_badge
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = glow
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = accent.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(24.dp))

                if (isImpostor) {
                    Text(
                        text = "YOU ARE THE IMPOSTOR",
                        style = MaterialTheme.typography.headlineMedium,
                        color = glow,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (hasHint) {
                            "No word revealed. Use the clue below to blend in or guess the term!"
                        } else {
                            "No word or hint revealed. Listen closely to blend in!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "SECRET WORD",
                        style = MaterialTheme.typography.labelSmall,
                        color = glow.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = secretWord,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("secret_word_text")
                    )
                }

                if (hasCategory) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = accent.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Down the word box: Name of the genre of the word
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${stringResource(R.string.genre_label).uppercase()}: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = genre.ifEmpty { category },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = glow,
                            modifier = Modifier.testTag("word_genre_text")
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Glowing Hint Box (Red pulsing glow for Impostor, Green pulsing glow for Normal)
        if (hasHint) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val spread = (glowSpreadDp * 0.7f).dp.toPx()
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glow.copy(alpha = glowAlpha * 0.9f),
                                    accent.copy(alpha = glowAlpha * 0.35f),
                                    Color.Transparent
                                ),
                                radius = maxOf(size.width, size.height) * 0.72f
                            ),
                            cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                            size = size.copy(
                                width = size.width + spread,
                                height = size.height + spread
                            ),
                            topLeft = Offset(-spread / 2f, -spread / 2f)
                        )
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .background(surfaceBg.copy(alpha = 0.92f))
                    .border(
                        width = 1.5.dp,
                        color = glow.copy(alpha = borderPulseAlpha * 0.9f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .testTag(if (isImpostor) "hint_box_red" else "hint_box_green")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = accent.copy(alpha = 0.16f + (glowAlpha * 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = "Word hint",
                            tint = glow,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isImpostor) "IMPOSTOR HINT" else "CREW HINT",
                            style = MaterialTheme.typography.labelSmall,
                            color = glow
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
