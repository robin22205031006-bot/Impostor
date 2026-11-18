package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundCuePlayer
import com.example.data.GameMode
import com.example.data.GameRepository
import com.example.data.GameRoom
import com.example.data.HostRoundSecret
import com.example.data.PlayerAssignment
import com.example.data.RoomStatus
import com.example.data.SecretRoundAction
import com.example.data.UiState
import com.example.data.WordBank
import com.example.data.extractFirestoreMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MultiplayerViewModel(
    private val repository: GameRepository,
    private val roomCode: String,
    val currentUserId: String
) : ViewModel() {

    val roomState: StateFlow<UiState<GameRoom?>> = repository.observeRoom(roomCode)
        .map<GameRoom?, UiState<GameRoom?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing room $roomCode", root)
            emit(UiState.Error(root.message ?: "Failed to sync room"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val assignmentState: StateFlow<UiState<PlayerAssignment?>> =
        repository.observeMyAssignment(roomCode, currentUserId)
            .map<PlayerAssignment?, UiState<PlayerAssignment?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing assignment", root)
                emit(UiState.Error(root.message ?: "Failed to load secret role"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val mySecretActionState: StateFlow<SecretRoundAction?> =
        repository.observeMySecretAction(roomCode, currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing secret action", firestoreError ?: error)
                emit(null)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = null
            )

    // Only attach hostSecrets listener when currentUserId is the room host
    val hostSecretState: StateFlow<HostRoundSecret?> = roomState
        .flatMapLatest { state ->
            val room = (state as? UiState.Success)?.data
            if (room != null && room.hostId == currentUserId && room.currentRound > 0L) {
                repository.observeHostRoundSecret(roomCode)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Error observing host secret", firestoreError ?: error)
                        emit(null)
                    }
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    // Synchronized 1-second ticker computed from room.turnStartedAt
    val remainingTurnSeconds: StateFlow<Int> = roomState
        .flatMapLatest { state ->
            val room = (state as? UiState.Success)?.data
            if (room == null) {
                flowOf(45)
            } else if (!room.timerEnabled || room.roomStatus() != RoomStatus.PLAYING || room.timerPaused) {
                flowOf(room.turnDurationSeconds.toInt())
            } else {
                flow {
                    var lastRemaining = room.turnDurationSeconds.toInt()
                    var playedExpiredCue = false
                    while (true) {
                        val startedMs = room.turnStartedAt?.toDate()?.time ?: System.currentTimeMillis()
                        val elapsedSec = ((System.currentTimeMillis() - startedMs) / 1000L).coerceAtLeast(0L)
                        val remaining = (room.turnDurationSeconds - elapsedSec).coerceIn(0L, room.turnDurationSeconds).toInt()
                        if (lastRemaining > 0 && remaining == 0 && !playedExpiredCue) {
                            playedExpiredCue = true
                            SoundCuePlayer.playTimerExpiredCue()
                        }
                        lastRemaining = remaining
                        emit(remaining)
                        delay(500L)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 45
        )

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val usedWords = mutableSetOf<String>()

    fun clearError() {
        _actionError.value = null
    }

    fun renameSelf(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            _actionError.value = "Name cannot be empty."
            return
        }
        runAction("Failed to rename player.") {
            repository.renamePlayerInRoom(roomCode, trimmed)
        }
    }

    fun updateSettings(
        gameMode: GameMode,
        impostorCount: Int,
        turnDurationSeconds: Int,
        selectedCategory: String,
        hintsEnabled: Boolean,
        categoryCluesEnabled: Boolean,
        timerEnabled: Boolean,
        impostorAbilityEnabled: Boolean,
        saviorAgentEnabled: Boolean,
        agentDirectorEnabled: Boolean,
        agentPredictionEnabled: Boolean
    ) {
        runAction("Failed to update room settings.") {
            repository.updateRoomSettings(
                roomCode = roomCode,
                gameMode = gameMode,
                impostorCount = impostorCount,
                turnDurationSeconds = turnDurationSeconds,
                selectedCategory = selectedCategory,
                hintsEnabled = hintsEnabled,
                categoryCluesEnabled = categoryCluesEnabled,
                timerEnabled = timerEnabled,
                impostorAbilityEnabled = impostorAbilityEnabled,
                saviorAgentEnabled = saviorAgentEnabled,
                agentDirectorEnabled = agentDirectorEnabled,
                agentPredictionEnabled = agentPredictionEnabled
            )
        }
    }

    fun selectKillerTarget(round: Long, targetUid: String?) {
        val current = mySecretActionState.value?.takeIf { it.round == round }
        val existingTarget = current?.killedVoterUid?.takeIf { it.isNotBlank() }
        val nextTarget = if (targetUid == null || targetUid == existingTarget) null else targetUid
        runAction("Failed to lock Ultimate Killer target.") {
            repository.submitSecretAction(
                roomCode = roomCode,
                round = round,
                killedVoterUid = nextTarget,
                protectedPlayerUid = current?.protectedPlayerUid?.takeIf { it.isNotBlank() },
                predictedImpostorUid = current?.predictedImpostorUid?.takeIf { it.isNotBlank() }
            )
        }
    }

    fun selectSaviorTarget(round: Long, targetUid: String?) {
        val current = mySecretActionState.value?.takeIf { it.round == round }
        val existingProtected = current?.protectedPlayerUid?.takeIf { it.isNotBlank() }
        val nextProtected = if (targetUid == null || targetUid == existingProtected) null else targetUid
        runAction("Failed to lock Savior protection shield.") {
            repository.submitSecretAction(
                roomCode = roomCode,
                round = round,
                killedVoterUid = current?.killedVoterUid?.takeIf { it.isNotBlank() },
                protectedPlayerUid = nextProtected,
                predictedImpostorUid = current?.predictedImpostorUid?.takeIf { it.isNotBlank() }
            )
        }
    }

    fun selectAgentPrediction(round: Long, suspectUid: String?) {
        val current = mySecretActionState.value?.takeIf { it.round == round }
        val existingPred = current?.predictedImpostorUid?.takeIf { it.isNotBlank() }
        val nextPred = if (suspectUid == null || suspectUid == existingPred) null else suspectUid
        runAction("Failed to submit impostor prediction.") {
            repository.submitSecretAction(
                roomCode = roomCode,
                round = round,
                killedVoterUid = current?.killedVoterUid?.takeIf { it.isNotBlank() },
                protectedPlayerUid = current?.protectedPlayerUid?.takeIf { it.isNotBlank() },
                predictedImpostorUid = nextPred
            )
        }
    }

    fun startRound(room: GameRoom) {
        if (WordBank.isCustomCategory(room.selectedCategory) && WordBank.customEntries.isEmpty()) {
            _actionError.value = "Add at least 1 custom word first, or choose another category."
            return
        }
        val picked = WordBank.pickRandomWord(room.selectedCategory, usedWords)
        usedWords.add(picked.word)
        runAction("Failed to start round.") {
            repository.startMultiplayerRound(room, picked)
        }
    }

    fun markRoleReady() {
        runAction("Failed to confirm ready state.") {
            repository.markRoleReady(roomCode)
        }
    }

    fun startDiscussionTurns() {
        runAction("Failed to start discussion turns.") {
            repository.startDiscussionTurns(roomCode)
        }
    }

    fun nextTurn(room: GameRoom) {
        runAction("Failed to advance turn.") {
            repository.advanceTurn(
                roomCode = roomCode,
                nextIndex = room.currentTurnIndex.toInt() + 1,
                totalPlayers = room.turnOrder.size
            )
        }
    }

    fun toggleTimerPause(room: GameRoom) {
        runAction("Failed to toggle timer.") {
            repository.toggleTimerPaused(roomCode, !room.timerPaused)
        }
    }

    fun startVoting() {
        runAction("Failed to start voting.") {
            repository.startVoting(roomCode)
        }
    }

    fun castVote(suspectUid: String) {
        runAction("Failed to submit vote.") {
            repository.castVote(roomCode, suspectUid)
        }
    }

    fun submitImpostorGuess(guess: String) {
        runAction("Failed to submit word guess.") {
            repository.submitImpostorGuess(roomCode, guess)
        }
    }

    fun revealResults(room: GameRoom, secret: HostRoundSecret) {
        runAction("Failed to reveal results.") {
            repository.revealResults(room, secret)
        }
    }

    fun returnToLobby() {
        runAction("Failed to return to lobby.") {
            repository.returnToLobby(roomCode)
        }
    }

    fun leaveRoom(room: GameRoom, onLeft: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            val res = repository.leaveRoom(room)
            _isBusy.value = false
            res.onSuccess { onLeft() }
                .onFailure { err ->
                    _actionError.value = extractFirestoreMessage(err, "Failed to leave room.")
                }
        }
    }

    private fun runAction(fallbackError: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            val res = block()
            _isBusy.value = false
            res.onFailure { err ->
                _actionError.value = extractFirestoreMessage(err, fallbackError)
            }
        }
    }

    private companion object {
        const val TAG = "MultiplayerVM"
    }
}
