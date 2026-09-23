package com.example.chiptune.ui.screens.smb

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
import com.example.chiptune.ui.components.OCTAVE_NOTES
import com.example.chiptune.ui.components.PianoKeyboard
import com.example.chiptune.ui.components.Visualiser

@Composable
fun SmbScreen(
    modifier: Modifier = Modifier,
    viewModel: SmbViewModel = viewModel()
) {
    val isPlayingMelody by viewModel.isPlayingMelody.collectAsState()
    val activeNote by viewModel.activeNote.collectAsState()
    val waveformData by viewModel.waveformData.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Playing a melody",
            style = MaterialTheme.typography.titleLarge
        )

        Visualiser(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp))
                .padding(4.dp),
            wavedata = waveformData.mixed,
            lineColor = Color(0xFF00E676)
        )

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
                text = if (isPlayingMelody) "Stop Melody" else "Play Melody",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // 1-Octave Piano Keyboard Card
        PianoKeyboard(
            notes = OCTAVE_NOTES,
            activeNote = activeNote,
            onNoteDown = { note -> viewModel.playNote(note) },
            onNoteUp = { note -> viewModel.stopNote(note) }
        )
    }
}
