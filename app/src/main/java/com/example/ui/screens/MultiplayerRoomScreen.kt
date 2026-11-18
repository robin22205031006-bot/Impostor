package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.SoundCuePlayer
import com.example.data.GameMode
import com.example.data.GameRoom
import com.example.data.HostRoundSecret
import com.example.data.PlayerAssignment
import com.example.data.PlayerRole
import com.example.data.RoomStatus
import com.example.data.SecretRoundAction
import com.example.data.UiState
import com.example.data.WordBank
import com.example.ui.components.AgentDirectorBadge
import com.example.ui.components.AgentPredictionCard
import com.example.ui.components.ImpostorAnimatedBackdrop
import com.example.ui.components.OtherImpostorAllyNote
import com.example.ui.components.RoleWordCard
import com.example.ui.components.SaviorAbilityCard
import com.example.ui.components.TurnTimerBar
import com.example.ui.components.UltimateKillerAbilityCard
import com.example.ui.components.WordCategoryBox
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalImpostorPalette
import com.example.viewmodel.MultiplayerViewModel

@Composable
fun MultiplayerRoomScreen(
    viewModel: MultiplayerViewModel,
    onExitRoom: () -> Unit
) {
    val roomState by viewModel.roomState.collectAsStateWithLifecycle()
    val assignmentState by viewModel.assignmentState.collectAsStateWithLifecycle()
    val mySecretAction by viewModel.mySecretActionState.collectAsStateWithLifecycle()
    val hostSecret by viewModel.hostSecretState.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingTurnSeconds.collectAsStateWithLifecycle()
    val soundEnabled by SoundCuePlayer.soundEnabled.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    val palette = LocalImpostorPalette.current
    val clipboard = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameDraft by remember { mutableStateOf("") }

    BackHandler {
        onExitRoom()
    }

    ImpostorAnimatedBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
        ) {
            when (val rState = roomState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = rState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.impostorGlow,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onExitRoom) {
                            Text("Back to Hub")
                        }
                    }
                }

                is UiState.Success -> {
                    val room = rState.data
                    if (room == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "This room has been closed by the host.",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onExitRoom) {
                                Text("Back to Hub")
                            }
                        }
                    } else {
                        val isHost = room.hostId == viewModel.currentUserId
                        val assignment = (assignmentState as? UiState.Success)?.data

                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onExitRoom,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("room_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Hub"
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = room.roomCode,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = palette.cyanAccent
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(room.roomCode))
                                                copiedNotice = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                contentDescription = "Copy room code",
                                                tint = palette.cyanAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (copiedNotice) "Code copied!" else "${room.resolvedGameMode().title} • Round ${room.currentRound}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (copiedNotice) palette.normalGlow else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = SoundCuePlayer::toggleSound,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("room_sound_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                        contentDescription = if (soundEnabled) "Mute sound cues" else "Enable sound cues",
                                        tint = if (soundEnabled) palette.cyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.leaveRoom(room, onExitRoom) },
                                    enabled = !isBusy
                                ) {
                                    Text(
                                        text = if (isHost) "Close" else "Leave",
                                        color = palette.impostorGlow
                                    )
                                }
                            }
                        }

                        if (actionError != null) {
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
                                        text = actionError ?: "",
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
                            targetState = room.roomStatus(),
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(260)) +
                                    slideInVertically(animationSpec = tween(260)) { it / 16 } +
                                    scaleIn(initialScale = 0.98f, animationSpec = tween(260)))
                                    .togetherWith(fadeOut(animationSpec = tween(180)))
                                    .using(SizeTransform(clip = false))
                            },
                            label = "room_status_transition",
                            modifier = Modifier.weight(1f)
                        ) { status ->
                            when (status) {
                                RoomStatus.LOBBY -> MultiplayerLobbyContent(
                                room = room,
                                currentUserId = viewModel.currentUserId,
                                isHost = isHost,
                                isBusy = isBusy,
                                onRenameSelfClick = {
                                    renameDraft = room.playerNames[viewModel.currentUserId] ?: ""
                                    showRenameDialog = true
                                },
                                onSelectMode = { mode ->
                                    viewModel.updateSettings(
                                        gameMode = mode,
                                        impostorCount = room.impostorCount.toInt(),
                                        turnDurationSeconds = mode.defaultTurnSeconds,
                                        selectedCategory = room.selectedCategory,
                                        hintsEnabled = room.hintsEnabled,
                                        categoryCluesEnabled = room.categoryCluesEnabled,
                                        timerEnabled = room.timerEnabled,
                                        impostorAbilityEnabled = room.impostorAbilityEnabled,
                                        saviorAgentEnabled = room.saviorAgentEnabled,
                                        agentDirectorEnabled = room.agentDirectorEnabled,
                                        agentPredictionEnabled = room.agentPredictionEnabled
                                    )
                                },
                                onSetImpostors = { count ->
                                    viewModel.updateSettings(
                                        gameMode = room.resolvedGameMode(),
                                        impostorCount = count,
                                        turnDurationSeconds = room.turnDurationSeconds.toInt(),
                                        selectedCategory = room.selectedCategory,
                                        hintsEnabled = room.hintsEnabled,
                                        categoryCluesEnabled = room.categoryCluesEnabled,
                                        timerEnabled = room.timerEnabled,
                                        impostorAbilityEnabled = room.impostorAbilityEnabled,
                                        saviorAgentEnabled = room.saviorAgentEnabled,
                                        agentDirectorEnabled = room.agentDirectorEnabled,
                                        agentPredictionEnabled = room.agentPredictionEnabled
                                    )
                                },
                                onSetDuration = { sec ->
                                    viewModel.updateSettings(
                                        gameMode = room.resolvedGameMode(),
                                        impostorCount = room.impostorCount.toInt(),
                                        turnDurationSeconds = sec,
                                        selectedCategory = room.selectedCategory,
                                        hintsEnabled = room.hintsEnabled,
                                        categoryCluesEnabled = room.categoryCluesEnabled,
                                        timerEnabled = room.timerEnabled,
                                        impostorAbilityEnabled = room.impostorAbilityEnabled,
                                        saviorAgentEnabled = room.saviorAgentEnabled,
                                        agentDirectorEnabled = room.agentDirectorEnabled,
                                        agentPredictionEnabled = room.agentPredictionEnabled
                                    )
                                },
                                onSelectCategory = { cat ->
                                    viewModel.updateSettings(
                                        gameMode = room.resolvedGameMode(),
                                        impostorCount = room.impostorCount.toInt(),
                                        turnDurationSeconds = room.turnDurationSeconds.toInt(),
                                        selectedCategory = cat,
                                        hintsEnabled = room.hintsEnabled,
                                        categoryCluesEnabled = room.categoryCluesEnabled,
                                        timerEnabled = room.timerEnabled,
                                        impostorAbilityEnabled = room.impostorAbilityEnabled,
                                        saviorAgentEnabled = room.saviorAgentEnabled,
                                        agentDirectorEnabled = room.agentDirectorEnabled,
                                        agentPredictionEnabled = room.agentPredictionEnabled
                                    )
                                },
                                onToggleHints = { enabled ->
                                    viewModel.updateSettings(
                                        gameMode = room.resolvedGameMode(),
                                        impostorCount = room.impostorCount.toInt(),
                                        turnDurationSeconds = room.turnDurationSeconds.toInt(),
                                        selectedCategory = room.selectedCategory,
                                        hintsEnabled = enabled,
                                        categoryCluesEnabled = room.categoryCluesEnabled,
                                        timerEnabled = room.timerEnabled,
                                        impostorAbilityEnabled = room.impostorAbilityEnabled,
                                        saviorAgentEnabled = room.saviorAgentEnabled,
                                        agentDirectorEnabled = room.agentDirectorEnabled,
                                        agentPredictionEnabled = room.agentPredictionEnabled
                                    )
                                },
                                onToggleCategoryClues = { enabled ->
                                    viewModel.updateSettings(
                                        gameMode = room.resolvedGameMode(),
                                        impostorCount = room.impostorCount.toInt(),
                                        turnDurationSeconds = room.turnDurationSeconds.toInt(),
                                        selectedCategory = room.selectedCategory,
                                        hintsEnabled = room.hintsEnabled,
                                        categoryCluesEnabled = enabled,
                                        timerEnabled = room.timerEnabled,
                                        impostorAbilityEnabled = room.impostorAbilityEnabled,
                                        saviorAgentEnabled = room.saviorAgentEnabled,
                                        agentDirectorEnabled = room.agentDirectorEnabled,
                                        agentPredictionEnabled = room.agentPredictionEnabled
                                    )
                                },
                                onToggleTimer = { enabled ->
                                    viewModel.updateSettings(
                                        gameMode = room.resolvedGameMode(),
                                        impostorCount = room.impostorCount.toInt(),
                                        turnDurationSeconds = room.turnDurationSeconds.toInt(),
                                        selectedCategory = room.selectedCategory,
                                        hintsEnabled = room.hintsEnabled,
                                        categoryCluesEnabled = room.categoryCluesEnabled,
                                        timerEnabled = enabled,
                                        impostorAbilityEnabled = room.impostorAbilityEnabled,
                                        saviorAgentEnabled = room.saviorAgentEnabled,
                                        agentDirectorEnabled = room.agentDirectorEnabled,
                                        agentPredictionEnabled = room.agentPredictionEnabled
                                    )
                                },
                                onToggleImpostorAbility = { enabled ->
                                    viewModel.updateSettings(
                                        gameMode = room.resolvedGameMode(),
                                        impostorCount = room.impostorCount.toInt(),
                                        turnDurationSeconds = room.turnDurationSeconds.toInt(),
                                        selectedCategory = room.selectedCategory,
                                        hintsEnabled = room.hintsEnabled,
                                        categoryCluesEnabled = room.categoryCluesEnabled,
                                        timerEnabled = room.timerEnabled,
                                        impostorAbilityEnabled = enabled,
                                        saviorAgentEnabled = room.saviorAgentEnabled,
                                        agentDirectorEnabled = room.agentDirectorEnabled,
                                        agentPredictionEnabled = room.agentPredictionEnabled
                                    )
                                },
                                onToggleSaviorAgent = { enabled ->
                                    viewModel.updateSettings(
                                        gameMode = room.resolvedGameMode(),
                                        impostorCount = room.impostorCount.toInt(),
                                        turnDurationSeconds = room.turnDurationSeconds.toInt(),
                                        selectedCategory = room.selectedCategory,
                                        hintsEnabled = room.hintsEnabled,
                                        categoryCluesEnabled = room.categoryCluesEnabled,
                                        timerEnabled = room.timerEnabled,
                                        impostorAbilityEnabled = room.impostorAbilityEnabled,
                                        saviorAgentEnabled = enabled,
                                        agentDirectorEnabled = room.agentDirectorEnabled,
                                        agentPredictionEnabled = room.agentPredictionEnabled
                                    )
                                },
                                onToggleAgentDirector = { enabled ->
                                    viewModel.updateSettings(
                                        gameMode = room.resolvedGameMode(),
                                        impostorCount = room.impostorCount.toInt(),
                                        turnDurationSeconds = room.turnDurationSeconds.toInt(),
                                        selectedCategory = room.selectedCategory,
                                        hintsEnabled = room.hintsEnabled,
                                        categoryCluesEnabled = room.categoryCluesEnabled,
                                        timerEnabled = room.timerEnabled,
                                        impostorAbilityEnabled = room.impostorAbilityEnabled,
                                        saviorAgentEnabled = room.saviorAgentEnabled,
                                        agentDirectorEnabled = enabled,
                                        agentPredictionEnabled = room.agentPredictionEnabled
                                    )
                                },
                                onToggleAgentPrediction = { enabled ->
                                    viewModel.updateSettings(
                                        gameMode = room.resolvedGameMode(),
                                        impostorCount = room.impostorCount.toInt(),
                                        turnDurationSeconds = room.turnDurationSeconds.toInt(),
                                        selectedCategory = room.selectedCategory,
                                        hintsEnabled = room.hintsEnabled,
                                        categoryCluesEnabled = room.categoryCluesEnabled,
                                        timerEnabled = room.timerEnabled,
                                        impostorAbilityEnabled = room.impostorAbilityEnabled,
                                        saviorAgentEnabled = room.saviorAgentEnabled,
                                        agentDirectorEnabled = room.agentDirectorEnabled,
                                        agentPredictionEnabled = enabled
                                    )
                                },
                                onStartRound = { viewModel.startRound(room) }
                            )

                            RoomStatus.REVEAL -> MultiplayerRevealContent(
                                room = room,
                                assignment = assignment,
                                mySecretAction = mySecretAction,
                                currentUserId = viewModel.currentUserId,
                                isHost = isHost,
                                isBusy = isBusy,
                                onSelectKillTarget = { targetUid -> viewModel.selectKillerTarget(room.currentRound, targetUid) },
                                onSelectProtectedTarget = { targetUid -> viewModel.selectSaviorTarget(room.currentRound, targetUid) },
                                onSelectPrediction = { suspectUid -> viewModel.selectAgentPrediction(room.currentRound, suspectUid) },
                                onMarkReady = viewModel::markRoleReady,
                                onStartTurns = viewModel::startDiscussionTurns
                            )

                            RoomStatus.PLAYING -> MultiplayerPlayingContent(
                                room = room,
                                assignment = assignment,
                                mySecretAction = mySecretAction,
                                remainingSeconds = remainingSeconds,
                                currentUserId = viewModel.currentUserId,
                                isHost = isHost,
                                isBusy = isBusy,
                                onSelectKillTarget = { targetUid -> viewModel.selectKillerTarget(room.currentRound, targetUid) },
                                onSelectProtectedTarget = { targetUid -> viewModel.selectSaviorTarget(room.currentRound, targetUid) },
                                onSelectPrediction = { suspectUid -> viewModel.selectAgentPrediction(room.currentRound, suspectUid) },
                                onTogglePause = { viewModel.toggleTimerPause(room) },
                                onNextTurn = { viewModel.nextTurn(room) },
                                onSubmitGuess = viewModel::submitImpostorGuess,
                                onStartVoting = viewModel::startVoting
                            )

                            RoomStatus.VOTING -> MultiplayerVotingContent(
                                room = room,
                                assignment = assignment,
                                hostSecret = hostSecret,
                                currentUserId = viewModel.currentUserId,
                                isHost = isHost,
                                isBusy = isBusy,
                                onCastVote = viewModel::castVote,
                                onSubmitGuess = viewModel::submitImpostorGuess,
                                onRevealResults = {
                                    if (hostSecret != null) {
                                        viewModel.revealResults(room, hostSecret!!)
                                    }
                                }
                            )

                                RoomStatus.RESULTS -> MultiplayerResultsContent(
                                    room = room,
                                    isHost = isHost,
                                    isBusy = isBusy,
                                    onNextRound = { viewModel.startRound(room) },
                                    onReturnLobby = viewModel::returnToLobby
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Your Player Name") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it.take(24) },
                    label = { Text("Player Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameSelf(renameDraft)
                        showRenameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MultiplayerLobbyContent(
    room: GameRoom,
    currentUserId: String,
    isHost: Boolean,
    isBusy: Boolean,
    onRenameSelfClick: () -> Unit,
    onSelectMode: (GameMode) -> Unit,
    onSetImpostors: (Int) -> Unit,
    onSetDuration: (Int) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleHints: (Boolean) -> Unit,
    onToggleCategoryClues: (Boolean) -> Unit,
    onToggleTimer: (Boolean) -> Unit,
    onToggleImpostorAbility: (Boolean) -> Unit,
    onToggleSaviorAgent: (Boolean) -> Unit,
    onToggleAgentDirector: (Boolean) -> Unit,
    onToggleAgentPrediction: (Boolean) -> Unit,
    onStartRound: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val customWords by WordBank.customWordsFlow.collectAsStateWithLifecycle()
    val customCategoryName by WordBank.customCategoryNameFlow.collectAsStateWithLifecycle()
    val maxImpostors = (room.memberUids.size - 1).coerceAtLeast(1)
    val currentMode = room.resolvedGameMode()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                color = palette.cyanSurface.copy(alpha = 0.65f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SHARE ROOM CODE WITH FRIENDS",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.cyanAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = room.roomCode,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Players Roster with Rename Self
        item {
            Text(
                text = "PLAYERS IN LOBBY (${room.memberUids.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(room.memberUids, key = { it }) { uid ->
            val name = room.playerNames[uid] ?: "Agent"
            val score = room.playerScores[uid] ?: 0L
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (uid == currentUserId) palette.cyanAccent else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uid == room.hostId) {
                                Text(
                                    text = "HOST",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.timerAccent
                                )
                            }
                        }
                        if (score != 0L) {
                            Text(
                                text = "$score pts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (score < 0L) palette.impostorGlow else palette.timerAccent
                            )
                        }
                    }

                    if (uid == currentUserId) {
                        IconButton(
                            onClick = onRenameSelfClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("room_rename_self_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Rename yourself",
                                tint = palette.cyanAccent
                            )
                        }
                    }
                }
            }
        }

        // Host Settings
        item {
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
                        text = if (isHost) "HOST GAME SETTINGS" else "ROOM SETTINGS (HOST CONTROLLED)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GameMode.entries.forEach { mode ->
                            FilterChip(
                                selected = currentMode == mode,
                                onClick = { if (isHost) onSelectMode(mode) },
                                enabled = isHost,
                                label = { Text(mode.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.cyanSurface,
                                    selectedLabelColor = palette.cyanAccent
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Impostors: ${room.impostorCount}",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.impostorGlow
                        )
                        if (isHost) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onSetImpostors((room.impostorCount.toInt() - 1).coerceAtLeast(1)) },
                                    enabled = room.impostorCount > 1L,
                                    modifier = Modifier.size(48.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("-")
                                }
                                OutlinedButton(
                                    onClick = { onSetImpostors((room.impostorCount.toInt() + 1).coerceAtMost(maxImpostors)) },
                                    enabled = room.impostorCount < maxImpostors.toLong(),
                                    modifier = Modifier.size(48.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("+")
                                }
                            }
                        }
                    }

                    Text(
                        text = "Turn Timer: ${room.turnDurationSeconds}s",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.timerAccent
                    )
                    if (isHost) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(10, 20, 30, 45, 60, 90, 120).forEach { sec ->
                                FilterChip(
                                    selected = room.turnDurationSeconds.toInt() == sec,
                                    onClick = { onSetDuration(sec) },
                                    label = { Text("${sec}s") }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    RoomHostSwitchRow(
                        label = "Word Hints",
                        checked = room.hintsEnabled,
                        enabled = isHost,
                        onCheckedChange = onToggleHints
                    )
                    RoomHostSwitchRow(
                        label = "Category & Genre Labels",
                        checked = room.categoryCluesEnabled,
                        enabled = isHost,
                        onCheckedChange = onToggleCategoryClues
                    )
                    RoomHostSwitchRow(
                        label = "Turn Timer",
                        checked = room.timerEnabled,
                        enabled = isHost,
                        onCheckedChange = onToggleTimer
                    )
                    RoomHostSwitchRow(
                        label = "Impostor Ability (Agent Killer)",
                        checked = room.impostorAbilityEnabled,
                        enabled = isHost,
                        onCheckedChange = onToggleImpostorAbility
                    )
                    RoomHostSwitchRow(
                        label = "Savior Agent (Protection Shield)",
                        checked = room.saviorAgentEnabled,
                        enabled = isHost,
                        onCheckedChange = onToggleSaviorAgent
                    )
                    RoomHostSwitchRow(
                        label = "Agent Director (2x Vote Power)",
                        checked = room.agentDirectorEnabled,
                        enabled = isHost,
                        onCheckedChange = onToggleAgentDirector
                    )
                    RoomHostSwitchRow(
                        label = "Agent Prediction (+3/+1 or -2)",
                        checked = room.agentPredictionEnabled,
                        enabled = isHost,
                        onCheckedChange = onToggleAgentPrediction
                    )
                }
            }
        }

        // Unified Word Category + Named Custom Words Box
        item {
            WordCategoryBox(
                selectedCategory = room.selectedCategory,
                customCategoryName = customCategoryName,
                customWords = customWords,
                canEdit = isHost,
                onSelectCategory = onSelectCategory
            )
        }

        item {
            if (isHost) {
                Button(
                    onClick = onStartRound,
                    enabled = !isBusy && room.memberUids.size >= 2,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.normalAccent,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("room_start_round_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = if (room.memberUids.size < 2) "Need 2+ Players to Start" else "Start Round",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Waiting for the host to start the round...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiplayerRevealContent(
    room: GameRoom,
    assignment: PlayerAssignment?,
    mySecretAction: SecretRoundAction?,
    currentUserId: String,
    isHost: Boolean,
    isBusy: Boolean,
    onSelectKillTarget: (String?) -> Unit,
    onSelectProtectedTarget: (String?) -> Unit,
    onSelectPrediction: (String?) -> Unit,
    onMarkReady: () -> Unit,
    onStartTurns: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val isReady = currentUserId in room.readyUids
    val otherPlayers = remember(room.memberUids, room.playerNames, currentUserId) {
        room.memberUids.filter { it != currentUserId }.map { it to (room.playerNames[it] ?: "Agent") }
    }
    val activeAction = mySecretAction?.takeIf { it.round == room.currentRound }

    LaunchedEffect(room.currentRound, assignment?.uid, assignment?.role) {
        if (assignment != null && assignment.round == room.currentRound) {
            SoundCuePlayer.playRoleRevealCue(isImpostor = assignment.playerRole() == PlayerRole.IMPOSTOR)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TurnTimerBar(
                activePlayerName = "Role Briefing (${room.readyUids.size}/${room.memberUids.size} Ready)",
                turnIndex = 0,
                totalTurns = room.memberUids.size,
                remainingSeconds = room.turnDurationSeconds.toInt(),
                totalSeconds = room.turnDurationSeconds.toInt(),
                isPaused = true,
                showControls = false
            )
        }

        item {
            if (assignment == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    RoleWordCard(
                        role = assignment.playerRole(),
                        category = assignment.category,
                        genre = assignment.genre,
                        secretWord = assignment.secretWord,
                        hint = assignment.hint,
                        showCategoryAndGenre = room.categoryCluesEnabled,
                        showHint = room.hintsEnabled
                    )
                    if (room.impostorAbilityEnabled && assignment.playerRole() == PlayerRole.IMPOSTOR) {
                        if (assignment.ultimateKiller) {
                            UltimateKillerAbilityCard(
                                candidates = otherPlayers,
                                selectedTargetId = activeAction?.killedVoterUid?.takeIf { it.isNotBlank() },
                                onSelectTarget = onSelectKillTarget
                            )
                        } else {
                            OtherImpostorAllyNote()
                        }
                    }
                    if (room.agentDirectorEnabled && assignment.playerRole() == PlayerRole.NORMAL && assignment.director) {
                        AgentDirectorBadge()
                    }
                    if (room.saviorAgentEnabled && assignment.playerRole() == PlayerRole.NORMAL && assignment.savior) {
                        SaviorAbilityCard(
                            candidates = otherPlayers,
                            selectedProtectedId = activeAction?.protectedPlayerUid?.takeIf { it.isNotBlank() },
                            onSelectProtected = onSelectProtectedTarget
                        )
                    }
                    if (room.agentPredictionEnabled && assignment.playerRole() == PlayerRole.NORMAL) {
                        AgentPredictionCard(
                            candidates = otherPlayers,
                            selectedSuspectId = activeAction?.predictedImpostorUid?.takeIf { it.isNotBlank() },
                            onSelectSuspect = onSelectPrediction
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onMarkReady,
                enabled = !isBusy && !isReady,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isReady) MaterialTheme.colorScheme.surfaceContainerHigh else palette.cyanAccent,
                    contentColor = if (isReady) palette.normalGlow else MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isReady) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Ready! Waiting for others (${room.readyUids.size}/${room.memberUids.size})")
                } else {
                    Text("I've Memorized My Role")
                }
            }
        }

        if (isHost) {
            item {
                OutlinedButton(
                    onClick = onStartTurns,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, palette.normalAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text("Start Timed Turns Now", color = palette.normalGlow)
                }
            }
        }
    }
}

@Composable
private fun MultiplayerPlayingContent(
    room: GameRoom,
    assignment: PlayerAssignment?,
    mySecretAction: SecretRoundAction?,
    remainingSeconds: Int,
    currentUserId: String,
    isHost: Boolean,
    isBusy: Boolean,
    onSelectKillTarget: (String?) -> Unit,
    onSelectProtectedTarget: (String?) -> Unit,
    onSelectPrediction: (String?) -> Unit,
    onTogglePause: () -> Unit,
    onNextTurn: () -> Unit,
    onSubmitGuess: (String) -> Unit,
    onStartVoting: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val activeUid = room.turnOrder.getOrNull(room.currentTurnIndex.toInt())
    val activeName = room.playerNames[activeUid] ?: "Agent"
    var guessDraft by remember(room.currentRound) { mutableStateOf(room.impostorGuess.orEmpty()) }
    val otherPlayers = remember(room.memberUids, room.playerNames, currentUserId) {
        room.memberUids.filter { it != currentUserId }.map { it to (room.playerNames[it] ?: "Agent") }
    }
    val activeAction = mySecretAction?.takeIf { it.round == room.currentRound }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (room.timerEnabled) {
                TurnTimerBar(
                    activePlayerName = if (activeUid == currentUserId) "$activeName (Your Turn!)" else activeName,
                    turnIndex = room.currentTurnIndex.toInt(),
                    totalTurns = room.turnOrder.size,
                    remainingSeconds = remainingSeconds,
                    totalSeconds = room.turnDurationSeconds.toInt(),
                    isPaused = room.timerPaused,
                    showControls = isHost,
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
                                text = if (activeUid == currentUserId) "$activeName (Your Turn!)" else activeName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isHost) {
                            Button(onClick = onNextTurn) {
                                Text("Next")
                            }
                        }
                    }
                }
            }
        }

        if (assignment != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    RoleWordCard(
                        role = assignment.playerRole(),
                        category = assignment.category,
                        genre = assignment.genre,
                        secretWord = assignment.secretWord,
                        hint = assignment.hint,
                        showCategoryAndGenre = room.categoryCluesEnabled,
                        showHint = room.hintsEnabled
                    )
                    if (room.impostorAbilityEnabled && assignment.playerRole() == PlayerRole.IMPOSTOR) {
                        if (assignment.ultimateKiller) {
                            UltimateKillerAbilityCard(
                                candidates = otherPlayers,
                                selectedTargetId = activeAction?.killedVoterUid?.takeIf { it.isNotBlank() },
                                onSelectTarget = onSelectKillTarget
                            )
                        } else {
                            OtherImpostorAllyNote()
                        }
                    }
                    if (room.agentDirectorEnabled && assignment.playerRole() == PlayerRole.NORMAL && assignment.director) {
                        AgentDirectorBadge()
                    }
                    if (room.saviorAgentEnabled && assignment.playerRole() == PlayerRole.NORMAL && assignment.savior) {
                        SaviorAbilityCard(
                            candidates = otherPlayers,
                            selectedProtectedId = activeAction?.protectedPlayerUid?.takeIf { it.isNotBlank() },
                            onSelectProtected = onSelectProtectedTarget
                        )
                    }
                    if (room.agentPredictionEnabled && assignment.playerRole() == PlayerRole.NORMAL) {
                        AgentPredictionCard(
                            candidates = otherPlayers,
                            selectedSuspectId = activeAction?.predictedImpostorUid?.takeIf { it.isNotBlank() },
                            onSelectSuspect = onSelectPrediction
                        )
                    }
                }
            }
        }

        // Impostor Guess Box (visible to impostor so they can blend in & guess the term)
        if (assignment?.playerRole() == PlayerRole.IMPOSTOR) {
            item {
                Surface(
                    color = palette.impostorSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, palette.impostorAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "BLEND IN & GUESS THE SECRET WORD",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.impostorGlow
                        )
                        OutlinedTextField(
                            value = guessDraft,
                            onValueChange = { guessDraft = it.take(80) },
                            label = { Text("Secret Word Guess") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { onSubmitGuess(guessDraft) },
                            enabled = !isBusy && guessDraft.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.impostorAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lock In Word Guess")
                        }
                    }
                }
            }
        }

        if (isHost) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onNextTurn,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Next Turn")
                    }
                    Button(
                        onClick = onStartVoting,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.timerAccent,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Start Voting")
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiplayerVotingContent(
    room: GameRoom,
    assignment: PlayerAssignment?,
    hostSecret: HostRoundSecret?,
    currentUserId: String,
    isHost: Boolean,
    isBusy: Boolean,
    onCastVote: (String) -> Unit,
    onSubmitGuess: (String) -> Unit,
    onRevealResults: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val myVote = room.votes[currentUserId]
    var guessDraft by remember(room.currentRound) { mutableStateOf(room.impostorGuess.orEmpty()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "CAST YOUR VOTE",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${room.votes.size} of ${room.memberUids.size} players voted",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.timerAccent
            )
        }

        items(room.memberUids, key = { it }) { uid ->
            val name = room.playerNames[uid] ?: "Agent"
            val selected = myVote == uid
            Surface(
                onClick = { onCastVote(uid) },
                color = if (selected) palette.impostorSurface else MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) palette.impostorGlow else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (uid == currentUserId) "$name (You)" else name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) palette.impostorGlow else MaterialTheme.colorScheme.onSurface
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Voted",
                            tint = palette.impostorGlow
                        )
                    }
                }
            }
        }

        if (assignment?.playerRole() == PlayerRole.IMPOSTOR) {
            item {
                Surface(
                    color = palette.impostorSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, palette.impostorAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "LAST-CHANCE IMPOSTOR GUESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.impostorGlow
                        )
                        OutlinedTextField(
                            value = guessDraft,
                            onValueChange = { guessDraft = it.take(80) },
                            label = { Text("Secret Word Guess") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { onSubmitGuess(guessDraft) },
                            enabled = !isBusy && guessDraft.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.impostorAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Word Guess")
                        }
                    }
                }
            }
        }

        if (isHost) {
            item {
                Button(
                    onClick = onRevealResults,
                    enabled = !isBusy && hostSecret != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.impostorAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Reveal Truth & Tally Scores", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun MultiplayerResultsContent(
    room: GameRoom,
    isHost: Boolean,
    isBusy: Boolean,
    onNextRound: () -> Unit,
    onReturnLobby: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    val impostorSet = room.revealedImpostorUids.toSet()
    val nullifiedUid = if (room.impostorAbilityEnabled) room.revealedNullifiedVoterUid?.takeIf { it.isNotBlank() } else null
    val protectedUid = if (room.saviorAgentEnabled) room.revealedProtectedUid?.takeIf { it.isNotBlank() } else null
    val saviorUid = if (room.saviorAgentEnabled) room.revealedSaviorUid?.takeIf { it.isNotBlank() } else null
    val directorUid = if (room.agentDirectorEnabled) room.revealedDirectorUid?.takeIf { it.isNotBlank() } else null
    val saviorSaved = room.revealedSaviorSaved == true

    val countedVotes = if (nullifiedUid != null) {
        room.votes.filterKeys { it != nullifiedUid }
    } else {
        room.votes
    }
    val voteCounts = buildMap<String, Int> {
        countedVotes.forEach { (voterUid, suspectUid) ->
            val weight = if (voterUid == directorUid) 2 else 1
            put(suspectUid, (get(suspectUid) ?: 0) + weight)
        }
    }
    val topCount = voteCounts.values.maxOrNull() ?: 0
    val caught = topCount > 0 && voteCounts.filterValues { it == topCount }.keys.any { it in impostorSet }
    val guessWon = room.impostorGuessCorrect == true
    val crewWon = caught && !guessWon

    LaunchedEffect(room.currentRound, crewWon) {
        SoundCuePlayer.playVotingResultsCue(crewWon)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val accent = if (crewWon) palette.normalAccent else palette.impostorAccent
            val bg = if (crewWon) palette.normalSurface else palette.impostorSurface
            Surface(
                color = bg,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (crewWon) "AGENTS WIN!" else "IMPOSTORS WIN!",
                        style = MaterialTheme.typography.displayMedium,
                        color = accent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            guessWon -> "Impostor guessed \"${room.impostorGuess}\" and stole the round!"
                            crewWon -> "The impostor was exposed!"
                            else -> "Impostors escaped detection!"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            RoleWordCard(
                role = PlayerRole.NORMAL,
                category = room.category,
                genre = room.genre,
                secretWord = room.revealedWord.orEmpty(),
                hint = room.hint,
                showCategoryAndGenre = room.categoryCluesEnabled,
                showHint = room.hintsEnabled
            )
        }

        if (room.impostorAbilityEnabled && !room.revealedKillerUid.isNullOrBlank()) {
            item {
                val killerName = room.playerNames[room.revealedKillerUid] ?: "Impostor"
                val attackedUid = if (saviorSaved) protectedUid else nullifiedUid
                val attackedName = attackedUid?.let { room.playerNames[it] ?: "Agent" }
                val attackedVotedImpostor = attackedUid?.let { room.votes[it] } in impostorSet

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
                                    attackedName == null -> "$killerName chose not to silence anyone's vote."
                                    saviorSaved -> "$killerName attacked $attackedName — BLOCKED by Savior Shield! Vote still counted!"
                                    attackedVotedImpostor -> "$killerName silenced $attackedName — their vote for an impostor was VOIDED!"
                                    else -> "$killerName silenced $attackedName's vote."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        if (room.saviorAgentEnabled && saviorUid != null) {
            item {
                val saviorName = room.playerNames[saviorUid] ?: "Savior"
                val protectedName = protectedUid?.let { room.playerNames[it] ?: "Agent" }

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
                                    saviorSaved -> "$saviorName shielded $protectedName and blocked the Agent Killer! (+1 Savior bonus)"
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

        if (room.agentDirectorEnabled && directorUid != null) {
            item {
                val directorName = room.playerNames[directorUid] ?: "Director"
                val directorVoided = directorUid == nullifiedUid

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
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "IMPOSTOR REVEAL, VOTES & SCOREBOARD",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    room.memberUids.sortedByDescending { room.playerScores[it] ?: 0L }.forEach { uid ->
                        val name = room.playerNames[uid] ?: "Agent"
                        val score = room.playerScores[uid] ?: 0L
                        val delta = room.revealedRoundDeltas[uid] ?: 0L
                        val deltaText = if (delta >= 0L) "+$delta" else "$delta"
                        val deltaColor = when {
                            delta > 0L -> palette.normalGlow
                            delta < 0L -> palette.impostorGlow
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val isImp = uid in impostorSet
                        val isKiller = uid == room.revealedKillerUid
                        val isSavior = uid == saviorUid
                        val isDirector = uid == directorUid
                        val isVoided = uid == nullifiedUid
                        val isProtected = uid == protectedUid
                        val votedForUid = room.votes[uid]
                        val votedForName = votedForUid?.let { room.playerNames[it] ?: "Agent" } ?: "None"
                        val predictedUid = room.revealedPredictions[uid]?.takeIf { it.isNotBlank() }
                        val predictedName = predictedUid?.let { room.playerNames[it] ?: "Agent" }

                        val roleTag = buildList {
                            if (isImp) add(if (isKiller) "AGENT KILLER" else "IMPOSTOR")
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
                                    text = if (roleTag.isNotEmpty()) "$name ($roleTag)" else name,
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
                                        text = "$score pts",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (score < 0L) palette.impostorGlow else palette.timerAccent
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
                                        if (isDirector && !isVoided && votedForUid != null) append(" [2x]")
                                        if (isProtected) {
                                            append(if (saviorSaved) " [SAVED]" else " [SHIELDED]")
                                        }
                                        if (isVoided) append(" [VOIDED]")
                                        if (!isImp && room.agentPredictionEnabled) {
                                            if (predictedName != null) {
                                                val hit = predictedUid in impostorSet
                                                append(" • Predicted: $predictedName (${if (hit) "+1" else "-2"})")
                                            } else {
                                                append(" • Prediction: Skipped")
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        isVoided -> palette.impostorGlow
                                        isProtected && saviorSaved -> palette.normalGlow
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isHost) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReturnLobby,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text("Lobby")
                    }
                    Button(
                        onClick = onNextRound,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.normalAccent,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Next Round")
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomHostSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalImpostorPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                checkedTrackColor = palette.normalAccent
            )
        )
    }
}
