package com.example

import com.example.data.GamePhase
import com.example.data.PlayerRole
import com.example.data.TurnStatus
import com.example.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameViewModelTest {

    @Test
    fun `initial state is lobby with assigned impostor matching count`() {
        val viewModel = GameViewModel()
        val state = viewModel.gameState.value

        assertEquals(GamePhase.LOBBY, state.phase)
        assertEquals(TurnStatus.IDLE, state.turnStatus)
        assertEquals(4, state.players.size)
        assertEquals(4, state.playerRoles.size)
        assertEquals(1, state.playerRoles.values.count { it == PlayerRole.IMPOSTOR })
    }

    @Test
    fun `updating impostor count reassigns exact impostor count clamped to player count`() {
        val viewModel = GameViewModel()
        viewModel.setImpostorCount(2)
        assertEquals(2, viewModel.gameState.value.impostorCount)
        assertEquals(2, viewModel.gameState.value.playerRoles.values.count { it == PlayerRole.IMPOSTOR })

        // Cannot exceed players.size - 1
        viewModel.setImpostorCount(10)
        assertEquals(3, viewModel.gameState.value.impostorCount)
        assertEquals(3, viewModel.gameState.value.playerRoles.values.count { it == PlayerRole.IMPOSTOR })
    }

    @Test
    fun `transitions cleanly between lobby discussion and voting phases`() {
        val viewModel = GameViewModel()
        viewModel.setTurnDuration(10)

        // Lobby -> Discussion
        viewModel.startDiscussion()
        val discussionState = viewModel.gameState.value
        assertEquals(GamePhase.DISCUSSION, discussionState.phase)
        assertEquals(TurnStatus.ACTIVE, discussionState.turnStatus)
        assertEquals(10, discussionState.currentTimerCount)
        assertEquals(4, discussionState.turnOrderIds.size)

        // Pause & Resume turn status
        viewModel.toggleTimerPause()
        assertEquals(TurnStatus.PAUSED, viewModel.gameState.value.turnStatus)
        viewModel.toggleTimerPause()
        assertEquals(TurnStatus.ACTIVE, viewModel.gameState.value.turnStatus)

        // Tick down to 0 -> EXPIRED
        repeat(10) { viewModel.tickTimerOnceForTest() }
        assertEquals(0, viewModel.gameState.value.currentTimerCount)
        assertEquals(TurnStatus.EXPIRED, viewModel.gameState.value.turnStatus)

        // Next turn resets timer and status
        viewModel.nextTurn()
        assertEquals(1, viewModel.gameState.value.currentTurnIndex)
        assertEquals(10, viewModel.gameState.value.currentTimerCount)
        assertEquals(TurnStatus.ACTIVE, viewModel.gameState.value.turnStatus)

        // Discussion -> Voting
        viewModel.startVoting()
        val votingState = viewModel.gameState.value
        assertEquals(GamePhase.VOTING, votingState.phase)
        assertEquals(TurnStatus.IDLE, votingState.turnStatus)
        assertTrue(votingState.votesByPlayerId.isEmpty())

        // Voting -> Lobby
        viewModel.returnToLobby()
        val resetLobby = viewModel.gameState.value
        assertEquals(GamePhase.LOBBY, resetLobby.phase)
        assertEquals(TurnStatus.IDLE, resetLobby.turnStatus)
        assertEquals(10, resetLobby.currentTimerCount)
    }
}
