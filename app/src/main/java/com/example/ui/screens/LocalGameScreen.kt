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
import androidx.compose.material.icons.filled.Public
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
import com.example.ui.components.ImpostorAnimatedBackdrop
import com.example.ui.components.OtherImpostorAllyNote
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.RoleWordCard
import com.example.ui.components.SaviorAbilityCard
import com.example.ui.components.TurnTimerBar
import com.example.ui.components.UltimateKillerAbilityCard
import com.example.ui.theme.LocalImpostorPalette
import com.example.viewmodel.LocalGameUiState
import com.example.viewmodel.LocalGameViewModel
import com.example.viewmodel.LocalPhase

@Composable
fun LocalGameScreen(
    viewModel: LocalGameViewModel,
    onOpenOnline: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val soundEnabled by SoundCuePlayer.soundEnabled.collectAsStateWithLifecycle()
    val palette = LocalImpostorPalette.current
    var dismissedEliminationRound by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = state.phase != LocalPhase.SETUP) {
        viewModel.returnToSetup()
    }

    ImpostorAnimatedBackdrop {
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
                            onClick = viewModel::returnToSetup,
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PulsingStatusDot(
                                color = if (state.phase == LocalPhase.SETUP) palette.normalAccent else palette.cyanAccent
                            )
                            Text(
                                text = if (state.phase == LocalPhase.SETUP) "IMPOSTOR" else "PASS & PLAY",
                                style = if (state.phase == LocalPhase.SETUP) {
                                    MaterialTheme.typography.headlineMedium
                                } else {
                                    MaterialTheme.typography.titleLarge
                                },
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "Pass & Play • ${state.gameMode.title} • ${state.players.size} Players",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.normalAccent
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = SoundCuePlayer::toggleSound,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("local_sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (soundEnabled) "Mute sound cues" else "Enable sound cues",
                            tint = if (soundEnabled) palette.cyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.phase == LocalPhase.SETUP) {
                        OutlinedButton(
                            onClick = onOpenOnline,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.75f)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("open_online_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = "Online Multiplayer",
                                tint = palette.cyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Online",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.cyanAccent
                            )
                        }
                    } else {
                        TextButton(
                            onClick = viewModel::returnToSetup,
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
                    LocalPhase.SETUP -> GameSetupScreen(
                        state = state,
                        onAddPlayer = viewModel::addPlayer,
                        onRenamePlayer = viewModel::renamePlayer,
                        onRemovePlayer = viewModel::removePlayer,
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
            val eliminatedPlayers = remember(state.roundNumber, state.roundVoteCounts, state.players, state.impostorIds) {
                if (topVotes > 0) {
                    state.roundVoteCounts.filterValues { it == topVotes }.keys.mapNotNull { pid ->
                        state.players.firstOrNull { it.id == pid }?.let { player ->
                            EliminatedPlayerReveal(
                                id = player.id,
                                name = player.name,
                                isImpostor = player.id in state.impostorIds,
                                voteCount = topVotes
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
    onGoToVoting: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val activeUid = state.turnOrderIds.getOrNull(state.currentTurnIndex)
    val activePlayer = state.players.firstOrNull { it.id == activeUid }
    var peekPlayer by remember { mutableStateOf<LocalPlayer?>(null) }
    var showGuessInput by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (state.timerEnabled) {
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

    if (peekPlayer != null && state.activeWord != null) {
        val target = peekPlayer!!
        val wordEntry = state.activeWord!!
        val targetRole = roleForPlayer(target.id)
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
                        showHint = state.hintsEnabled
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
                    Surface(
                        onClick = { onCastVote(voter.id, candidate.id) },
                        color = if (selected) palette.impostorSurface else MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) palette.impostorGlow else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("local_vote_player_${candidate.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = candidate.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) palette.impostorGlow else MaterialTheme.colorScheme.onSurface
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected suspect",
                                    tint = palette.impostorGlow
                                )
                            }
                        }
                    }
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
            val accent = if (state.crewWon) palette.normalAccent else palette.impostorAccent
            val bg = if (state.crewWon) palette.normalSurface else palette.impostorSurface
            Surface(
                color = bg,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.crewWon) "AGENTS WIN!" else "IMPOSTORS WIN!",
                        style = MaterialTheme.typography.displayMedium,
                        color = accent,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            state.impostorGuessCorrect -> "An impostor correctly deduced the secret word!"
                            state.crewWon -> "The agents exposed the impostor!"
                            else -> "The impostor survived the vote undetected!"
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
                val killerName = nameMap[state.ultimateKillerId] ?: "Impostor"
                val targetedId = state.rawKillerTargetId ?: state.nullifiedVoterId
                val targetedName = targetedId?.let { nameMap[it] }
                val targetedVotedImpostor = targetedId?.let { state.votesByPlayerId[it] } in state.impostorIds

                Surface(
                    color = palette.impostorSurface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, palette.impostorAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = palette.impostorGlow,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ULTIMATE AGENT KILLER: $killerName",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.impostorGlow
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
                val saviorName = nameMap[state.saviorAgentId] ?: "Savior"
                val protectedName = state.protectedPlayerId?.let { nameMap[it] }

                Surface(
                    color = palette.normalSurface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, palette.normalAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = palette.normalGlow,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "SAVIOR AGENT: $saviorName",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.normalGlow
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
                val directorName = nameMap[state.agentDirectorId] ?: "Director"
                val directorVoided = state.agentDirectorId == state.nullifiedVoterId

                Surface(
                    color = palette.timerSurface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, palette.timerAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HowToVote,
                            contentDescription = null,
                            tint = palette.timerAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "AGENT DIRECTOR: $directorName",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.timerAccent
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
                        Surface(
                            color = palette.impostorSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, palette.impostorAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (imp.id == state.ultimateKillerId) "${imp.name} (Agent Killer)" else imp.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.impostorGlow
                                )
                                val votesAgainst = state.roundVoteCounts[imp.id] ?: 0
                                Text(
                                    text = "$votesAgainst counted vote${if (votesAgainst == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.impostorGlow
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
                        val isSavior = state.saviorAgentEnabled && player.id == state.saviorAgentId
                        val isDirector = state.agentDirectorEnabled && player.id == state.agentDirectorId
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
                            if (isImp) add(if (player.id == state.ultimateKillerId) "AGENT KILLER" else "IMPOSTOR")
                            if (isSavior) add("SAVIOR")
                            if (isDirector) add("DIRECTOR")
                        }.joinToString(" • ")

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (roleTag.isNotEmpty()) "${player.name} ($roleTag)" else player.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = when {
                                        isImp -> palette.impostorGlow
                                        isSavior -> palette.normalGlow
                                        isDirector -> palette.timerAccent
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
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
                                        tint = palette.normalGlow,
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
                                        isProtected && state.saviorSavedTarget -> palette.normalGlow
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
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
