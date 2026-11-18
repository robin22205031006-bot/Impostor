package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class RoomStatus {
    LOBBY,
    REVEAL,
    PLAYING,
    VOTING,
    RESULTS;

    companion object {
        fun fromRaw(raw: String): RoomStatus =
            entries.firstOrNull { it.name == raw } ?: LOBBY
    }
}

enum class PlayerRole {
    NORMAL,
    IMPOSTOR;

    companion object {
        fun fromRaw(raw: String): PlayerRole =
            entries.firstOrNull { it.name == raw } ?: NORMAL
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Agent",
    val gamesPlayed: Long = 0L,
    val wins: Long = 0L,
    val impostorRounds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GameRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostId: String = "",
    val status: String = RoomStatus.LOBBY.name,
    val gameMode: String = GameMode.CLASSIC.name,
    val impostorCount: Long = 1L,
    val turnDurationSeconds: Long = 45L,
    val selectedCategory: String = WordBank.CATEGORY_ALL,
    val hintsEnabled: Boolean = true,
    val categoryCluesEnabled: Boolean = true,
    val timerEnabled: Boolean = true,
    val impostorAbilityEnabled: Boolean = false,
    val saviorAgentEnabled: Boolean = false,
    val agentDirectorEnabled: Boolean = false,
    val agentPredictionEnabled: Boolean = false,
    val currentRound: Long = 0L,
    val category: String = "",
    val genre: String = "",
    val hint: String = "",
    val memberUids: List<String> = emptyList(),
    val playerNames: Map<String, String> = emptyMap(),
    val playerScores: Map<String, Long> = emptyMap(),
    val turnOrder: List<String> = emptyList(),
    val currentTurnIndex: Long = 0L,
    val turnStartedAt: Timestamp? = null,
    val timerPaused: Boolean = false,
    val readyUids: List<String> = emptyList(),
    val votes: Map<String, String> = emptyMap(),
    val revealedWord: String? = null,
    val revealedImpostorUids: List<String> = emptyList(),
    val revealedKillerUid: String? = null,
    val revealedNullifiedVoterUid: String? = null,
    val revealedSaviorUid: String? = null,
    val revealedProtectedUid: String? = null,
    val revealedSaviorSaved: Boolean? = null,
    val revealedDirectorUid: String? = null,
    val revealedPredictions: Map<String, String> = emptyMap(),
    val revealedRoundDeltas: Map<String, Long> = emptyMap(),
    val impostorGuess: String? = null,
    val impostorGuessBy: String? = null,
    val impostorGuessCorrect: Boolean? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roomStatus(): RoomStatus = RoomStatus.fromRaw(status)
    fun resolvedGameMode(): GameMode = GameMode.fromRaw(gameMode)
}

@Keep
@IgnoreExtraProperties
data class PlayerAssignment(
    @DocumentId val id: String = "",
    val uid: String = "",
    val role: String = PlayerRole.NORMAL.name,
    val secretWord: String = "",
    val category: String = "",
    val genre: String = "",
    val hint: String = "",
    val ultimateKiller: Boolean = false,
    val savior: Boolean = false,
    val director: Boolean = false,
    val round: Long = 1L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun playerRole(): PlayerRole = PlayerRole.fromRaw(role)
}

@Keep
@IgnoreExtraProperties
data class HostRoundSecret(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val secretWord: String = "",
    val impostorUids: List<String> = emptyList(),
    val ultimateKillerUid: String? = null,
    val saviorUid: String? = null,
    val directorUid: String? = null,
    val round: Long = 1L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SecretRoundAction(
    @DocumentId val id: String = "",
    val uid: String = "",
    val round: Long = 1L,
    val killedVoterUid: String = "",
    val protectedPlayerUid: String = "",
    val predictedImpostorUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class LocalPlayer(
    val id: String,
    val name: String,
    val score: Int = 0
)
