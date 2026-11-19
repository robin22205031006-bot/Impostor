package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GamePhase
import com.example.data.GameState
import com.example.data.LocalPlayer
import com.example.data.PlayerRole
import com.example.data.TurnStatus
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.CircularCountdownTimer
import com.example.ui.components.GameRoleIdentity
import com.example.ui.components.GlassSectionCard
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.RoleEmblemBadge
import com.example.ui.components.TurnTimerEventBus
import com.example.ui.components.style
import com.example.ui.theme.LocalImpostorPalette
import com.example.viewmodel.GameViewModel

@Composable
fun GameLobbyScreen(
    viewModel: GameViewModel = viewModel(key = "tactical_game_lobby"),
    modifier: Modifier = Modifier
) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.phase != GamePhase.LOBBY) {
        viewModel.returnToLobby()
    }

    GameLobbyContent(
        state = state,
        onSetImpostorCount = viewModel::setImpostorCount,
        onShuffleRoles = viewModel::assignRoles,
        onToggleRoleReveal = viewModel::toggleRoleReveal,
        onRevealAll = viewModel::revealAllRoles,
        onHideAll = viewModel::hideAllRoles,
        onAddPlayer = viewModel::addPlayer,
        onRemovePlayer = viewModel::removePlayer,
        onStartDiscussion = viewModel::startDiscussion,
        onToggleTimerPause = viewModel::toggleTimerPause,
        onNextTurn = viewModel::nextTurn,
        onStartVoting = viewModel::startVoting,
        onCastVote = viewModel::castVote,
        onReturnToLobby = viewModel::returnToLobby,
        modifier = modifier
    )
}

@Composable
fun GameLobbyContent(
    state: GameState,
    onSetImpostorCount: (Int) -> Unit,
    onShuffleRoles: () -> Unit,
    onToggleRoleReveal: (String) -> Unit,
    onRevealAll: () -> Unit,
    onHideAll: () -> Unit,
    onAddPlayer: (String) -> Unit,
    onRemovePlayer: (String) -> Unit,
    onStartDiscussion: () -> Unit,
    onToggleTimerPause: () -> Unit,
    onNextTurn: () -> Unit,
    onStartVoting: () -> Unit,
    onCastVote: (String, String) -> Unit,
    onReturnToLobby: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalImpostorPalette.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_lobby_screen")
    ) {
        // Phase Stepper Header: Lobby -> Discussion -> Voting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GamePhase.entries.forEach { phase ->
                val isActive = state.phase == phase
                val phaseAccent = when (phase) {
                    GamePhase.LOBBY -> palette.normalAccent
                    GamePhase.DISCUSSION -> palette.cyanAccent
                    GamePhase.VOTING -> palette.impostorAccent
                }
                val bgColor by animateColorAsState(
                    targetValue = if (isActive) phaseAccent.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceContainer,
                    label = "phase_pill_bg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isActive) phaseAccent else MaterialTheme.colorScheme.outlineVariant,
                    label = "phase_pill_border"
                )
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("phase_pill_${phase.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isActive) {
                            PulsingStatusDot(color = phaseAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = phase.title.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) phaseAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = state.phase,
            transitionSpec = {
                (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 18 }) togetherWith
                    fadeOut(tween(160))
            },
            label = "game_state_phase_transition",
            modifier = Modifier.weight(1f)
        ) { phase ->
            when (phase) {
                GamePhase.LOBBY -> LobbyPhaseView(
                    state = state,
                    onSetImpostorCount = onSetImpostorCount,
                    onShuffleRoles = onShuffleRoles,
                    onToggleRoleReveal = onToggleRoleReveal,
                    onRevealAll = onRevealAll,
                    onHideAll = onHideAll,
                    onAddPlayer = onAddPlayer,
                    onRemovePlayer = onRemovePlayer,
                    onStartDiscussion = onStartDiscussion
                )

                GamePhase.DISCUSSION -> DiscussionPhaseView(
                    state = state,
                    onToggleTimerPause = onToggleTimerPause,
                    onNextTurn = onNextTurn,
                    onStartVoting = onStartVoting,
                    onReturnToLobby = onReturnToLobby
                )

                GamePhase.VOTING -> VotingPhaseView(
                    state = state,
                    onCastVote = onCastVote,
                    onReturnToLobby = onReturnToLobby
                )
            }
        }
    }
}

