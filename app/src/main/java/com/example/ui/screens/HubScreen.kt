package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.GameRoom
import com.example.data.UiState
import com.example.data.signOutUser
import com.example.ui.components.ImpostorAnimatedBackdrop
import com.example.ui.components.PulsingStatusDot
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalImpostorPalette
import com.example.viewmodel.HubViewModel

@Composable
fun HubScreen(
    viewModel: HubViewModel,
    fallbackName: String,
    onLaunchPassAndPlay: () -> Unit,
    onEnterRoom: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val palette = LocalImpostorPalette.current

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val myRoomsState by viewModel.myRoomsState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    val currentNickname = when (val p = profileState) {
        is UiState.Success -> p.data?.displayName ?: fallbackName
        else -> fallbackName
    }

    var joinCodeInput by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var nicknameDraft by remember { mutableStateOf(currentNickname) }

    ImpostorAnimatedBackdrop {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header bar with back to Pass & Play, editable Agent Nickname & Sign Out
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onLaunchPassAndPlay,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("hub_back_to_local_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Pass & Play",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PulsingStatusDot(color = palette.cyanAccent)
                                Text(
                                    text = "ONLINE LOBBY",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        nicknameDraft = currentNickname
                                        showRenameDialog = true
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("edit_nickname_row")
                            ) {
                                Text(
                                    text = "Agent: $currentNickname",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.cyanAccent
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Rename your agent nickname",
                                    tint = palette.cyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { signOutUser(credentialManager, scope) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.sign_out),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (actionError != null) {
                item {
                    Surface(
                        color = palette.impostorSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.impostorAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = actionError ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.impostorGlow,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = viewModel::clearError) {
                                Text("Dismiss", color = palette.impostorGlow)
                            }
                        }
                    }
                }
            }

            // Mode 1: Pass & Play (1 Phone)
            item {
                Surface(
                    onClick = onLaunchPassAndPlay,
                    color = palette.normalSurface.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.5.dp, palette.normalAccent.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pass_and_play_card")
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = palette.normalAccent.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhoneAndroid,
                                contentDescription = null,
                                tint = palette.normalGlow,
                                modifier = Modifier
                                    .padding(14.dp)
                                    .size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pass_and_play),
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.normalGlow
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1 phone passed around. Add unlimited players, rename anyone, set 1–4+ impostors, and track turns with the live timer.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Start Pass and Play",
                            tint = palette.normalGlow
                        )
                    }
                }
            }

            // Mode 2: Online Multiplayer
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = palette.cyanAccent.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Public,
                                    contentDescription = null,
                                    tint = palette.cyanAccent,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.online_multiplayer),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Everyone views their own red/green glowing role card & live timer on their own device.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.createRoom(currentNickname, onEnterRoom) },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.cyanAccent,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_room_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = stringResource(R.string.create_room),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { raw ->
                                    joinCodeInput = raw.uppercase().filter { it.isLetterOrDigit() }.take(8)
                                },
                                label = { Text("Room Code") },
                                placeholder = { Text("AB3K9Z") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("join_room_code_input")
                            )
                            OutlinedButton(
                                onClick = {
                                    viewModel.joinRoom(joinCodeInput, currentNickname, onEnterRoom)
                                },
                                enabled = !isBusy && joinCodeInput.length >= 4,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, palette.cyanAccent),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("join_room_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.join_room),
                                    color = palette.cyanAccent
                                )
                            }
                        }
                    }
                }
            }

            // Active Rooms List
            item {
                Text(
                    text = "YOUR ACTIVE ROOMS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (val roomsState = myRoomsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Text(
                            text = roomsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.impostorGlow
                        )
                    }
                }

                is UiState.Success -> {
                    if (roomsState.data.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MeetingRoom,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "No active online rooms yet. Create a room or enter a friend's code above.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(roomsState.data, key = { it.roomCode }) { room ->
                            ActiveRoomRow(
                                room = room,
                                onClick = { onEnterRoom(room.roomCode) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Your Nickname") },
            text = {
                OutlinedTextField(
                    value = nicknameDraft,
                    onValueChange = { nicknameDraft = it.take(24) },
                    label = { Text("Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateNickname(nicknameDraft)
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
private fun ActiveRoomRow(
    room: GameRoom,
    onClick: () -> Unit
) {
    val palette = LocalImpostorPalette.current
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_room_${room.roomCode}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = palette.cyanSurface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = room.roomCode,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = palette.cyanAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Column {
                    Text(
                        text = "Round ${room.currentRound} • ${room.status}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${room.memberUids.size} players • ${room.impostorCount} impostor(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Enter room",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
