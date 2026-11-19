package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GameMode
import com.example.data.LocalPlayer
import com.example.data.WordBank
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.components.GameRoleIdentity
import com.example.ui.components.GlassSectionCard
import com.example.ui.components.RoleEmblemBadge
import com.example.ui.components.RoleEmblemChip
import com.example.ui.components.WordCategoryBox
import com.example.ui.components.style
import com.example.ui.theme.LocalImpostorPalette
import com.example.viewmodel.LocalGameUiState

@Composable
fun GameSetupScreen(
    state: LocalGameUiState,
    onAddPlayer: (String) -> Unit,
    onRenamePlayer: (String, String) -> Unit,
    onRemovePlayer: (String) -> Unit,
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
    onStartRound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalImpostorPalette.current
    val customWords by WordBank.customWordsFlow.collectAsStateWithLifecycle()
    val customCategoryName by WordBank.customCategoryNameFlow.collectAsStateWithLifecycle()
    var newPlayerDraft by remember { mutableStateOf("") }
    var editingPlayer by remember { mutableStateOf<LocalPlayer?>(null) }
    var renameDraft by remember { mutableStateOf("") }

    val maxImpostors = (state.players.size - 1).coerceAtLeast(1)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_setup_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Game Mode Selector (Classic, Speed Round, Expert Mode)
        item {
            GlassSectionCard(
                title = "GAME MODE",
                icon = Icons.Filled.SportsEsports,
                accentColor = palette.cyanAccent
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameMode.entries.forEach { mode ->
                        val selected = state.gameMode == mode
                        val modeIcon = when (mode) {
                            GameMode.CLASSIC -> Icons.Filled.SportsEsports
                            GameMode.SPEED_ROUND -> Icons.Filled.Bolt
                            GameMode.EXPERT -> Icons.Filled.Psychology
                        }
                        val cardBg by animateColorAsState(
                            targetValue = if (selected) palette.cyanSurface else MaterialTheme.colorScheme.surfaceContainerLow,
                            label = "mode_bg"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (selected) palette.cyanAccent else MaterialTheme.colorScheme.outlineVariant,
                            label = "mode_border"
                        )
                        Surface(
                            onClick = { onSelectMode(mode) },
                            color = cardBg,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = borderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mode_chip_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = (if (selected) palette.cyanAccent else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = null,
                                        tint = if (selected) palette.cyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (selected) palette.cyanAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                AnimatedVisibility(visible = selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = palette.cyanAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Impostor Count Selector (1, 2, 3, 4...)
        item {
            GlassSectionCard(
                title = "IMPOSTOR COUNT (1 TO $maxImpostors)",
                icon = Icons.Filled.VisibilityOff,
                accentColor = palette.impostorAccent
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AnimatedContent(
                            targetState = state.impostorCount,
                            transitionSpec = {
                                (slideInVertically { -it / 2 } + fadeIn()) togetherWith
                                    (slideOutVertically { it / 2 } + fadeOut())
                            },
                            label = "impostor_count_anim"
                        ) { count ->
                            Text(
                                text = "$count Impostor${if (count > 1) "s" else ""}",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.impostorGlow
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onSetImpostors(state.impostorCount - 1) },
                                enabled = state.impostorCount > 1,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("decrease_impostor_button"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-", style = MaterialTheme.typography.titleLarge)
                            }
                            OutlinedButton(
                                onClick = { onSetImpostors(state.impostorCount + 1) },
                                enabled = state.impostorCount < maxImpostors,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("increase_impostor_button"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..minOf(maxImpostors, 6)).forEach { num ->
                            FilterChip(
                                selected = state.impostorCount == num,
                                onClick = { onSetImpostors(num) },
                                label = { Text("$num") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.impostorSurface,
                                    selectedLabelColor = palette.impostorGlow
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Host Controls (Turn Hints, Category/Genre, Timer On/Off)
        item {
            GlassSectionCard(
                title = "HOST CONTROLS",
                icon = Icons.Filled.Tune,
                accentColor = palette.timerAccent
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HostToggleRow(
                        title = "Word Hints",
                        subtitle = "Show 1-word clue on revealed role cards",
                        checked = state.hintsEnabled,
                        onCheckedChange = onToggleHints,
                        tag = "toggle_hints_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    HostToggleRow(
                        title = "Category & Genre Labels",
                        subtitle = "Show Category & Genre around the word box",
                        checked = state.categoryCluesEnabled,
                        onCheckedChange = onToggleCategoryClues,
                        tag = "toggle_category_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    HostToggleRow(
                        title = "Turn Timer",
                        subtitle = "Track each player's turn with a countdown",
                        checked = state.timerEnabled,
                        onCheckedChange = onToggleTimer,
                        tag = "toggle_timer_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    HostToggleRow(
                        title = "Impostor Ability (Agent Killer)",
                        subtitle = "One impostor can void a player's vote during voting",
                        checked = state.impostorAbilityEnabled,
                        onCheckedChange = onToggleImpostorAbility,
                        tag = "toggle_impostor_ability_switch",
                        roleIdentity = GameRoleIdentity.AGENT_KILLER
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    HostToggleRow(
                        title = "Savior Agent",
                        subtitle = "One agent can shield a teammate from Agent Killer (+1 bonus if saved)",
                        checked = state.saviorAgentEnabled,
                        onCheckedChange = onToggleSaviorAgent,
                        tag = "toggle_savior_agent_switch",
                        roleIdentity = GameRoleIdentity.SAVIOR_AGENT
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    HostToggleRow(
                        title = "Agent Director",
                        subtitle = "One agent's vote counts as 2 votes (no wrong-vote deduction)",
                        checked = state.agentDirectorEnabled,
                        onCheckedChange = onToggleAgentDirector,
                        tag = "toggle_agent_director_switch",
                        roleIdentity = GameRoleIdentity.AGENT_DIRECTOR
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    HostToggleRow(
                        title = "Agent Prediction",
                        subtitle = "Agents may predict an impostor (+3/+1 or -2 pts)",
                        checked = state.agentPredictionEnabled,
                        onCheckedChange = onToggleAgentPrediction,
                        tag = "toggle_agent_prediction_switch",
                        roleIdentity = GameRoleIdentity.AGENT
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ROLE EMBLEMS & COLORS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GameRoleIdentity.entries.forEach { identity ->
                                RoleEmblemChip(identity = identity, badgeSize = 22.dp)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = state.timerEnabled,
                        enter = fadeIn() + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                        exit = fadeOut() + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = palette.timerAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "TURN TIMER LENGTH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.timerAccent
                                )
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(10, 20, 30, 45, 60, 90, 120).forEach { sec ->
                                    FilterChip(
                                        selected = state.turnDurationSeconds == sec,
                                        onClick = { onSetDuration(sec) },
                                        label = { Text("${sec}s") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = palette.timerSurface,
                                            selectedLabelColor = palette.timerAccent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Unified Word Category + Named Custom Words Box (All includes Custom!)
        item {
            WordCategoryBox(
                selectedCategory = state.selectedCategory,
                customCategoryName = customCategoryName,
                customWords = customWords,
                canEdit = true,
                onSelectCategory = onSelectCategory
            )
        }

        // 5. Unlimited Players with Rename & Remove
        item {
            GlassSectionCard(
                title = "PLAYERS (${state.players.size})",
                icon = Icons.Filled.Group,
                accentColor = palette.cyanAccent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPlayerDraft,
                        onValueChange = { newPlayerDraft = it.take(24) },
                        label = { Text("Add Player Name") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_player_input")
                    )
                    Button(
                        onClick = {
                            onAddPlayer(newPlayerDraft)
                            newPlayerDraft = ""
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("add_player_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add player")
                    }
                }
            }
        }

        items(state.players, key = { it.id }) { player ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (player.score != 0) {
                            Text(
                                text = "${player.score} pts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (player.score < 0) palette.impostorGlow else palette.timerAccent
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                editingPlayer = player
                                renameDraft = player.name
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("rename_player_${player.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Rename ${player.name}",
                                tint = palette.cyanAccent
                            )
                        }
                        IconButton(
                            onClick = { onRemovePlayer(player.id) },
                            enabled = state.players.size > 2,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("remove_player_${player.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove ${player.name}",
                                tint = if (state.players.size > 2) palette.impostorAccent else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onStartRound,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.normalAccent,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .drawBehind {
                        val spread = 12.dp.toPx()
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(palette.normalGlow.copy(alpha = 0.30f), Color.Transparent),
                                radius = maxOf(size.width, size.height) * 0.72f
                            ),
                            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                            size = size.copy(width = size.width + spread, height = size.height + spread),
                            topLeft = Offset(-spread / 2f, -spread / 2f)
                        )
                    }
                    .testTag("local_start_round_button")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(
                    text = "Deal Secret Roles & Start Round",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (editingPlayer != null) {
        AlertDialog(
            onDismissRequest = { editingPlayer = null },
            title = { Text("Rename Player") },
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
                        val target = editingPlayer
                        if (target != null) {
                            onRenamePlayer(target.id, renameDraft)
                        }
                        editingPlayer = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPlayer = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HostToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
    roleIdentity: GameRoleIdentity? = null
) {
    val palette = LocalImpostorPalette.current
    val roleStyle = roleIdentity?.style(palette)
    val trackColor = roleStyle?.accent ?: palette.normalAccent
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (roleIdentity != null) {
            RoleEmblemBadge(
                identity = roleIdentity,
                size = 32.dp,
                animated = checked,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (checked && roleStyle != null) roleStyle.glow else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = trackColor
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}
