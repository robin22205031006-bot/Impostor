package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalImpostorPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UltimateKillerAbilityCard(
    candidates: List<Pair<String, String>>,
    selectedTargetId: String?,
    onSelectTarget: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalImpostorPalette.current
    val selectedName = candidates.firstOrNull { it.first == selectedTargetId }?.second

    Surface(
        color = palette.impostorSurface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, palette.impostorAccent),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ultimate_killer_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.GpsFixed,
                    contentDescription = "Ultimate Agent Killer ability",
                    tint = palette.impostorGlow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "ULTIMATE AGENT KILLER",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.impostorGlow
                )
            }

            Text(
                text = "Tap one player below to void their vote. If they vote for an impostor during voting, their vote will not be counted!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedTargetId == null,
                    onClick = { onSelectTarget(null) },
                    label = { Text("No Target") },
                    leadingIcon = if (selectedTargetId == null) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("killer_skip_chip")
                )

                candidates.forEach { (id, name) ->
                    val isSelected = id == selectedTargetId
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) palette.impostorAccent else MaterialTheme.colorScheme.surfaceContainer,
                        label = "killer_chip_bg"
                    )
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectTarget(id) },
                        label = { Text(name) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.GpsFixed,
                                    contentDescription = "Silenced target",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipBg,
                            selectedLabelColor = MaterialTheme.colorScheme.onError,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.testTag("killer_target_$id")
                    )
                }
            }

            Text(
                text = if (selectedName != null) {
                    "Locked Target: $selectedName's vote will be voided."
                } else {
                    "Tap a player's name to silence their vote (optional)."
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (selectedName != null) palette.impostorGlow else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentPredictionCard(
    candidates: List<Pair<String, String>>,
    selectedSuspectId: String?,
    onSelectSuspect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalImpostorPalette.current
    val selectedName = candidates.firstOrNull { it.first == selectedSuspectId }?.second

    Surface(
        color = palette.cyanSurface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, palette.cyanAccent.copy(alpha = 0.85f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("agent_prediction_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = "Agent Impostor Prediction",
                    tint = palette.cyanAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "IMPOSTOR PREDICTION (OPTIONAL)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.cyanAccent
                )
            }

            Text(
                text = "Lock an early impostor call or skip:\n• Right prediction + right vote = +3 pts (+1 if vote misses)\n• Wrong prediction = -2 pts (0 pts total if vote hits)\n• Skip prediction = 0 pts (+2 if vote hits)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedSuspectId == null,
                    onClick = { onSelectSuspect(null) },
                    label = { Text("Skip Prediction") },
                    leadingIcon = if (selectedSuspectId == null) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("prediction_skip_chip")
                )

                candidates.forEach { (id, name) ->
                    val isSelected = id == selectedSuspectId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSuspect(id) },
                        label = { Text(name) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Predicted impostor",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.cyanAccent,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("prediction_target_$id")
                    )
                }
            }

            Text(
                text = if (selectedName != null) {
                    "Prediction locked: $selectedName"
                } else {
                    "No prediction selected (safe 0 pts baseline)."
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (selectedName != null) palette.cyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaviorAbilityCard(
    candidates: List<Pair<String, String>>,
    selectedProtectedId: String?,
    onSelectProtected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalImpostorPalette.current
    val selectedName = candidates.firstOrNull { it.first == selectedProtectedId }?.second

    Surface(
        color = palette.normalSurface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, palette.normalAccent),
        modifier = modifier
            .fillMaxWidth()
            .testTag("savior_ability_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "Savior Agent protection shield",
                    tint = palette.normalGlow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "SAVIOR AGENT — PROTECTION SHIELD",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.normalGlow
                )
            }

            Text(
                text = "Select one agent to protect. If the Agent Killer tries to void their vote, the shield saves them (+1 bonus pt, no deduction if missed).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedProtectedId == null,
                    onClick = { onSelectProtected(null) },
                    label = { Text("No Shield (Skip)") },
                    leadingIcon = if (selectedProtectedId == null) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("savior_skip_chip")
                )

                candidates.forEach { (id, name) ->
                    val isSelected = id == selectedProtectedId
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) palette.normalAccent else MaterialTheme.colorScheme.surfaceContainer,
                        label = "savior_chip_bg"
                    )
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectProtected(id) },
                        label = { Text(name) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = "Protected player",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipBg,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.testTag("savior_target_$id")
                    )
                }
            }

            Text(
                text = if (selectedName != null) {
                    "Protection Shield active on: $selectedName"
                } else {
                    "Tap a player's name to grant them your Protection Shield (optional)."
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (selectedName != null) palette.normalGlow else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AgentDirectorBadge(modifier: Modifier = Modifier) {
    val palette = LocalImpostorPalette.current
    Surface(
        color = palette.timerSurface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, palette.timerAccent),
        modifier = modifier
            .fillMaxWidth()
            .testTag("agent_director_badge")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.HowToVote,
                    contentDescription = "Agent Director double vote",
                    tint = palette.timerAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AGENT DIRECTOR — 2X VOTE POWER",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.timerAccent
                )
            }
            Text(
                text = "Your vote counts as 2 votes in the tally! No deduction if your vote is wrong (only wrong start-of-round prediction deducts -2 pts).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun OtherImpostorAllyNote(modifier: Modifier = Modifier) {
    val palette = LocalImpostorPalette.current
    Surface(
        color = palette.impostorSurface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.impostorAccent.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Another impostor holds the Ultimate Agent Killer ability this round.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.impostorGlow,
            modifier = Modifier.padding(14.dp)
        )
    }
}
