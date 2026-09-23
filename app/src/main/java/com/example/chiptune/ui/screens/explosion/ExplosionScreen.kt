package com.example.chiptune.ui.screens.explosion

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chiptune.PercussionSound
import com.example.chiptune.ui.components.Visualiser

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExplosionScreen(
    modifier: Modifier = Modifier,
    viewModel: ExplosionViewModel = viewModel(),
) {
    val filterEnabled by viewModel.filterEnabled.collectAsState()
    val cutoffFrequency by viewModel.cutoffFrequency.collectAsState()
    val dynamicSweep by viewModel.dynamicSweep.collectAsState()
    val decayDuration by viewModel.decayDuration.collectAsState()
    val loopExplosion by viewModel.loopExplosion.collectAsState()

    val isPlayingPercussion by viewModel.isPlayingPercussion.collectAsState()
    val percussionSound by viewModel.percussionSound.collectAsState()
    val percussionBpm by viewModel.percussionBpm.collectAsState()
    val currentPercussionStep by viewModel.currentPercussionStep.collectAsState()

    val isExplosionMuted by viewModel.isExplosionMuted.collectAsState()
    val isPercussionMuted by viewModel.isPercussionMuted.collectAsState()

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
            text = "Explosion & Noise Synthesis",
            style = MaterialTheme.typography.titleLarge
        )

        // Theory & Introduction Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "8-Bit Arcade Noise & Filter DSP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "White noise contains equal energy across all audible frequencies, producing a sharp television static hiss. " +
                            "In retro games like Space Invaders and Asteroids, explosions are made by passing white noise through a Low-Pass Filter " +
                            "to cut the high-pitched hiss and leaving a deep bass rumble, then applying a decay envelope.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Realtime Oscilloscopes / Visualisers Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Realtime Channel Oscilloscopes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Explosion Channel Visualiser
                val explosionWave = waveformData.channelWaveforms["Explosion"] ?: FloatArray(0)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Explosion Channel Waveform",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                        if (isExplosionMuted) {
                            Text("MUTED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        } else if (!filterEnabled) {
                            Text("RAW HISS (Filter OFF)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("FILTERED (${cutoffFrequency.toInt()} Hz)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E676))
                        }
                    }
                    Visualiser(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        wavedata = explosionWave,
                        lineColor = Color(0xFF00E676)
                    )
                }

                // Percussion Channel Visualiser
                val percussionWave = waveformData.channelWaveforms["Percussion"] ?: FloatArray(0)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Percussion Channel Waveform",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00BCD4)
                        )
                        if (isPercussionMuted) {
                            Text("MUTED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        } else if (isPlayingPercussion) {
                            Text("PLAYING (${percussionBpm.toInt()} BPM)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00BCD4))
                        } else {
                            Text("STOPPED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Visualiser(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        wavedata = percussionWave,
                        lineColor = Color(0xFF00BCD4)
                    )
                }

                // Master Output Visualiser
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Master Mixed Output",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C4DFF)
                    )
                    Visualiser(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        wavedata = waveformData.mixed,
                        lineColor = Color(0xFF7C4DFF)
                    )
                }
            }
        }

        // Explosion Arcade Presets
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Classic Arcade Sound Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExplosionPreset.entries.forEach { preset ->
                        Button(
                            onClick = { viewModel.applyPreset(preset) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(preset.title)
                        }
                    }
                }
            }
        }

        // Explosion Sound Generator Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Explosion Generator Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = { viewModel.toggleExplosionMute() }
                    ) {
                        Text(if (isExplosionMuted) "Unmute" else "Mute")
                    }
                }

                // Big Trigger Button
                Button(
                    onClick = { viewModel.triggerExplosion() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) {
                    Text(
                        text = "💥 TRIGGER EXPLOSION!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Low-Pass Filter Toggle Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Low-Pass Filter (LPF)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (filterEnabled) "Filters out high-pitched hiss (Deep rumble)" else "OFF: Raw harsh white noise static",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (filterEnabled) Color(0xFF00E676) else MaterialTheme.colorScheme.error
                        )
                    }
                    Switch(
                        checked = filterEnabled,
                        onCheckedChange = { viewModel.setFilterEnabled(it) }
                    )
                }

                // Cutoff Frequency Slider
                if (filterEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cutoff Frequency (f_c)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${cutoffFrequency.toInt()} Hz",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = cutoffFrequency,
                            onValueChange = { viewModel.setCutoffFrequency(it) },
                            valueRange = 50f..4000f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = when {
                                cutoffFrequency < 200f -> "Deep sub-bass rumble (Asteroids style)"
                                cutoffFrequency < 600f -> "Medium arcade explosion (Space Invaders style)"
                                cutoffFrequency < 1500f -> "Bright energetic blast"
                                else -> "Harsh unfiltered hiss territory"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Dynamic Pitch/Cutoff Sweep Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dynamic Cutoff Sweep",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sweeps cutoff down from 2500Hz during explosion blast",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dynamicSweep,
                            onCheckedChange = { viewModel.setDynamicSweep(it) }
                        )
                    }
                }

                // Volume Envelope Decay Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Volume Envelope Decay", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${"%.2f".format(decayDuration)} s",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = decayDuration,
                        onValueChange = { viewModel.setDecayDuration(it) },
                        valueRange = 0.1f..3.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Loop Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Continuous Explosion Loop",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = loopExplosion,
                        onCheckedChange = { viewModel.setLoopExplosion(it) }
                    )
                }
            }
        }

        // Percussion Channel Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Percussion Channel (Noise Drums)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = { viewModel.togglePercussionMute() }
                    ) {
                        Text(if (isPercussionMuted) "Unmute" else "Mute")
                    }
                }

                // Play / Stop Percussion Beat
                Button(
                    onClick = { viewModel.togglePercussionPlay() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = if (isPlayingPercussion) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    }
                ) {
                    Text(
                        text = if (isPlayingPercussion) "⏹ STOP PERCUSSION BEAT" else "▶ PLAY PERCUSSION BEAT",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Sound Selector Chips
                Text("Sound Preset Selection:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PercussionSound.entries.forEach { sound ->
                        val isSelected = percussionSound == sound
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setPercussionSound(sound) },
                            label = { Text(sound.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // BPM Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tempo (BPM)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${percussionBpm.toInt()} BPM",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = percussionBpm.toFloat(),
                        onValueChange = { viewModel.setPercussionBpm(it.toDouble()) },
                        valueRange = 60f..220f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 16-Step Grid Visualizer
                Text("16-Step Beat Grid Visualizer:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (step in 0 until 16) {
                        val isActive = isPlayingPercussion && (currentPercussionStep == step)
                        val bgColor by animateColorAsState(
                            if (isActive) Color(0xFF00BCD4) else MaterialTheme.colorScheme.surface,
                            label = "stepColor"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .background(bgColor, shape = RoundedCornerShape(4.dp))
                                .border(
                                    1.dp,
                                    if (isActive) Color.White else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (step + 1).toString(),
                                fontSize = 10.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // DSP Equations & Explanation Footer Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DSP Equations & Audio Physics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Single-pole Low-Pass Filter:  y[n] = y[n-1] + α · (x[n] - y[n-1])",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Smoothing factor α = Δt / (RC + Δt), where RC = 1 / (2π · f_c).",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Volume Decay Envelope:  E(t) = exp(-3.0 · t / T_decay)",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
