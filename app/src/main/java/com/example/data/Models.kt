package com.example.data

enum class PlayerRole {
    NORMAL,
    IMPOSTOR
}

enum class GamePhase(val title: String) {
    LOBBY("Lobby"),
    DISCUSSION("Discussion"),
    VOTING("Voting")
}

enum class TurnStatus(val label: String) {
    IDLE("Standby"),
    ACTIVE("Active Turn"),
    PAUSED("Paused"),
    EXPIRED("Time Expired")
}

data class LocalPlayer(
    val id: String,
    val name: String,
    val score: Int = 0
)

data class GameState(
    val phase: GamePhase = GamePhase.LOBBY,
    val players: List<LocalPlayer> = listOf(
        LocalPlayer(id = "p1", name = "Player 1"),
        LocalPlayer(id = "p2", name = "Player 2"),
        LocalPlayer(id = "p3", name = "Player 3"),
        LocalPlayer(id = "p4", name = "Player 4")
    ),
    val impostorCount: Int = 1,
    val playerRoles: Map<String, PlayerRole> = emptyMap(),
    val revealedRolePlayerIds: Set<String> = emptySet(),
    val turnOrderIds: List<String> = emptyList(),
    val currentTurnIndex: Int = 0,
    val turnStatus: TurnStatus = TurnStatus.IDLE,
    val currentTimerCount: Int = 45,
    val turnDurationSeconds: Int = 45,
    val votesByPlayerId: Map<String, String> = emptyMap()
) {
    val activeTurnPlayer: LocalPlayer?
        get() {
            val activeId = turnOrderIds.getOrNull(currentTurnIndex) ?: return null
            return players.firstOrNull { it.id == activeId }
        }

    val impostorIds: Set<String>
        get() = playerRoles.filterValues { it == PlayerRole.IMPOSTOR }.keys
}
