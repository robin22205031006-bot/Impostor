package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audio.SoundCuePlayer
import com.example.data.PlayerRole
import com.example.ui.components.AgentDirectorBadge
import com.example.ui.components.AgentPredictionCard
import com.example.ui.components.GameRoleIdentity
import com.example.ui.components.OtherImpostorAllyNote
import com.example.ui.components.RoleWordCard
import com.example.ui.components.SaviorAbilityCard
import com.example.ui.components.TurnTimerBar
import com.example.ui.components.UltimateKillerAbilityCard
import com.example.ui.components.style
import com.example.ui.theme.LocalImpostorPalette

@Composable
fun RoleRevealScreen(
    playerName: String,
    playerIndex: Int,
    totalPlayers: Int,
    turnDurationSeconds: Int,
    role: PlayerRole,
    category: String,
    genre: String,
    secretWord: String,
    hint: String,
    isRevealed: Boolean,
    showCategoryAndGenre: Boolean = true,
    showHint: Boolean = true,
    timerEnabled: Boolean = true,
    impostorAbilityEnabled: Boolean = false,
    isUltimateKiller: Boolean = false,
    saviorAgentEnabled: Boolean = false,
    isSavior: Boolean = false,
    agentDirectorEnabled: Boolean = false,
    isDirector: Boolean = false,
    agentPredictionEnabled: Boolean = false,
    otherPlayers: List<Pair<String, String>> = emptyList(),
    selectedKillTargetId: String? = null,
    selectedProtectedId: String? = null,
    selectedPredictionId: String? = null,
    onSelectKillTarget: (String?) -> Unit = {},
    onSelectProtectedTarget: (String?) -> Unit = {},
    onSelectPrediction: (String?) -> Unit = {},
    onReveal: () -> Unit,
    onHideAndNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalImpostorPalette.current
    val isLastPlayer = playerIndex >= totalPlayers - 1
    val roleIdentity = GameRoleIdentity.resolve(
        role = role,
        isUltimateKiller = impostorAbilityEnabled && isUltimateKiller,
        isSavior = saviorAgentEnabled && isSavior,
        isDirector = agentDirectorEnabled && isDirector
    )
    val roleStyle = roleIdentity.style(palette)
    val buttonAccent by animateColorAsState(
        targetValue = if (isRevealed) roleStyle.accent else palette.cyanAccent,
        animationSpec = tween(360),
        label = "reveal_cta_accent"
    )

    LaunchedEffect(isRevealed, role) {
        if (isRevealed) {
            SoundCuePlayer.playRoleRevealCue(isImpostor = role == PlayerRole.IMPOSTOR)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("role_reveal_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (timerEnabled) {
                TurnTimerBar(
                    activePlayerName = "Pass to $playerName",
                    turnIndex = playerIndex,
                    totalTurns = totalPlayers,
                    remainingSeconds = turnDurationSeconds,
                    totalSeconds = turnDurationSeconds,
                    isPaused = true,
                    showControls = false
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pass to $playerName",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${playerIndex + 1} / $totalPlayers",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.cyanAccent
                        )
                    }
                }
            }

            // Progress indicator pills for pass-and-play order
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPlayers.coerceAtMost(12)) { idx ->
                    val isCurrent = idx == playerIndex
                    val isDone = idx < playerIndex
                    val dotWidth by animateDpAsState(
                        targetValue = if (isCurrent) 24.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = dotWidth, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> if (isRevealed) roleStyle.glow else palette.cyanAccent
                                    isDone -> palette.normalAccent.copy(alpha = 0.7f)
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                    )
                }
            }

            // Tap-to-reveal overlay card strictly hides the assigned word & role until tapped
            AnimatedContent(
                targetState = isRevealed,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(260)) +
                        scaleIn(
                            initialScale = 0.90f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ))
                        .togetherWith(
                            fadeOut(animationSpec = tween(160)) +
                                scaleOut(targetScale = 0.94f, animationSpec = tween(160))
                        )
                        .using(SizeTransform(clip = false))
                },
                label = "role_reveal_overlay_transition"
            ) { revealed ->
                if (!revealed) {
                    TapToRevealOverlayCard(
                        playerName = playerName,
                        onReveal = onReveal
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        RoleWordCard(
                            role = role,
                            category = category,
                            genre = genre,
                            secretWord = secretWord,
                            hint = hint,
                            showCategoryAndGenre = showCategoryAndGenre,
                            showHint = showHint,
                            roleIdentity = roleIdentity
                        )
                        if (impostorAbilityEnabled && role == PlayerRole.IMPOSTOR) {
                            if (isUltimateKiller) {
                                UltimateKillerAbilityCard(
                                    candidates = otherPlayers,
                                    selectedTargetId = selectedKillTargetId,
                                    onSelectTarget = onSelectKillTarget
                                )
                            } else {
                                OtherImpostorAllyNote()
                            }
                        }
                        if (agentDirectorEnabled && role == PlayerRole.NORMAL && isDirector) {
                            AgentDirectorBadge()
                        }
                        if (saviorAgentEnabled && role == PlayerRole.NORMAL && isSavior) {
                            SaviorAbilityCard(
                                candidates = otherPlayers,
                                selectedProtectedId = selectedProtectedId,
                                onSelectProtected = onSelectProtectedTarget
                            )
                        }
                        if (agentPredictionEnabled && role == PlayerRole.NORMAL) {
                            AgentPredictionCard(
                                candidates = otherPlayers,
                                selectedSuspectId = selectedPredictionId,
                                onSelectSuspect = onSelectPrediction
                            )
                        }
                    }
                }
            }
        }

        if (isRevealed) {
            Button(
                onClick = onHideAndNext,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonAccent,
                    contentColor = if (role == PlayerRole.IMPOSTOR) Color.White else MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(56.dp)
                    .drawBehind {
                        val spread = 10.dp.toPx()
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(roleStyle.glow.copy(alpha = 0.28f), Color.Transparent),
                                radius = maxOf(size.width, size.height) * 0.72f
                            ),
                            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                            size = size.copy(width = size.width + spread, height = size.height + spread),
                            topLeft = Offset(-spread / 2f, -spread / 2f)
                        )
                    }
                    .testTag("local_hide_and_pass_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = when {
                        !isLastPlayer -> "Hide Card & Pass to Next Player"
                        timerEnabled -> "Hide Card & Start Timed Turns"
                        else -> "Hide Card & Start Discussion"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = palette.cyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Privacy Shield Active — Word & role stay hidden until $playerName taps the card.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TapToRevealOverlayCard(
    playerName: String,
    onReveal: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tap_card_press"
    )

    val pulseTransition = rememberInfiniteTransition(label = "overlay_pulse")
    val borderAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "overlay_border_alpha"
    )
    val rippleRadius by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_ripple"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .drawBehind {
                val spread = 14.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.cyanAccent.copy(alpha = 0.22f * borderAlpha), Color.Transparent),
                        radius = maxOf(size.width, size.height) * 0.72f
                    ),
                    cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
                    size = size.copy(width = size.width + spread, height = size.height + spread),
                    topLeft = Offset(-spread / 2f, -spread / 2f)
                )
            }
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
            .border(
                width = 2.dp,
                color = palette.cyanAccent.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(28.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onReveal
            )
            .padding(28.dp)
            .testTag("local_tap_to_reveal_card"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxR = size.minDimension * 0.48f
            drawCircle(
                color = palette.cyanAccent.copy(alpha = (1f - rippleRadius) * 0.25f),
                radius = maxR * rippleRadius,
                style = Stroke(width = 2.dp.toPx())
            )
            val secondaryPhase = (rippleRadius + 0.5f) % 1f
            drawCircle(
                color = palette.cyanAccent.copy(alpha = (1f - secondaryPhase) * 0.20f),
                radius = maxR * secondaryPhase,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = palette.cyanSurface,
                shape = CircleShape,
                border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = borderAlpha))
            ) {
                Icon(
                    imageVector = Icons.Filled.TouchApp,
                    contentDescription = "Tap to reveal secret role",
                    tint = palette.cyanAccent,
                    modifier = Modifier
                        .padding(20.dp)
                        .size(38.dp)
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "PASS PHONE TO",
                style = MaterialTheme.typography.labelMedium,
                color = palette.cyanAccent
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = playerName,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Make sure no other players are looking.\nTap anywhere on this card to reveal your role.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
