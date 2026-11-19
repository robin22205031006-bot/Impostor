package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.SoundCuePlayer
import com.example.data.LocalPlayer
import com.example.data.PlayerRole
import com.example.ui.components.AgentDirectorBadge
import com.example.ui.components.AgentPredictionCard
import com.example.ui.components.EliminatedPlayerReveal
import com.example.ui.components.EliminationRevealOverlay
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.EmergencyMeetingOverlay
import com.example.ui.components.GameRoleIdentity
import com.example.ui.components.ImpostorAnimatedBackdrop
import com.example.ui.components.OtherImpostorAllyNote
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.RoleEmblemBadge
import com.example.ui.components.RoleEmblemChip
import com.example.ui.components.RoleWordCard
import com.example.ui.components.SaviorAbilityCard
import com.example.ui.components.SettingsMenuSheet
import com.example.ui.components.TurnExpiredBroadcast
import com.example.ui.components.TurnTimerBar
import com.example.ui.components.TurnTimerEventBus
import com.example.ui.components.UltimateKillerAbilityCard
import com.example.ui.components.VotingPlayerCard
import com.example.ui.components.style
import com.example.ui.theme.LocalImpostorPalette
import com.example.data.GamePhase
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.LocalGameUiState
import com.example.viewmodel.LocalGameViewModel
import com.example.viewmodel.LocalPhase
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LocalGameScreen(
    viewModel: LocalGameViewModel,
    gameViewModel: GameViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key = "tactical_game_lobby")
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lobbyState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val soundEnabled by SoundCuePlayer.soundEnabled.collectAsStateWithLifecycle()
    val emergencySoundEnabled by SoundCuePlayer.emergencyAlertSoundEnabled.collectAsStateWithLifecycle()
    val palette = LocalImpostorPalette.current
    var dismissedEliminationRound by rememberSaveable { mutableIntStateOf(0) }
    var showConfirmExitDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showRoleLobby by rememberSaveable { mutableStateOf(false) }

    val isRoundInProgress = state.phase == LocalPhase.REVEAL ||
        state.phase == LocalPhase.PLAYING ||
        state.phase == LocalPhase.VOTING

    BackHandler(enabled = state.phase != LocalPhase.SETUP || (showRoleLobby && lobbyState.phase == GamePhase.LOBBY)) {
        if (state.phase == LocalPhase.SETUP && showRoleLobby) {
            showRoleLobby = false
        } else if (isRoundInProgress) {
            showConfirmExitDialog = true
        } else {
            viewModel.returnToSetup()
        }
    }

    val revealPlayer = state.players.getOrNull(state.revealPlayerIndex)
    val revealedIdentity = if (state.phase == LocalPhase.REVEAL && state.isRoleCardVisible && revealPlayer != null) {
        GameRoleIdentity.resolve(
            role = viewModel.roleFor(revealPlayer.id),
            isUltimateKiller = state.impostorAbilityEnabled && state.ultimateKillerId == revealPlayer.id,
            isSavior = state.saviorAgentEnabled && state.saviorAgentId == revealPlayer.id,
            isDirector = state.agentDirectorEnabled && state.agentDirectorId == revealPlayer.id
        )
    } else null

    val isTimerUrgentOrExpired = state.phase == LocalPhase.PLAYING &&
        state.timerEnabled &&
        !state.isTimerPaused &&
        state.remainingTurnSeconds <= 5

    val (orbPrimary, orbSecondary, orbTertiary) = when {
        revealedIdentity != null -> {
            val s = revealedIdentity.style(palette)
            Triple(s.glow, s.accent, s.deepSurface)
        }
        isTimerUrgentOrExpired -> Triple(palette.impostorGlow, palette.impostorAccent, palette.timerAccent)
        state.phase == LocalPhase.VOTING -> Triple(palette.impostorAccent, palette.cyanAccent, palette.directorAccent)
        state.phase == LocalPhase.RESULTS -> {
            val winAccent = if (state.crewWon) palette.normalGlow else palette.impostorGlow
            Triple(winAccent, palette.cyanAccent, if (state.crewWon) palette.normalAccent else palette.impostorAccent)
        }
        else -> Triple(palette.cyanAccent, palette.normalAccent, palette.impostorAccent)
    }

    ImpostorAnimatedBackdrop(
        primaryOrbColor = orbPrimary,
        secondaryOrbColor = orbSecondary,
        tertiaryOrbColor = orbTertiary
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.phase != LocalPhase.SETUP) {
                        IconButton(
                            onClick = {
                                if (isRoundInProgress) {
                                    showConfirmExitDialog = true
                                } else {
                                    viewModel.returnToSetup()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("local_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Setup",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    Column {
                        val isLobbyTab = state.phase == LocalPhase.SETUP && showRoleLobby
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PulsingStatusDot(
                                color = when {
                                    isLobbyTab -> palette.cyanAccent
                                    state.phase == LocalPhase.SETUP -> palette.normalAccent
                                    else -> palette.cyanAccent
                                }
                            )
                            Text(
                                text = when {
                                    isLobbyTab -> "ROLE LOBBY"
                                    state.phase == LocalPhase.SETUP -> "IMPOSTOR"
                                    else -> "PASS & PLAY"
                                },
                                style = if (state.phase == LocalPhase.SETUP) {
                                    MaterialTheme.typography.headlineMedium
                                } else {
                                    MaterialTheme.typography.titleLarge
                                },
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = if (isLobbyTab) {
                                "Role Lobby • ${lobbyState.phase.title} • ${lobbyState.players.size} Players"
                            } else {
                                "Pass & Play • ${state.gameMode.title} • ${state.players.size} Players"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isLobbyTab) palette.cyanAccent else palette.normalAccent
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Open settings menu",
                            tint = palette.cyanAccent
                        )
                    }
                    IconButton(
                        onClick = SoundCuePlayer::toggleSound,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("local_sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (soundEnabled) "Mute sound cues" else "Enable sound cues",
                            tint = if (soundEnabled) palette.cyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.phase != LocalPhase.SETUP) {
                        TextButton(
                            onClick = {
                                if (isRoundInProgress) {
                                    showConfirmExitDialog = true
                                } else {
                                    viewModel.returnToSetup()
                                }
                            },
                            modifier = Modifier.testTag("local_end_round_button")
                        ) {
                            Text("Setup", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (state.errorBanner != null) {
                Surface(
                    color = palette.impostorSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.impostorAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.errorBanner ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.impostorGlow,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::clearError, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = palette.impostorGlow)
                        }
                    }
                }
            }

            if (state.phase == LocalPhase.SETUP) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fullActive = !showRoleLobby
                    Surface(
                        onClick = { showRoleLobby = false },
                        color = if (fullActive) palette.normalSurface else MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (fullActive) 1.5.dp else 1.dp,
                            color = if (fullActive) palette.normalAccent else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_pass_and_play")
                    ) {
                        Text(
                            text = "Pass & Play",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (fullActive) palette.normalGlow else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                    Surface(
                        onClick = { showRoleLobby = true },
                        color = if (showRoleLobby) palette.cyanSurface else MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (showRoleLobby) 1.5.dp else 1.dp,
                            color = if (showRoleLobby) palette.cyanAccent else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_role_lobby")
                    ) {
                        Text(
                            text = "Role Lobby",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (showRoleLobby) palette.cyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = state.phase,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(260)) +
                        slideInVertically(animationSpec = tween(260)) { it / 16 } +
                        scaleIn(initialScale = 0.98f, animationSpec = tween(260)))
                        .togetherWith(fadeOut(animationSpec = tween(180)))
                        .using(SizeTransform(clip = false))
                },
                label = "local_phase_transition",
                modifier = Modifier.weight(1f)
            ) { phase ->
                when (phase) {
                    LocalPhase.SETUP -> {
                        if (showRoleLobby) {
                            GameLobbyScreen(viewModel = gameViewModel)
                        } else {
                            GameSetupScreen(
                                state = state,
                                onAddPlayer = viewModel::addPlayer,
                                onRenamePlayer = viewModel::renamePlayer,
                                onRemovePlayer = viewModel::removePlayer,
                                onSetPlayerCount = viewModel::setPlayerCount,
                                onSelectMode = viewModel::setGameMode,
                                onSetImpostors = viewModel::setImpostorCount,
                                onSetDuration = viewModel::setTurnDuration,
                                onSelectCategory = viewModel::setCategory,
                                onToggleHints = viewModel::setHintsEnabled,
                                onToggleCategoryClues = viewModel::setCategoryCluesEnabled,
                                onToggleTimer = viewModel::setTimerEnabled,
                                onToggleImpostorAbility = viewModel::setImpostorAbilityEnabled,
                                onToggleSaviorAgent = viewModel::setSaviorAgentEnabled,
                                onToggleAgentDirector = viewModel::setAgentDirectorEnabled,
                                onToggleAgentPrediction = viewModel::setAgentPredictionEnabled,
                                onStartRound = viewModel::startRound
                            )
                        }
                    }

                    LocalPhase.REVEAL -> LocalRevealContent(
                        state = state,
                        roleForPlayer = viewModel::roleFor,
                        onSelectKillTarget = viewModel::setNullifiedVoter,
                        onSelectProtectedTarget = viewModel::setProtectedPlayer,
                        onSelectPrediction = viewModel::setAgentPrediction,
                        onReveal = viewModel::showCurrentPlayerCard,
                        onHideAndNext = viewModel::hideAndAdvanceReveal
                    )

                    LocalPhase.PLAYING -> LocalPlayingContent(
                        state = state,
                        roleForPlayer = viewModel::roleFor,
                        onTogglePause = viewModel::toggleTimerPause,
                        onAddSeconds = { viewModel.addExtraSeconds(10) },
                        onNextTurn = viewModel::nextTurn,
                        onUpdateGuess = viewModel::updateImpostorWordGuess,
                        onSelectKillTarget = viewModel::setNullifiedVoter,
                        onSelectProtectedTarget = viewModel::setProtectedPlayer,
                        onSelectPrediction = viewModel::setAgentPrediction,
                        onTriggerEmergencyMeeting = {
                            viewModel.triggerEmergencyMeeting(
                                reason = "Suspicious clue reported during discussion!"
                            )
                        },
                        onGoToVoting = viewModel::goToVoting
                    )

                    LocalPhase.VOTING -> LocalVotingContent(
                        state = state,
                        onRevealBallot = viewModel::revealVoterBallot,
                        onCastVote = viewModel::castLocalVote,
                        onConfirmVoteAndNext = viewModel::confirmVoteAndAdvance,
                        onUpdateGuess = viewModel::updateImpostorWordGuess,
                        onSubmitResults = viewModel::submitResults
                    )

                    LocalPhase.RESULTS -> LocalResultsContent(
                        state = state,
                        onNextRound = viewModel::startRound,
                        onReturnSetup = viewModel::returnToSetup
                    )
                }
            }
        }

        val showEliminationOverlay =
            state.phase == LocalPhase.RESULTS && dismissedEliminationRound != state.roundNumber
        AnimatedVisibility(
            visible = showEliminationOverlay,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(220))
        ) {
            val topVotes = state.roundVoteCounts.values.maxOrNull() ?: 0
            val eliminatedPlayers = remember(state.roundNumber, state.roundVoteCounts, state.players, state.impostorIds, state.ultimateKillerId, state.saviorAgentId, state.agentDirectorId) {
                if (topVotes > 0) {
                    state.roundVoteCounts.filterValues { it == topVotes }.keys.mapNotNull { pid ->
                        state.players.firstOrNull { it.id == pid }?.let { player ->
                            val isImp = player.id in state.impostorIds
                            val identity = GameRoleIdentity.resolve(
                                role = if (isImp) PlayerRole.IMPOSTOR else PlayerRole.NORMAL,
                                isUltimateKiller = state.impostorAbilityEnabled && player.id == state.ultimateKillerId,
                                isSavior = state.saviorAgentEnabled && player.id == state.saviorAgentId,
                                isDirector = state.agentDirectorEnabled && player.id == state.agentDirectorId
                            )
                            EliminatedPlayerReveal(
                                id = player.id,
                                name = player.name,
                                isImpostor = isImp,
                                voteCount = topVotes,
                                roleIdentity = identity
                            )
                        }
                    }
                } else {
                    emptyList()
                }
            }
            androidx.compose.runtime.key(state.roundNumber) {
                EliminationRevealOverlay(
                    eliminatedPlayers = eliminatedPlayers,
                    crewWon = state.crewWon,
                    impostorGuessCorrect = state.impostorGuessCorrect,
                    onRevealVerdict = { SoundCuePlayer.playVotingResultsCue(state.crewWon) },
                    onDismiss = { dismissedEliminationRound = state.roundNumber }
                )
            }
        }

        AnimatedVisibility(
            visible = state.phase == LocalPhase.PLAYING && state.emergencyMeeting.isActive,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            EmergencyMeetingOverlay(
                alert = state.emergencyMeeting,
                onProceedNow = viewModel::dismissEmergencyMeetingAndVote
            )
        }

        if (showSettingsSheet) {
            val activeTimerDuration = if (state.phase == LocalPhase.SETUP && showRoleLobby) {
                lobbyState.turnDurationSeconds
            } else {
                state.turnDurationSeconds
            }
            SettingsMenuSheet(
                emergencyAlertSoundEnabled = emergencySoundEnabled,
                soundEffectsEnabled = soundEnabled,
                discussionDurationSeconds = activeTimerDuration,
                onToggleEmergencyAlertSound = { enabled ->
                    gameViewModel.setEmergencyAlertSoundEnabled(enabled)
                },
                onToggleSoundEffects = { enabled ->
                    gameViewModel.setSoundEffectsEnabled(enabled)
                },
                onChangeDiscussionDuration = { seconds ->
                    gameViewModel.setTurnDuration(seconds)
                    viewModel.setTurnDuration(seconds)
                },
                onDismiss = { showSettingsSheet = false }
            )
        }

        if (showConfirmExitDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmExitDialog = false },
                title = { Text("End Current Round?") },
                text = { Text("Returning to setup will cancel the active round in progress.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmExitDialog = false
                            viewModel.returnToSetup()
                        }
                    ) {
                        Text("End Round")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmExitDialog = false }) {
                        Text("Keep Playing")
                    }
                }
            )
        }
    }
}

