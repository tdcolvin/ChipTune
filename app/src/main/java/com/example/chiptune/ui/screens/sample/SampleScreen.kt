package com.example.chiptune.ui.screens.sample

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
import com.example.chiptune.ui.components.PianoKeyboard
import com.example.chiptune.ui.components.Visualiser

@Composable
fun SampleScreen(
    modifier: Modifier = Modifier,
    viewModel: SampleViewModel = viewModel()
) {
    val activeNote by viewModel.activeNote.collectAsState()
    val notesList by viewModel.currentNotesList.collectAsState()
    val selectedOctaveOffset by viewModel.selectedOctaveOffset.collectAsState()
    val waveform by viewModel.waveform.collectAsState()
    val sampleCount by viewModel.sampleCount.collectAsState()
    val isPlayingDemo by viewModel.isPlayingDemo.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Piano Sample Synthesizer",
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
                        text = "Realtime Oscilloscope",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676)
                    )
                    Text(
                        text = activeNote?.let { "Note: ${it.name} (${it.frequency.toInt()} Hz)" }
                            ?: if (isPlayingDemo) "Demo Playing" else "Ready",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (activeNote != null || isPlayingDemo) Color(0xFF00E676) else MaterialTheme.colorScheme.outline
                    )
                }

                Visualiser(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    wavedata = waveform,
                    lineColor = Color(0xFF00E676)
                )
            }
        }

        // Sample Information & Octave Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sample & Octave Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Loaded res/raw/piano.raw ($sampleCount samples @ 44.1kHz, Base C4)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(-1 to "Octave 3", 0 to "Octave 4", 1 to "Octave 5").forEach { (offset, label) ->
                        FilterChip(
                            selected = selectedOctaveOffset == offset,
                            onClick = { viewModel.setOctaveOffset(offset) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // Play Demo Arpeggio Button
        Button(
            onClick = { viewModel.toggleDemo() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = if (isPlayingDemo) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            }
        ) {
            Text(
                text = if (isPlayingDemo) "Stop Demo" else "Play Demo Arpeggio",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Piano Keyboard Component
        PianoKeyboard(
            notes = notesList,
            activeNote = activeNote,
            onNoteDown = { note -> viewModel.playNote(note) },
            onNoteUp = { note -> viewModel.stopNote(note) }
        )
    }
}
