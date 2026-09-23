package com.example.chiptune.ui.screens.waves

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chiptune.ui.components.Visualiser

@Composable
fun WavesScreen(
    modifier: Modifier = Modifier,
    viewModel: WavesViewModel = viewModel()
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val selectedWaveform by viewModel.selectedWaveform.collectAsState()
    val currentNote by viewModel.currentNote.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val dutyCycle by viewModel.dutyCycle.collectAsState()
    val waveform by viewModel.waveform.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Waves & Formulas Synthesizer",
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
                        text = if (isPlaying && currentNote != null) "Playing ${currentNote?.name} (${currentNote?.frequency?.toInt()} Hz)" else "Stopped",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isPlaying) Color(0xFF00E676) else MaterialTheme.colorScheme.outline
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

        // Waveform Selector & Formula Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Waveform Selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WaveformType.entries.forEach { waveType ->
                        val isSelected = selectedWaveform == waveType
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setWaveform(waveType) },
                            label = { Text(waveType.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Formula Details Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val formulaTitle = if (selectedWaveform == WaveformType.SQUARE) {
                            "Square Wave Formula (${(dutyCycle * 100).toInt()}% Duty Cycle)"
                        } else {
                            selectedWaveform.formulaTitle
                        }

                        val formulaExpr = if (selectedWaveform == WaveformType.SQUARE) {
                            "y(t) = if ((t × f) mod 1.0 < ${(dutyCycle * 100).toInt()}%) A else -A"
                        } else {
                            selectedWaveform.formulaExpression
                        }

                        Text(
                            text = formulaTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formulaExpr,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = selectedWaveform.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Signal Parameters Card (Amplitude Slider + Duty Cycle for Square)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Signal Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Amplitude Slider
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Amplitude",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${(amplitude * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = amplitude,
                        onValueChange = { viewModel.setAmplitude(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Duty Cycle Slider (Only for Square Wave)
                if (selectedWaveform == WaveformType.SQUARE) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Duty Cycle",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${(dutyCycle * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = dutyCycle,
                            onValueChange = { viewModel.setDutyCycle(it) },
                            valueRange = 0.05f..0.95f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Play / Stop Button
        Button(
            onClick = { viewModel.togglePlay() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = if (isPlaying) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            }
        ) {
            Text(
                text = if (isPlaying) "Stop" else "Play Scale",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