@Composable
private fun LocalRevealContent(
    state: LocalGameUiState,
    roleForPlayer: (String) -> PlayerRole,
    onSelectKillTarget: (String?) -> Unit,
    onSelectProtectedTarget: (String?) -> Unit,
    onSelectPrediction: (String, String?) -> Unit,
    onReveal: () -> Unit,
    onHideAndNext: () -> Unit
) {
    val player = state.players.getOrNull(state.revealPlayerIndex) ?: return
    val wordEntry = state.activeWord ?: return
    val role = roleForPlayer(player.id)
    val otherPlayers = remember(state.players, player.id) {
        state.players.filter { it.id != player.id }.map { it.id to it.name }
    }

    androidx.compose.runtime.key(player.id, state.roundNumber) {
        RoleRevealScreen(
            playerName = player.name,
            playerIndex = state.revealPlayerIndex,
            totalPlayers = state.players.size,
            turnDurationSeconds = state.turnDurationSeconds,
            role = role,
            category = wordEntry.category,
            genre = wordEntry.genre,
            secretWord = wordEntry.word,
            hint = state.activeHint,
            isRevealed = state.isRoleCardVisible,
            showCategoryAndGenre = state.categoryCluesEnabled,
            showHint = state.hintsEnabled,
            timerEnabled = state.timerEnabled,
            impostorAbilityEnabled = state.impostorAbilityEnabled,
            isUltimateKiller = state.ultimateKillerId == player.id,
            saviorAgentEnabled = state.saviorAgentEnabled,
            isSavior = state.saviorAgentId == player.id,
            agentDirectorEnabled = state.agentDirectorEnabled,
            isDirector = state.agentDirectorId == player.id,
            agentPredictionEnabled = state.agentPredictionEnabled,
            otherPlayers = otherPlayers,
            selectedKillTargetId = state.nullifiedVoterId,
            selectedProtectedId = state.protectedPlayerId,
            selectedPredictionId = state.agentPredictions[player.id],
            onSelectKillTarget = onSelectKillTarget,
            onSelectProtectedTarget = onSelectProtectedTarget,
            onSelectPrediction = { suspectId -> onSelectPrediction(player.id, suspectId) },
            onReveal = onReveal,
            onHideAndNext = onHideAndNext
        )
    }
}