@Composable
private fun LobbyPhaseView(
    state: GameState,
    onSetImpostorCount: (Int) -> Unit,
    onShuffleRoles: () -> Unit,
    onToggleRoleReveal: (String) -> Unit,
    onRevealAll: () -> Unit,
    onHideAll: () -> Unit,
    onAddPlayer: (String) -> Unit,
    onRemovePlayer: (String) -> Unit,
    onStartDiscussion: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val maxImpostors = (state.players.size - 1).coerceAtLeast(1)
    val allRevealed = state.players.isNotEmpty() && state.revealedRolePlayerIds.size >= state.players.size
    var newPlayerName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Impostor Count & Shuffle Header Card
            item {
                GlassSectionCard(
                    title = "IMPOSTORS (1 TO $maxImpostors)",
                    icon = Icons.Filled.VisibilityOff,
                    accentColor = palette.impostorAccent,
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            TextButton(
                                onClick = onShuffleRoles,
                                modifier = Modifier.testTag("lobby_shuffle_roles_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Re-deal roles",
                                    tint = palette.cyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Deal", color = palette.cyanAccent)
                            }
                            TextButton(
                                onClick = { if (allRevealed) onHideAll() else onRevealAll() },
                                modifier = Modifier.testTag("lobby_toggle_all_roles_button")
                            ) {
                                Icon(
                                    imageVector = if (allRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (allRevealed) "Hide all roles" else "Reveal all roles",
                                    tint = palette.cyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (allRevealed) "Hide All" else "Reveal All",
                                    color = palette.cyanAccent
                                )
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AnimatedContent(
                            targetState = state.impostorCount,
                            transitionSpec = {
                                (slideInVertically { -it / 2 } + fadeIn()) togetherWith
                                    (slideOutVertically { it / 2 } + fadeOut())
                            },
                            label = "lobby_impostor_count"
                        ) { count ->
                            Text(
                                text = "$count Impostor${if (count > 1) "s" else ""}",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.impostorGlow,
                                modifier = Modifier.testTag("lobby_impostor_count_text")
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onSetImpostorCount(state.impostorCount - 1) },
                                enabled = state.impostorCount > 1,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("lobby_decrease_impostor_button"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-", style = MaterialTheme.typography.titleLarge)
                            }
                            OutlinedButton(
                                onClick = { onSetImpostorCount(state.impostorCount + 1) },
                                enabled = state.impostorCount < maxImpostors,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("lobby_increase_impostor_button"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }

            // Player Cards with Distinct Glowing Compose Modifiers per Assigned Role
            items(state.players, key = { it.id }) { player ->
                val assignedRole = state.playerRoles[player.id] ?: PlayerRole.NORMAL
                val isRevealed = player.id in state.revealedRolePlayerIds
                GlowingLobbyRoleCard(
                    player = player,
                    assignedRole = assignedRole,
                    isRevealed = isRevealed,
                    canRemove = state.players.size > 2,
                    onToggleReveal = { onToggleRoleReveal(player.id) },
                    onRemove = { onRemovePlayer(player.id) }
                )
            }

            // Add Player Row
            item {
                GlassSectionCard(
                    title = "ADD PLAYER (${state.players.size})",
                    icon = Icons.Filled.Group,
                    accentColor = palette.cyanAccent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPlayerName,
                            onValueChange = { newPlayerName = it.take(24) },
                            label = { Text("Add Player") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onAddPlayer(newPlayerName)
                                newPlayerName = ""
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add player")
                        }
                    }
                }
            }
        }

        // Pinned Start Discussion Phase Transition CTA
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onStartDiscussion,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.cyanAccent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .height(54.dp)
                    .testTag("start_discussion_phase_button")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Start Discussion Phase", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun GlowingLobbyRoleCard(
    player: LocalPlayer,
    assignedRole: PlayerRole,
    isRevealed: Boolean,
    canRemove: Boolean,
    onToggleReveal: () -> Unit,
    onRemove: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val identity = if (assignedRole == PlayerRole.IMPOSTOR) {
        GameRoleIdentity.IMPOSTOR
    } else {
        GameRoleIdentity.AGENT
    }
    val style = identity.style(palette)
    val cardShape = RoundedCornerShape(18.dp)

    val activeAccent by animateColorAsState(
        targetValue = if (isRevealed) style.accent else MaterialTheme.colorScheme.outline,
        animationSpec = tween(320),
        label = "lobby_card_accent"
    )
    val activeGlow by animateColorAsState(
        targetValue = if (isRevealed) style.glow else palette.cyanAccent.copy(alpha = 0.25f),
        animationSpec = tween(320),
        label = "lobby_card_glow"
    )
    val surfaceColor by animateColorAsState(
        targetValue = if (isRevealed) style.deepSurface else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(320),
        label = "lobby_card_surface"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .distinctRoleGlow(
                isGlowing = isRevealed,
                glowColor = activeGlow,
                accentColor = activeAccent,
                shape = cardShape
            )
            .clip(cardShape)
            .background(surfaceColor)
            .clickable(onClick = onToggleReveal)
            .minimumInteractiveComponentSize()
            .testTag("lobby_role_card_${player.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isRevealed) {
                RoleEmblemBadge(
                    identity = identity,
                    size = 42.dp,
                    animated = true
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "Tap to reveal ${player.name}'s role",
                            tint = palette.cyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AnimatedVisibility(visible = isRevealed) {
                    Text(
                        text = if (assignedRole == PlayerRole.IMPOSTOR) {
                            "ROLE: IMPOSTOR"
                        } else {
                            "ROLE: AGENT (CREW)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = style.glow
                    )
                }
                if (!isRevealed) {
                    Text(
                        text = "Role hidden — tap to reveal glow",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (canRemove) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove ${player.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Custom Compose modifier applying a layered shadow, animated radial aura bloom,
 * scale pulse, and gradient border to visually highlight assigned player roles.
 */
fun Modifier.distinctRoleGlow(
    isGlowing: Boolean,
    glowColor: Color,
    accentColor: Color,
    shape: Shape = RoundedCornerShape(18.dp)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "distinct_role_glow")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.34f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse_alpha"
    )
    val pulseScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse_scale"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isGlowing) 16.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glow_shadow_elevation"
    )

    this
        .graphicsLayer {
            if (isGlowing) {
                scaleX = pulseScale
                scaleY = pulseScale
            }
        }
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = glowColor,
            spotColor = glowColor
        )
        .drawBehind {
            if (isGlowing) {
                val spread = 14.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = pulseAlpha * 0.60f),
                            accentColor.copy(alpha = pulseAlpha * 0.25f),
                            Color.Transparent
                        ),
                        radius = maxOf(size.width, size.height) * 0.74f
                    ),
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                    size = size.copy(width = size.width + spread, height = size.height + spread),
                    topLeft = Offset(-spread / 2f, -spread / 2f)
                )
            }
        }
        .border(
            width = if (isGlowing) 2.dp else 1.dp,
            brush = Brush.linearGradient(
                colors = if (isGlowing) {
                    listOf(
                        glowColor.copy(alpha = pulseAlpha.coerceAtLeast(0.7f)),
                        accentColor.copy(alpha = 0.9f),
                        glowColor.copy(alpha = pulseAlpha.coerceAtLeast(0.7f))
                    )
                } else {
                    listOf(accentColor.copy(alpha = 0.45f), accentColor.copy(alpha = 0.45f))
                }
            ),
            shape = shape
        )
}

@Composable
private fun DiscussionPhaseView(
    state: GameState,
    onToggleTimerPause: () -> Unit,
    onNextTurn: () -> Unit,
    onStartVoting: () -> Unit,
    onReturnToLobby: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val context = LocalContext.current
    val speakerName = state.activeTurnPlayer?.name ?: "Discussion"
    val isPaused = state.turnStatus == TurnStatus.PAUSED

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassSectionCard(
                title = "DISCUSSION • TURN ${state.currentTurnIndex + 1} OF ${state.turnOrderIds.size.coerceAtLeast(1)}",
                icon = Icons.Filled.PlayArrow,
                accentColor = palette.cyanAccent
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.runtime.key(state.currentTurnIndex) {
                        CircularCountdownTimer(
                            remainingSeconds = state.currentTimerCount,
                            totalSeconds = state.turnDurationSeconds,
                            isPaused = isPaused,
                            size = 156.dp,
                            strokeWidth = 11.dp,
                            onTimeExpired = {
                                TurnTimerEventBus.emitExpired(
                                    context = context,
                                    playerName = speakerName,
                                    turnIndex = state.currentTurnIndex,
                                    totalTurns = state.turnOrderIds.size
                                )
                            }
                        )
                    }

                    Text(
                        text = speakerName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = when (state.turnStatus) {
                            TurnStatus.EXPIRED -> palette.impostorSurface
                            TurnStatus.PAUSED -> palette.timerSurface
                            else -> palette.cyanSurface
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = state.turnStatus.label.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = when (state.turnStatus) {
                                TurnStatus.EXPIRED -> palette.impostorGlow
                                TurnStatus.PAUSED -> palette.timerAccent
                                else -> palette.cyanAccent
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("discussion_turn_status_badge")
                        )
                    }

                    AnimatedVisibility(visible = state.turnStatus == TurnStatus.EXPIRED) {
                        Surface(
                            color = palette.impostorSurface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, palette.impostorGlow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = "Time expired alert",
                                    tint = palette.impostorGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "TIME EXPIRED FOR $speakerName — TAP NEXT TURN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.impostorGlow
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onToggleTimerPause,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(if (isPaused) "Resume" else "Pause")
                        }
                        Button(
                            onClick = onNextTurn,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("discussion_next_turn_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text("Next Turn")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReturnToLobby,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("Lobby")
                }
                Button(
                    onClick = onStartVoting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.impostorAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("go_to_voting_phase_button")
                ) {
                    Icon(Icons.Filled.HowToVote, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Voting Phase")
                }
            }
        }
    }
}

@Composable
private fun VotingPhaseView(
    state: GameState,
    onCastVote: (String, String) -> Unit,
    onReturnToLobby: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    var activeVoterId by remember(state.players) {
        mutableStateOf(state.players.firstOrNull()?.id.orEmpty())
    }
    var showVerdict by remember(state.votesByPlayerId) { mutableStateOf(false) }
    val selectedSuspectId = state.votesByPlayerId[activeVoterId]
    val voteCounts = remember(state.votesByPlayerId) {
        state.votesByPlayerId.values.groupingBy { it }.eachCount()
    }
    val topCount = voteCounts.values.maxOrNull() ?: 0
    val topVotedPlayers = remember(state.players, voteCounts, topCount) {
        if (topCount > 0) state.players.filter { (voteCounts[it.id] ?: 0) == topCount } else emptyList()
    }
    val impostorCaught = topVotedPlayers.any { it.id in state.impostorIds }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassSectionCard(
                title = "VOTING PHASE • SELECT VOTER",
                icon = Icons.Filled.HowToVote,
                accentColor = palette.impostorAccent
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.players.forEach { voter ->
                        FilterChip(
                            selected = voter.id == activeVoterId,
                            onClick = { activeVoterId = voter.id },
                            label = { Text(voter.name) }
                        )
                    }
                }
                Text(
                    text = "Tap a suspect below to cast vote (${state.votesByPlayerId.size}/${state.players.size} voted).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(state.players.filter { it.id != activeVoterId }, key = { it.id }) { candidate ->
            val isSelected = selectedSuspectId == candidate.id
            val count = voteCounts[candidate.id] ?: 0
            Surface(
                onClick = { onCastVote(activeVoterId, candidate.id) },
                color = if (isSelected) palette.impostorSurface else MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) palette.impostorGlow else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = candidate.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) palette.impostorGlow else MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (count > 0) {
                            Text(
                                text = "$count vote${if (count == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.timerAccent
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Voted",
                                tint = palette.impostorGlow
                            )
                        }
                    }
                }
            }
        }

        if (showVerdict && topVotedPlayers.isNotEmpty()) {
            item {
                val verdictIdentity = if (impostorCaught) GameRoleIdentity.AGENT else GameRoleIdentity.IMPOSTOR
                val verdictStyle = verdictIdentity.style(palette)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .distinctRoleGlow(
                            isGlowing = true,
                            glowColor = verdictStyle.glow,
                            accentColor = verdictStyle.accent
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .background(verdictStyle.deepSurface)
                        .padding(16.dp)
                        .testTag("lobby_voting_verdict_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RoleEmblemBadge(
                            identity = verdictIdentity,
                            size = 44.dp,
                            animated = true
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (impostorCaught) "IMPOSTOR CAUGHT!" else "IMPOSTOR SURVIVED!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = verdictStyle.glow
                            )
                            Text(
                                text = "Top voted (${topCount}): ${topVotedPlayers.joinToString(", ") { it.name }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.votesByPlayerId.isNotEmpty()) {
                    Button(
                        onClick = { showVerdict = !showVerdict },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.impostorAccent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("reveal_voting_verdict_button")
                    ) {
                        Text(if (showVerdict) "Hide Verdict" else "Reveal Verdict")
                    }
                }
                Button(
                    onClick = onReturnToLobby,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.normalAccent,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("return_to_lobby_button")
                ) {
                    Text("Return to Lobby", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
