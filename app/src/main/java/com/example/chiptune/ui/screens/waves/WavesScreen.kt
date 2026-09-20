package com.example.chiptune.ui.screens.waves

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chiptune.ui.components.Visualiser

@Composable
fun WavesScreen(
    modifier: Modifier = Modifier,
    viewModel: WavesViewModel = viewModel()
) {
    val selectedWaveform by viewModel.selectedWaveform.collectAsState()
    val activeNote by viewModel.activeNote.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
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
                        text = activeNote?.let { "Playing: ${it.name} (${it.frequency.toInt()} Hz)" }
                            ?: "Idle (A4 440 Hz)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (activeNote != null) Color(0xFF00E676) else MaterialTheme.colorScheme.outline
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
                        Text(
                            text = selectedWaveform.formulaTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = selectedWaveform.formulaExpression,
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
                        text = "1-Octave Keyboard (Monophonic)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hold key down to play",
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

        // Signal Volume Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
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
        }
    }
}

@Composable
fun PianoKeyboard(
    notes: List<PianoNote>,
    activeNote: PianoNote?,
    onNoteDown: (PianoNote) -> Unit,
    onNoteUp: (PianoNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val whiteNotes = notes.filter { !it.isBlack }
    val blackNotes = notes.filter { it.isBlack }

    // Map black notes to the index of white note they sit right after
    val blackNotePositions = mapOf(
        "C#4" to 1,
        "D#4" to 2,
        "F#4" to 4,
        "G#4" to 5,
        "A#4" to 6
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
            .padding(6.dp)
    ) {
        val totalWhiteKeys = whiteNotes.size
        val keySpacing = 2.dp
        val availableWidth = maxWidth - (keySpacing * (totalWhiteKeys - 1))
        val whiteKeyWidth = availableWidth / totalWhiteKeys
        val blackKeyWidth = whiteKeyWidth * 0.62f
        val blackKeyHeight = maxHeight * 0.60f

        // 1. White Keys Row
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(keySpacing)
        ) {
            whiteNotes.forEach { note ->
                val isPressed = activeNote == note
                WhiteKey(
                    note = note,
                    isPressed = isPressed,
                    width = whiteKeyWidth,
                    onNoteDown = onNoteDown,
                    onNoteUp = onNoteUp
                )
            }
        }

        // 2. Black Keys Overlay
        blackNotes.forEach { note ->
            val whiteIndexAfter = blackNotePositions[note.name] ?: return@forEach
            val isPressed = activeNote == note
            val xOffset = (whiteKeyWidth + keySpacing) * whiteIndexAfter - (blackKeyWidth / 2) - (keySpacing / 2)

            BlackKey(
                note = note,
                isPressed = isPressed,
                width = blackKeyWidth,
                height = blackKeyHeight,
                xOffset = xOffset,
                onNoteDown = onNoteDown,
                onNoteUp = onNoteUp
            )
        }
    }
}

@Composable
private fun WhiteKey(
    note: PianoNote,
    isPressed: Boolean,
    width: Dp,
    onNoteDown: (PianoNote) -> Unit,
    onNoteUp: (PianoNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isPressed) Color(0xFF81D4FA) else Color(0xFFFAFAFA)
    val borderColor = if (isPressed) Color(0xFF0288D1) else Color(0xFFB0BEC5)

    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .pointerInput(note) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onNoteDown(note)
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    onNoteUp(note)
                }
            }
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = note.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isPressed) Color(0xFF01579B) else Color(0xFF37474F)
        )
    }
}

@Composable
private fun BlackKey(
    note: PianoNote,
    isPressed: Boolean,
    width: Dp,
    height: Dp,
    xOffset: Dp,
    onNoteDown: (PianoNote) -> Unit,
    onNoteUp: (PianoNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isPressed) Color(0xFF0288D1) else Color(0xFF212121)
    val borderColor = if (isPressed) Color(0xFF81D4FA) else Color(0xFF424242)

    Box(
        modifier = modifier
            .offset(x = xOffset, y = 0.dp)
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .pointerInput(note) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onNoteDown(note)
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    onNoteUp(note)
                }
            }
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = note.name.replace("4", ""),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
