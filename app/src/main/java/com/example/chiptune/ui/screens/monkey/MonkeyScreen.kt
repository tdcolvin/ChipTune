package com.example.chiptune.ui.screens.monkey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chiptune.SynthType
import com.example.chiptune.ui.components.OCTAVE_NOTES
import com.example.chiptune.ui.components.PianoKeyboard
import com.example.chiptune.ui.components.Visualiser

@Composable
fun MonkeyScreen(
    modifier: Modifier = Modifier,
    viewModel: MonkeyViewModel = viewModel(),
) {
    val isPlayingMelody by viewModel.isPlayingMelody.collectAsState()
    val selectedSynthType by viewModel.selectedSynthType.collectAsState()
    val activeNote by viewModel.activeNote.collectAsState()
    val isVoicesPlaying by viewModel.isVoicesPlaying.collectAsState()
    val waveformData by viewModel.waveformData.collectAsState()
    val scrollState = rememberScrollState()

    val synthTitle = if (selectedSynthType == SynthType.Opl2) "OPL2" else "2-Op FM"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Monkey Island Theme",
            style = MaterialTheme.typography.titleLarge
        )

        // Visualiser Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$synthTitle Wave Output",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB74D)
                    )
                    Text(
                        text = when {
                            isPlayingMelody -> "Playing Monkey Island Theme"
                            activeNote != null -> "Playing: ${activeNote?.name} (${activeNote?.frequency?.toInt()} Hz)"
                            isVoicesPlaying -> "Playing Notes"
                            else -> "Idle"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isPlayingMelody || (activeNote != null) || isVoicesPlaying) Color(0xFFFFB74D) else MaterialTheme.colorScheme.outline
                    )
                }

                Visualiser(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    wavedata = waveformData.mixed,
                    lineColor = Color(0xFFFFB74D)
                )
            }
        }

        // Synthesis Mode Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Synthesis Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSynthType == SynthType.Opl2,
                        onClick = { viewModel.setSynthType(SynthType.Opl2) },
                        label = { Text("OPL2 Synthesis") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedSynthType == SynthType.Fm2op,
                        onClick = { viewModel.setSynthType(SynthType.Fm2op) },
                        label = { Text("2-Op FM Synthesis") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Play Melody Button
        Button(
            onClick = { viewModel.togglePlayMelody() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = if (isPlayingMelody) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            }
        ) {
            Text(
                text = if (isPlayingMelody) "Stop Melody" else "Play Melody ($synthTitle)",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // 1-Octave Piano Keyboard Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1-Octave Keyboard ($synthTitle Synthesis)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap key to play note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                PianoKeyboard(
                    notes = OCTAVE_NOTES,
                    activeNote = activeNote,
                    onNoteDown = { note -> viewModel.playNote(note) },
                    onNoteUp = { note -> viewModel.stopNote(note) }
                )
            }
        }
    }
}
