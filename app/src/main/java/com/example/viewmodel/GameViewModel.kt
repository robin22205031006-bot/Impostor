package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundCuePlayer
import com.example.data.CryptoRandom
import com.example.data.GamePhase
import com.example.data.GameState
import com.example.data.LocalPlayer
import com.example.data.PlayerRole
import com.example.data.TurnStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var timerJob: Job? = null

    init {
        assignRoles()
    }

    fun setImpostorCount(count: Int) {
        _gameState.update { curr ->
            val maxImpostors = (curr.players.size - 1).coerceAtLeast(1)
            val clamped = count.coerceIn(1, maxImpostors)
            curr.copy(
                impostorCount = clamped,
                playerRoles = buildRoleAssignments(curr.players, clamped)
            )
        }
    }

    fun assignRoles() {
        _gameState.update { curr ->
            val maxImpostors = (curr.players.size - 1).coerceAtLeast(1)
            val clamped = curr.impostorCount.coerceIn(1, maxImpostors)
            curr.copy(
                impostorCount = clamped,
                playerRoles = buildRoleAssignments(curr.players, clamped)
            )
        }
    }

    fun toggleRoleReveal(playerId: String) {
        _gameState.update { curr ->
            val next = if (playerId in curr.revealedRolePlayerIds) {
                curr.revealedRolePlayerIds - playerId
            } else {
                curr.revealedRolePlayerIds + playerId
            }
            curr.copy(revealedRolePlayerIds = next)
        }
    }

    fun revealAllRoles() {
        _gameState.update { curr ->
            curr.copy(revealedRolePlayerIds = curr.players.map { it.id }.toSet())
        }
    }

    fun hideAllRoles() {
        _gameState.update { curr ->
            curr.copy(revealedRolePlayerIds = emptySet())
        }
    }

    fun addPlayer(rawName: String) {
        val trimmed = rawName.trim().take(24)
        _gameState.update { curr ->
            val finalName = trimmed.ifEmpty { "Player ${curr.players.size + 1}" }
            val newPlayer = LocalPlayer(
                id = UUID.randomUUID().toString().take(8),
                name = finalName
            )
            val updatedPlayers = curr.players + newPlayer
            val maxImpostors = (updatedPlayers.size - 1).coerceAtLeast(1)
            val clampedImp = curr.impostorCount.coerceIn(1, maxImpostors)
            curr.copy(
                players = updatedPlayers,
                impostorCount = clampedImp,
                playerRoles = buildRoleAssignments(updatedPlayers, clampedImp)
            )
        }
    }

    fun removePlayer(playerId: String) {
        _gameState.update { curr ->
            if (curr.players.size <= 2) return@update curr
            val updatedPlayers = curr.players.filterNot { it.id == playerId }
            val maxImpostors = (updatedPlayers.size - 1).coerceAtLeast(1)
            val clampedImp = curr.impostorCount.coerceIn(1, maxImpostors)
            curr.copy(
                players = updatedPlayers,
                impostorCount = clampedImp,
                playerRoles = buildRoleAssignments(updatedPlayers, clampedImp),
                revealedRolePlayerIds = curr.revealedRolePlayerIds - playerId
            )
        }
    }

    fun setTurnDuration(seconds: Int) {
        val clamped = seconds.coerceIn(5, 300)
        _gameState.update { curr ->
            curr.copy(
                turnDurationSeconds = clamped,
                currentTimerCount = if (curr.phase == GamePhase.LOBBY) clamped else curr.currentTimerCount.coerceAtMost(clamped)
            )
        }
    }

    fun startDiscussion() {
        stopTimer()
        _gameState.update { curr ->
            val maxImpostors = (curr.players.size - 1).coerceAtLeast(1)
            val clampedImp = curr.impostorCount.coerceIn(1, maxImpostors)
            val roles = if (curr.playerRoles.keys == curr.players.map { it.id }.toSet()) {
                curr.playerRoles
            } else {
                buildRoleAssignments(curr.players, clampedImp)
            }
            val turnOrder = CryptoRandom.shuffled(curr.players.map { it.id })
            curr.copy(
                phase = GamePhase.DISCUSSION,
                impostorCount = clampedImp,
                playerRoles = roles,
                turnOrderIds = turnOrder,
                currentTurnIndex = 0,
                turnStatus = TurnStatus.ACTIVE,
                currentTimerCount = curr.turnDurationSeconds,
                votesByPlayerId = emptyMap()
            )
        }
        startTimerLoop()
    }

    fun toggleTimerPause() {
        _gameState.update { curr ->
            if (curr.phase != GamePhase.DISCUSSION) return@update curr
            val nextStatus = when (curr.turnStatus) {
                TurnStatus.ACTIVE -> TurnStatus.PAUSED
                TurnStatus.PAUSED -> if (curr.currentTimerCount > 0) TurnStatus.ACTIVE else TurnStatus.EXPIRED
                TurnStatus.EXPIRED, TurnStatus.IDLE -> curr.turnStatus
            }
            curr.copy(turnStatus = nextStatus)
        }
    }

    fun nextTurn() {
        val curr = _gameState.value
        if (curr.phase != GamePhase.DISCUSSION) return
        val nextIndex = curr.currentTurnIndex + 1
        if (nextIndex >= curr.turnOrderIds.size) {
            startVoting()
        } else {
            _gameState.update { state ->
                state.copy(
                    currentTurnIndex = nextIndex,
                    turnStatus = TurnStatus.ACTIVE,
                    currentTimerCount = state.turnDurationSeconds
                )
            }
        }
    }

    fun startVoting() {
        stopTimer()
        _gameState.update { curr ->
            curr.copy(
                phase = GamePhase.VOTING,
                turnStatus = TurnStatus.IDLE,
                votesByPlayerId = emptyMap()
            )
        }
    }

    fun castVote(voterId: String, suspectId: String) {
        if (voterId == suspectId) return
        _gameState.update { curr ->
            if (curr.phase != GamePhase.VOTING) return@update curr
            curr.copy(votesByPlayerId = curr.votesByPlayerId + (voterId to suspectId))
        }
    }

    fun returnToLobby() {
        stopTimer()
        _gameState.update { curr ->
            curr.copy(
                phase = GamePhase.LOBBY,
                turnStatus = TurnStatus.IDLE,
                currentTurnIndex = 0,
                currentTimerCount = curr.turnDurationSeconds,
                votesByPlayerId = emptyMap()
            )
        }
    }

    fun tickTimerOnceForTest() {
        _gameState.update { curr ->
            if (curr.phase != GamePhase.DISCUSSION || curr.turnStatus != TurnStatus.ACTIVE) {
                return@update curr
            }
            val nextSec = (curr.currentTimerCount - 1).coerceAtLeast(0)
            curr.copy(
                currentTimerCount = nextSec,
                turnStatus = if (nextSec == 0) TurnStatus.EXPIRED else TurnStatus.ACTIVE
            )
        }
    }

    private fun buildRoleAssignments(
        players: List<LocalPlayer>,
        impostorCount: Int
    ): Map<String, PlayerRole> {
        if (players.isEmpty()) return emptyMap()
        val maxImp = (players.size - 1).coerceAtLeast(1)
        val targetImpostors = impostorCount.coerceIn(1, maxImp)
        val impostorIds = CryptoRandom.shuffled(players.map { it.id })
            .take(targetImpostors)
            .toSet()
        return players.associate { player ->
            player.id to if (player.id in impostorIds) PlayerRole.IMPOSTOR else PlayerRole.NORMAL
        }
    }

    private fun startTimerLoop() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val curr = _gameState.value
                if (curr.phase != GamePhase.DISCUSSION) break
                if (curr.turnStatus == TurnStatus.ACTIVE && curr.currentTimerCount > 0) {
                    val nextSec = (curr.currentTimerCount - 1).coerceAtLeast(0)
                    _gameState.update { state ->
                        state.copy(
                            currentTimerCount = nextSec,
                            turnStatus = if (nextSec == 0) TurnStatus.EXPIRED else TurnStatus.ACTIVE
                        )
                    }
                    if (nextSec == 0) {
                        SoundCuePlayer.playTimerExpiredCue()
                    }
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
