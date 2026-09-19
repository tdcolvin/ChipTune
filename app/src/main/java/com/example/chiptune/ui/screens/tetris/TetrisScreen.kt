package com.example.chiptune.ui.screens.tetris

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chiptune.SynthType
import com.example.chiptune.ui.components.Visualiser

@Composable
fun TetrisScreen(
    modifier: Modifier = Modifier,
    viewModel: TetrisViewModel = viewModel()
) {
    val waveData by viewModel.waveData.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Tetris Chiptune Synth", style = MaterialTheme.typography.titleLarge)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(onClick = { viewModel.startSynth(SynthType.Sine) }) { Text("Sine") }
            Button(onClick = { viewModel.startSynth(SynthType.Square) }) { Text("Square") }
            Button(onClick = { viewModel.startSynth(SynthType.Sawtooth) }) { Text("Saw") }
            Button(onClick = { viewModel.startSynth(SynthType.Fm2op) }) { Text("FM") }
            Button(onClick = { viewModel.startSynth(SynthType.Opl2) }) { Text("OPL2") }
        }

        Button(
            onClick = { viewModel.stopSynth() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Stop")
        }

        if (viewModel.channelList.isNotEmpty()) {
            Text("Channels:", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.channelList.forEach { ch ->
                    OutlinedButton(
                        onClick = { viewModel.toggleMute(ch) }
                    ) {
                        Text(if (ch.isMuted) "${ch.name} (Muted)" else ch.name)
                    }
                }
            }
        }

        Visualiser(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            wavedata = waveData
        )
    }
}
