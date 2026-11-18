package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.GameRepository
import com.example.data.WordBank
import com.example.data.authStateFlow
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HubScreen
import com.example.ui.screens.LocalGameScreen
import com.example.ui.screens.MultiplayerRoomScreen
import com.example.ui.theme.ImpostorTheme
import com.example.viewmodel.HubViewModel
import com.example.viewmodel.LocalGameViewModel
import com.example.viewmodel.MultiplayerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val ROUTE_LOCAL = "local"
private const val ROUTE_ONLINE = "online"
private const val ROUTE_ROOM = "room/{roomCode}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WordBank.loadFromAssets(applicationContext)
        enableEdgeToEdge()
        setContent {
            ImpostorTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_LOCAL,
        enterTransition = {
            fadeIn(animationSpec = tween(260)) + slideInHorizontally(animationSpec = tween(260)) { it / 8 }
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) + slideOutHorizontally(animationSpec = tween(200)) { -it / 10 }
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(240)) + slideInHorizontally(animationSpec = tween(240)) { -it / 10 }
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) + slideOutHorizontally(animationSpec = tween(200)) { it / 8 }
        }
    ) {
        composable(ROUTE_LOCAL) {
            val localViewModel: LocalGameViewModel = viewModel(key = "local_game")
            LocalGameScreen(
                viewModel = localViewModel,
                onOpenOnline = {
                    navController.navigate(ROUTE_ONLINE) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(ROUTE_ONLINE) {
            val user = currentUser
            if (user == null) {
                AuthScreen(
                    onAuthSuccess = {},
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                val uid = user.uid
                val defaultName = user.displayName?.takeIf { it.isNotBlank() } ?: "Agent"
                key(uid) {
                    val hubViewModel: HubViewModel = viewModel(
                        key = "hub_$uid",
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY])
                                val databaseId = app.getString(R.string.firestore_database_id)
                                val db = FirebaseFirestore.getInstance(databaseId)
                                HubViewModel(
                                    repository = GameRepository(db),
                                    currentUserId = uid,
                                    defaultDisplayName = defaultName
                                )
                            }
                        }
                    )
                    HubScreen(
                        viewModel = hubViewModel,
                        fallbackName = defaultName,
                        onLaunchPassAndPlay = {
                            navController.popBackStack(ROUTE_LOCAL, inclusive = false)
                        },
                        onEnterRoom = { roomCode ->
                            navController.navigate("room/$roomCode")
                        }
                    )
                }
            }
        }

        composable(
            route = ROUTE_ROOM,
            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val user = currentUser
            val roomCode = backStackEntry.arguments?.getString("roomCode").orEmpty()
            if (user == null) {
                AuthScreen(
                    onAuthSuccess = {},
                    onNavigateBack = { navController.popBackStack(ROUTE_LOCAL, inclusive = false) }
                )
            } else {
                val uid = user.uid
                key(uid, roomCode) {
                    val multiplayerViewModel: MultiplayerViewModel = viewModel(
                        key = "room_${uid}_$roomCode",
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY])
                                val databaseId = app.getString(R.string.firestore_database_id)
                                val db = FirebaseFirestore.getInstance(databaseId)
                                MultiplayerViewModel(
                                    repository = GameRepository(db),
                                    roomCode = roomCode,
                                    currentUserId = uid
                                )
                            }
                        }
                    )
                    MultiplayerRoomScreen(
                        viewModel = multiplayerViewModel,
                        onExitRoom = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
