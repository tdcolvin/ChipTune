package com.example.chiptune.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class PianoNote(
    val name: String,
    val frequency: Float,
    val isBlack: Boolean
)

// 1-Octave scale from C4 (261.63 Hz) to C5 (523.25 Hz)
val OCTAVE_NOTES = listOf(
    PianoNote("C4", 261.63f, isBlack = false),
    PianoNote("C#4", 277.18f, isBlack = true),
    PianoNote("D4", 293.66f, isBlack = false),
    PianoNote("D#4", 311.13f, isBlack = true),
    PianoNote("E4", 329.63f, isBlack = false),
    PianoNote("F4", 349.23f, isBlack = false),
    PianoNote("F#4", 369.99f, isBlack = true),
    PianoNote("G4", 392.00f, isBlack = false),
    PianoNote("G#4", 415.30f, isBlack = true),
    PianoNote("A4", 440.00f, isBlack = false),
    PianoNote("A#4", 466.16f, isBlack = true),
    PianoNote("B4", 493.88f, isBlack = false),
    PianoNote("C5", 523.25f, isBlack = false)
)

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
