package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalImpostorPalette
import java.util.Locale

@Composable
fun TurnTimerBar(
    activePlayerName: String,
    turnIndex: Int,
    totalTurns: Int,
    remainingSeconds: Int,
    totalSeconds: Int,
    isPaused: Boolean,
    showControls: Boolean,
    onTogglePause: () -> Unit = {},
    onNextTurn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalImpostorPalette.current
    val safeTotal = totalSeconds.coerceAtLeast(1)
    val clampedRemaining = remainingSeconds.coerceIn(0, safeTotal)
    val targetProgress = clampedRemaining.toFloat() / safeTotal.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "timer_progress"
    )

    val timerColor by animateColorAsState(
        targetValue = when {
            clampedRemaining <= 5 -> palette.impostorAccent
            clampedRemaining <= 12 -> palette.timerAccent
            else -> palette.cyanAccent
        },
        label = "timer_color"
    )

    val isUrgent = !isPaused && clampedRemaining in 1..5
    val urgencyPulse = rememberInfiniteTransition(label = "timer_urgency")
    val urgentScale by urgencyPulse.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isUrgent) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "urgent_scale"
    )

    val mins = clampedRemaining / 60
    val secs = clampedRemaining % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", mins, secs)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("turn_timer_bar"),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, timerColor.copy(alpha = if (isUrgent) 0.85f else 0.45f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Turn timer",
                        tint = timerColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "TURN ${turnIndex + 1} OF ${totalTurns.coerceAtLeast(1)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = activePlayerName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isPaused) "$formattedTime II" else formattedTime,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        color = timerColor,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = urgentScale
                                scaleY = urgentScale
                            }
                            .padding(end = 4.dp)
                            .testTag("turn_timer_clock")
                    )

                    if (showControls) {
                        IconButton(
                            onClick = onTogglePause,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("timer_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (isPaused) "Resume timer" else "Pause timer",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onNextTurn,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("timer_next_turn_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next player's turn",
                                tint = timerColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = timerColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
