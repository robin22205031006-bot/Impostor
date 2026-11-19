package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Impostor", appName)
    }

    @Test
    fun `main activity launches and navigates between pass and play and role lobby`() {
        composeRule.onNodeWithTag("game_setup_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_role_lobby").performClick()
        composeRule.onNodeWithTag("game_lobby_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("lobby_toggle_all_roles_button").performClick()
        composeRule.onNodeWithTag("lobby_role_card_p1").assertIsDisplayed()

        composeRule.onNodeWithTag("start_discussion_phase_button").performClick()
        composeRule.onNodeWithTag("circular_countdown_timer").assertIsDisplayed()

        composeRule.onNodeWithTag("go_to_voting_phase_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("return_to_lobby_button").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("return_to_lobby_button").performClick()
        composeRule.onNodeWithTag("tab_pass_and_play").performClick()
        composeRule.onNodeWithTag("game_setup_screen").assertIsDisplayed()
    }
}