@Composable
private fun LocalPlayingContent(
    state: LocalGameUiState,
    roleForPlayer: (String) -> PlayerRole,
    onTogglePause: () -> Unit,
    onAddSeconds: () -> Unit,
    onNextTurn: () -> Unit,
    onUpdateGuess: (String) -> Unit,
    onSelectKillTarget: (String?) -> Unit,
    onSelectProtectedTarget: (String?) -> Unit,
    onSelectPrediction: (String, String?) -> Unit,
    onTriggerEmergencyMeeting: () -> Unit,
    onGoToVoting: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val context = LocalContext.current
    val activeUid = state.turnOrderIds.getOrNull(state.currentTurnIndex)
    val activePlayer = state.players.firstOrNull { it.id == activeUid }
    var peekPlayer by remember { mutableStateOf<LocalPlayer?>(null) }
    var showGuessInput by remember { mutableStateOf(false) }
    var lastBroadcast by remember { mutableStateOf<TurnExpiredBroadcast?>(null) }
    var prevRemaining by remember(state.currentTurnIndex, state.roundNumber) {
        mutableIntStateOf(state.remainingTurnSeconds)
    }

    LaunchedEffect(Unit) {
        TurnTimerEventBus.events.collectLatest { event ->
            lastBroadcast = event
        }
    }

    LaunchedEffect(
        state.remainingTurnSeconds,
        state.isTimerPaused,
        state.timerEnabled,
        state.currentTurnIndex,
        state.roundNumber
    ) {
        if (state.timerEnabled && !state.isTimerPaused && prevRemaining > 0 && state.remainingTurnSeconds == 0) {
            TurnTimerEventBus.emitExpired(
                context = context,
                playerName = activePlayer?.name ?: "Discussion",
                turnIndex = state.currentTurnIndex,
                totalTurns = state.turnOrderIds.size
            )
        }
        prevRemaining = state.remainingTurnSeconds
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (state.timerEnabled) {
                androidx.compose.runtime.key(state.currentTurnIndex, state.roundNumber) {
                    TurnTimerBar(
                        activePlayerName = activePlayer?.name ?: "Discussion",
                        turnIndex = state.currentTurnIndex,
                        totalTurns = state.turnOrderIds.size,
                        remainingSeconds = state.remainingTurnSeconds,
                        totalSeconds = state.turnDurationSeconds,
                        isPaused = state.isTimerPaused,
                        showControls = true,
                        onTogglePause = onTogglePause,
                        onNextTurn = onNextTurn
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT SPEAKER",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.cyanAccent
                            )
                            Text(
                                text = activePlayer?.name ?: "Discussion",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Turn ${state.currentTurnIndex + 1}/${state.turnOrderIds.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.cyanAccent
                        )
                    }
                }
            }
        }

        if (lastBroadcast != null && state.timerEnabled && state.remainingTurnSeconds == 0 && !state.isTimerPaused) {
            item {
                val broadcast = lastBroadcast!!
                Surface(
                    color = palette.impostorSurface.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.impostorGlow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Turn timer expired alert",
                            tint = palette.impostorGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Time's up for ${broadcast.playerName} (Turn ${broadcast.turnIndex + 1}/${broadcast.totalTurns}) — tap Next Turn or Finish Turns!",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.impostorGlow,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.timerEnabled) {
                    OutlinedButton(
                        onClick = onAddSeconds,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("+10s Time")
                    }
                }
                Button(
                    onClick = onNextTurn,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("local_next_turn_button")
                ) {
                    Text(
                        if (state.currentTurnIndex + 1 >= state.turnOrderIds.size) "Finish Turns"
                        else "Next Turn"
                    )
                }
            }
        }

        // Category + Genre table clue summary (only shown when host enables category clues)
        item {
            val wordEntry = state.activeWord
            if (wordEntry != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ROUND TABLE BRIEFING",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.cyanAccent
                        )
                        if (state.categoryCluesEnabled) {
                            Text(
                                text = "Category: ${wordEntry.category} • Genre: ${wordEntry.genre}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Each player gives one clue on their turn. Impostors try to blend in or guess the word!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Optional Impostor Blend-in Guess toggle
        item {
            Surface(
                color = palette.impostorSurface.copy(alpha = 0.65f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, palette.impostorAccent.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "IMPOSTOR WORD GUESS",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.impostorGlow
                        )
                        TextButton(onClick = { showGuessInput = !showGuessInput }) {
                            Text(if (showGuessInput) "Hide" else "Enter Guess", color = palette.impostorGlow)
                        }
                    }
                    AnimatedVisibility(visible = showGuessInput) {
                        OutlinedTextField(
                            value = state.impostorWordGuess,
                            onValueChange = onUpdateGuess,
                            label = { Text("Impostor's Secret Word Guess") },
                            placeholder = { Text("Type your guess to steal the win") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Private Role Peek (Tap your name privately to verify your word/hint)
        item {
            Text(
                text = "NEED TO RE-CHECK YOUR CARD? TAP YOUR NAME PRIVATELY:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(state.players, key = { it.id }) { player ->
            Surface(
                onClick = { peekPlayer = player },
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = "Peek ${player.name}'s card",
                        tint = palette.cyanAccent
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onTriggerEmergencyMeeting,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.impostorAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("local_emergency_meeting_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Campaign,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Call Emergency Meeting / Report", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = onGoToVoting,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.timerAccent,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("local_go_to_voting_button")
                ) {
                    Text("Proceed to Voting", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    val target = peekPlayer
    val wordEntry = state.activeWord
    if (target != null && wordEntry != null) {
        val targetRole = roleForPlayer(target.id)
        val targetIdentity = GameRoleIdentity.resolve(
            role = targetRole,
            isUltimateKiller = state.impostorAbilityEnabled && state.ultimateKillerId == target.id,
            isSavior = state.saviorAgentEnabled && state.saviorAgentId == target.id,
            isDirector = state.agentDirectorEnabled && state.agentDirectorId == target.id
        )
        val otherPlayers = remember(state.players, target.id) {
            state.players.filter { it.id != target.id }.map { it.id to it.name }
        }
        AlertDialog(
            onDismissRequest = { peekPlayer = null },
            title = { Text("${target.name}'s Private Card") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoleWordCard(
                        role = targetRole,
                        category = wordEntry.category,
                        genre = wordEntry.genre,
                        secretWord = wordEntry.word,
                        hint = state.activeHint,
                        showCategoryAndGenre = state.categoryCluesEnabled,
                        showHint = state.hintsEnabled,
                        roleIdentity = targetIdentity
                    )
                    if (state.impostorAbilityEnabled && targetRole == PlayerRole.IMPOSTOR) {
                        if (state.ultimateKillerId == target.id) {
                            UltimateKillerAbilityCard(
                                candidates = otherPlayers,
                                selectedTargetId = state.nullifiedVoterId,
                                onSelectTarget = onSelectKillTarget
                            )
                        } else {
                            OtherImpostorAllyNote()
                        }
                    }
                    if (state.agentDirectorEnabled && targetRole == PlayerRole.NORMAL && state.agentDirectorId == target.id) {
                        AgentDirectorBadge()
                    }
                    if (state.saviorAgentEnabled && targetRole == PlayerRole.NORMAL && state.saviorAgentId == target.id) {
                        SaviorAbilityCard(
                            candidates = otherPlayers,
                            selectedProtectedId = state.protectedPlayerId,
                            onSelectProtected = onSelectProtectedTarget
                        )
                    }
                    if (state.agentPredictionEnabled && targetRole == PlayerRole.NORMAL) {
                        AgentPredictionCard(
                            candidates = otherPlayers,
                            selectedSuspectId = state.agentPredictions[target.id],
                            onSelectSuspect = { suspectId -> onSelectPrediction(target.id, suspectId) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { peekPlayer = null }) {
                    Text("Hide Card")
                }
            }
        )
    }
}

@Composable
private fun LocalVotingContent(
    state: LocalGameUiState,
    onRevealBallot: () -> Unit,
    onCastVote: (String, String) -> Unit,
    onConfirmVoteAndNext: () -> Unit,
    onUpdateGuess: (String) -> Unit,
    onSubmitResults: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val allVoted = state.votingPlayerIndex >= state.players.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!allVoted) {
            val voter = state.players[state.votingPlayerIndex]
            val selectedSuspectId = state.votesByPlayerId[voter.id]
            val candidates = state.players.filter { it.id != voter.id }

            item {
                Text(
                    text = "PRIVATE VOTING (${state.votingPlayerIndex + 1}/${state.players.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.cyanAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (state.isVoteCardRevealed) "${voter.name}: Who is the Impostor?" else "Pass phone to ${voter.name}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (!state.isVoteCardRevealed) {
                item {
                    Surface(
                        onClick = onRevealBallot,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(2.dp, palette.cyanAccent.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .testTag("local_open_ballot_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TouchApp,
                                contentDescription = "Open private ballot",
                                tint = palette.cyanAccent,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "TAP TO OPEN ${voter.name.uppercase()}'S BALLOT",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.cyanAccent,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Other players look away while you cast your secret vote.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(candidates, key = { it.id }) { candidate ->
                    val selected = selectedSuspectId == candidate.id
                    VotingPlayerCard(
                        player = candidate,
                        isSelected = selected,
                        voteCount = 0,
                        onClick = { onCastVote(voter.id, candidate.id) },
                        modifier = Modifier.testTag("local_vote_player_${candidate.id}")
                    )
                }

                item {
                    Button(
                        onClick = onConfirmVoteAndNext,
                        enabled = selectedSuspectId != null,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.cyanAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("local_lock_vote_button")
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = if (state.votingPlayerIndex >= state.players.size - 1) {
                                "Lock Final Vote"
                            } else {
                                "Lock Vote & Pass Phone"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        } else {
            item {
                Surface(
                    color = palette.normalSurface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, palette.normalAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ALL ${state.players.size} VOTES LOCKED",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.normalGlow
                        )
                        Text(
                            text = "Every player has cast a private vote. Enter an optional impostor word guess below, then reveal the truth!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, palette.impostorAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "IMPOSTOR'S LAST-CHANCE WORD GUESS (OPTIONAL)",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.impostorGlow
                        )
                        OutlinedTextField(
                            value = state.impostorWordGuess,
                            onValueChange = onUpdateGuess,
                            label = { Text("Secret Word Guess") },
                            placeholder = { Text("If the impostor guesses right, they steal the win!") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("local_impostor_guess_input")
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onSubmitResults,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.impostorAccent,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("local_reveal_results_button")
                ) {
                    Text("Reveal Impostor & Secret Word", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun LocalResultsContent(
    state: LocalGameUiState,
    onNextRound: () -> Unit,
    onReturnSetup: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val wordEntry = state.activeWord
    val impostorPlayers = state.players.filter { it.id in state.impostorIds }
    val nameMap = remember(state.players) { state.players.associate { it.id to it.name } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val winnerIdentity = if (state.crewWon) GameRoleIdentity.AGENT else GameRoleIdentity.IMPOSTOR
            val winnerStyle = winnerIdentity.style(palette)
            Surface(
                color = winnerStyle.deepSurface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, winnerStyle.glow.copy(alpha = 0.85f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val spread = 12.dp.toPx()
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(winnerStyle.glow.copy(alpha = 0.25f), Color.Transparent),
                                radius = maxOf(size.width, size.height) * 0.72f
                            ),
                            cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                            size = size.copy(width = size.width + spread, height = size.height + spread),
                            topLeft = Offset(-spread / 2f, -spread / 2f)
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RoleEmblemBadge(
                        identity = winnerIdentity,
                        size = 58.dp,
                        animated = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (state.crewWon) "VICTORY" else "DEFEAT",
                        style = MaterialTheme.typography.displayMedium,
                        color = winnerStyle.glow,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (state.crewWon) "CREWMATES WIN!" else "IMPOSTORS WIN!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = winnerStyle.glow,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            state.impostorGuessCorrect -> "An impostor correctly deduced the secret word!"
                            state.crewWon -> "The impostor was successfully voted out!"
                            else -> "The impostors achieved their goal!"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (wordEntry != null) {
            item {
                RoleWordCard(
                    role = PlayerRole.NORMAL,
                    category = wordEntry.category,
                    genre = wordEntry.genre,
                    secretWord = wordEntry.word,
                    hint = state.activeHint,
                    showCategoryAndGenre = state.categoryCluesEnabled,
                    showHint = state.hintsEnabled
                )
            }
        }

        if (state.impostorAbilityEnabled && state.ultimateKillerId != null) {
            item {
                val killerStyle = GameRoleIdentity.AGENT_KILLER.style(palette)
                val killerName = nameMap[state.ultimateKillerId] ?: "Impostor"
                val targetedId = state.rawKillerTargetId ?: state.nullifiedVoterId
                val targetedName = targetedId?.let { nameMap[it] }
                val targetedVotedImpostor = targetedId?.let { state.votesByPlayerId[it] } in state.impostorIds

                Surface(
                    color = killerStyle.deepSurface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, killerStyle.glow.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoleEmblemBadge(
                            identity = GameRoleIdentity.AGENT_KILLER,
                            size = 38.dp,
                            animated = true
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ULTIMATE AGENT KILLER: $killerName",
                                style = MaterialTheme.typography.labelLarge,
                                color = killerStyle.glow
                            )
                            Text(
                                text = when {
                                    targetedName == null -> "$killerName chose not to silence anyone's vote."
                                    state.saviorSavedTarget -> "$killerName attacked $targetedName — BLOCKED by Savior Shield! Vote still counted!"
                                    targetedVotedImpostor -> "$killerName silenced $targetedName — their vote for an impostor was VOIDED!"
                                    else -> "$killerName silenced $targetedName's vote."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        if (state.saviorAgentEnabled && state.saviorAgentId != null) {
            item {
                val saviorStyle = GameRoleIdentity.SAVIOR_AGENT.style(palette)
                val saviorName = nameMap[state.saviorAgentId] ?: "Savior"
                val protectedName = state.protectedPlayerId?.let { nameMap[it] }

                Surface(
                    color = saviorStyle.deepSurface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, saviorStyle.glow.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoleEmblemBadge(
                            identity = GameRoleIdentity.SAVIOR_AGENT,
                            size = 38.dp,
                            animated = true
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "SAVIOR AGENT: $saviorName",
                                style = MaterialTheme.typography.labelLarge,
                                color = saviorStyle.glow
                            )
                            Text(
                                text = when {
                                    protectedName == null -> "$saviorName chose not to shield anyone (0 deduction)."
                                    state.saviorSavedTarget -> "$saviorName shielded $protectedName and blocked the Agent Killer! (+1 Savior bonus)"
                                    else -> "$saviorName shielded $protectedName (Killer targeted someone else — 0 deduction)."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        if (state.agentDirectorEnabled && state.agentDirectorId != null) {
            item {
                val directorStyle = GameRoleIdentity.AGENT_DIRECTOR.style(palette)
                val directorName = nameMap[state.agentDirectorId] ?: "Director"
                val directorVoided = state.agentDirectorId == state.nullifiedVoterId

                Surface(
                    color = directorStyle.deepSurface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, directorStyle.glow.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoleEmblemBadge(
                            identity = GameRoleIdentity.AGENT_DIRECTOR,
                            size = 38.dp,
                            animated = true
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "AGENT DIRECTOR: $directorName",
                                style = MaterialTheme.typography.labelLarge,
                                color = directorStyle.glow
                            )
                            Text(
                                text = if (directorVoided) {
                                    "$directorName's 2x vote was voided by the Agent Killer."
                                } else {
                                    "$directorName's vote counted as 2 votes in the tally."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "THE IMPOSTOR${if (impostorPlayers.size > 1) "S" else ""} WERE") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    impostorPlayers.forEach { imp ->
                        val impIdentity = if (state.impostorAbilityEnabled && imp.id == state.ultimateKillerId) {
                            GameRoleIdentity.AGENT_KILLER
                        } else {
                            GameRoleIdentity.IMPOSTOR
                        }
                        val impStyle = impIdentity.style(palette)
                        Surface(
                            color = impStyle.deepSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, impStyle.glow.copy(alpha = 0.8f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RoleEmblemBadge(
                                        identity = impIdentity,
                                        size = 34.dp,
                                        animated = true
                                    )
                                    Text(
                                        text = if (impIdentity == GameRoleIdentity.AGENT_KILLER) "${imp.name} (Agent Killer)" else imp.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = impStyle.glow
                                    )
                                }
                                val votesAgainst = state.roundVoteCounts[imp.id] ?: 0
                                Text(
                                    text = "$votesAgainst counted vote${if (votesAgainst == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = impStyle.glow
                                )
                            }
                        }
                    }
                    if (state.impostorWordGuess.isNotBlank()) {
                        Text(
                            text = "Impostor Guess: \"${state.impostorWordGuess}\" (${if (state.impostorGuessCorrect) "CORRECT" else "MISSED"})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.impostorGuessCorrect) palette.normalGlow else palette.impostorGlow
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "VOTES, PREDICTIONS & SCOREBOARD") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.players.sortedByDescending { it.score }.forEach { player ->
                        val isImp = player.id in state.impostorIds
                        val isKiller = state.impostorAbilityEnabled && player.id == state.ultimateKillerId
                        val isSavior = state.saviorAgentEnabled && player.id == state.saviorAgentId
                        val isDirector = state.agentDirectorEnabled && player.id == state.agentDirectorId
                        val playerIdentity = GameRoleIdentity.resolve(
                            role = if (isImp) PlayerRole.IMPOSTOR else PlayerRole.NORMAL,
                            isUltimateKiller = isKiller,
                            isSavior = isSavior,
                            isDirector = isDirector
                        )
                        val playerStyle = playerIdentity.style(palette)
                        val delta = state.roundDeltas[player.id] ?: 0
                        val deltaText = if (delta >= 0) "+$delta" else "$delta"
                        val deltaColor = when {
                            delta > 0 -> palette.normalGlow
                            delta < 0 -> palette.impostorGlow
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val votedForId = state.votesByPlayerId[player.id]
                        val votedForName = votedForId?.let { nameMap[it] } ?: "None"
                        val isVoided = player.id == state.nullifiedVoterId
                        val isProtected = state.saviorAgentEnabled && player.id == state.protectedPlayerId
                        val predictedId = state.agentPredictions[player.id]
                        val predictedName = predictedId?.let { nameMap[it] }

                        val roleTag = buildList {
                            if (isImp) add(if (isKiller) "AGENT KILLER" else "IMPOSTOR")
                            if (isSavior) add("SAVIOR")
                            if (isDirector) add("DIRECTOR")
                        }.joinToString(" • ")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RoleEmblemBadge(
                                identity = playerIdentity,
                                size = 32.dp,
                                animated = true
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (roleTag.isNotEmpty()) "${player.name} ($roleTag)" else player.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = playerStyle.glow,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "($deltaText)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = deltaColor
                                        )
                                        Text(
                                            text = "${player.score} pts",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (player.score < 0) palette.impostorGlow else palette.timerAccent
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isVoided) {
                                        Icon(
                                            imageVector = Icons.Filled.Block,
                                            contentDescription = "Vote voided",
                                            tint = palette.impostorGlow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else if (isProtected) {
                                        Icon(
                                            imageVector = Icons.Filled.Shield,
                                            contentDescription = "Shielded by Savior",
                                            tint = palette.saviorGlow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = buildString {
                                            append("Voted: $votedForName")
                                            if (isDirector && !isVoided && votedForId != null) append(" [2x]")
                                            if (isProtected) {
                                                append(if (state.saviorSavedTarget) " [SAVED]" else " [SHIELDED]")
                                            }
                                            if (isVoided) append(" [VOIDED]")
                                            if (!isImp && state.agentPredictionEnabled) {
                                                if (predictedName != null) {
                                                    val hit = predictedId in state.impostorIds
                                                    append(" • Predicted: $predictedName (${if (hit) "+1" else "-2"})")
                                                } else {
                                                    append(" • Prediction: Skipped")
                                                }
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when {
                                            isVoided -> palette.impostorGlow
                                            isProtected && state.saviorSavedTarget -> palette.saviorGlow
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
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
                    onClick = onReturnSetup,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Text("Edit Setup")
                }
                Button(
                    onClick = onNextRound,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.normalAccent,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("local_next_round_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Next Round")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}
