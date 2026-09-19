package com.example.chiptune.ui.screens.tetris

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedButton
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
import com.example.chiptune.ui.components.Visualiser

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TetrisScreen(
    modifier: Modifier = Modifier,
    viewModel: TetrisViewModel = viewModel()
) {
    val waveformData by viewModel.waveformData.collectAsState()
    val scrollState = rememberScrollState()

    // Distinct colors for each channel visualiser
    val channelColors = listOf(
        Color(0xFF00E676), // Bright Green
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFF9800), // Orange
        Color(0xFFE91E63), // Pink
        Color(0xFFFFEB3B)  // Yellow
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Tetris Chiptune Synth", style = MaterialTheme.typography.titleLarge)

        // Synth Type Preset buttons
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { viewModel.startSynth(SynthType.Sine) }) { Text("Sine") }
            Button(onClick = { viewModel.startSynth(SynthType.Square) }) { Text("Square") }
            Button(onClick = { viewModel.startSynth(SynthType.Sawtooth) }) { Text("Saw") }
            Button(onClick = { viewModel.startSynth(SynthType.Fm2op) }) { Text("FM") }
            Button(onClick = { viewModel.startSynth(SynthType.Opl2) }) { Text("OPL2") }
            Button(
                onClick = { viewModel.stopSynth() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Stop")
            }
        }

        // Channel Mute / Unmute controls
        if (viewModel.channelList.isNotEmpty()) {
            Text("Channel Mute Controls:", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.channelList.forEach { ch ->
                    OutlinedButton(
                        onClick = { viewModel.toggleMute(ch) }
                    ) {
                        Text(if (ch.isMuted) "${ch.name} (Muted)" else ch.name)
                    }
                }
            }
        }

        // Channel Visualisers
        if (viewModel.channelList.isNotEmpty()) {
            Text(
                text = "Channel Visualisers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            viewModel.channelList.forEachIndexed { index, ch ->
                val color = channelColors[index % channelColors.size]
                val wave = waveformData.channelWaveforms[ch.name] ?: ShortArray(0)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Channel: ${ch.name}",
                                style = MaterialTheme.typography.titleSmall,
                                color = color
                            )
                            if (ch.isMuted) {
                                Text(
                                    text = "MUTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Visualiser(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            wavedata = wave,
                            lineColor = color
                        )
                    }
                }
            }
        }

        // Master Mixed Visualiser
        Text(
            text = "Master Mixed Output Visualiser",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Everything Mixed Together",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF7C4DFF)
                )

                Visualiser(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    wavedata = waveformData.mixed,
                    lineColor = Color(0xFF7C4DFF)
                )
            }
        }
    }
}
