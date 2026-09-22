package com.example.chiptune.ui.screens.tetris

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chiptune.AudioChannel
import com.example.chiptune.ChiptuneSynthesizer
import com.example.chiptune.NOTE_A2
import com.example.chiptune.NOTE_A3
import com.example.chiptune.NOTE_A4
import com.example.chiptune.NOTE_A5
import com.example.chiptune.NOTE_B2
import com.example.chiptune.NOTE_B3
import com.example.chiptune.NOTE_B4
import com.example.chiptune.NOTE_C3
import com.example.chiptune.NOTE_C4
import com.example.chiptune.NOTE_C5
import com.example.chiptune.NOTE_D3
import com.example.chiptune.NOTE_D4
import com.example.chiptune.NOTE_D5
import com.example.chiptune.NOTE_E3
import com.example.chiptune.NOTE_E4
import com.example.chiptune.NOTE_E5
import com.example.chiptune.NOTE_F5
import com.example.chiptune.NOTE_G5
import com.example.chiptune.Note
import com.example.chiptune.REST
import com.example.chiptune.SynthType
import com.example.chiptune.WaveformData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TetrisViewModel : ViewModel() {
    private val synth = ChiptuneSynthesizer()

    val tetrisBass = listOf(
        Note(NOTE_E3, 0.5f),
        Note(NOTE_E4, 0.5f),
        Note(NOTE_E3, 0.5f),
        Note(NOTE_E4, 0.5f),
        Note(NOTE_E3, 0.5f),
        Note(NOTE_E4, 0.5f),
        Note(NOTE_E3, 0.5f),
        Note(NOTE_E4, 0.5f),

        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),

        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),

        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),

        Note(NOTE_D3, 0.5f),
        Note(NOTE_D4, 0.5f),
        Note(NOTE_D3, 0.5f),
        Note(NOTE_D4, 0.5f),
        Note(NOTE_D3, 0.5f),
        Note(NOTE_D4, 0.5f),
        Note(NOTE_D3, 0.5f),
        Note(NOTE_D4, 0.5f),

        Note(NOTE_C3, 0.5f),
        Note(NOTE_C4, 0.5f),
        Note(NOTE_C3, 0.5f),
        Note(NOTE_C4, 0.5f),
        Note(NOTE_C3, 0.5f),
        Note(NOTE_C4, 0.5f),
        Note(NOTE_C3, 0.5f),
        Note(NOTE_C4, 0.5f),

        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),

        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
    )

    val tetris = listOf(
        // Measure 1
        Note(NOTE_E5, 1.0f),
        Note(NOTE_B4, 0.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_D5, 1.0f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_B4, 0.5f),

        // Measure 2
        Note(NOTE_A4, 1.0f),
        Note(NOTE_A4, 0.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_E5, 1.0f),
        Note(NOTE_D5, 0.5f),
        Note(NOTE_C5, 0.5f),

        // Measure 3
        Note(NOTE_B4, 1.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_D5, 1.0f),
        Note(NOTE_E5, 1.0f),

        // Measure 4
        Note(NOTE_C5, 1.0f),
        Note(NOTE_A4, 1.0f),
        Note(NOTE_A4, 1.0f),
        Note(REST, 1.0f),

        // Measure 5
        Note(REST, 0.5f),
        Note(NOTE_D5, 1.0f),
        Note(NOTE_F5, 0.5f),
        Note(NOTE_A5, 1.0f),
        Note(NOTE_G5, 0.5f),
        Note(NOTE_F5, 0.5f),

        // Measure 6
        Note(NOTE_E5, 1.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_E5, 1.0f),
        Note(NOTE_D5, 0.5f),
        Note(NOTE_C5, 0.5f),

        // Measure 7
        Note(NOTE_B4, 1.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_D5, 1.0f),
        Note(NOTE_E5, 1.0f),

        // Measure 8
        Note(NOTE_C5, 1.0f),
        Note(NOTE_A4, 1.0f),
        Note(NOTE_A4, 1.0f),
        Note(REST, 1.0f),
    )

    val waveData: StateFlow<FloatArray> = synth.currentWaveform
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FloatArray(0)
        )

    val waveformData: StateFlow<WaveformData> = synth.waveformData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WaveformData()
        )

    val channelList = mutableStateListOf<AudioChannel>()

    fun startSynth(synthType: SynthType) {
        synth.start(synthType, leadSequence = tetris, bassSequence = tetrisBass)
        updateChannelList()
    }

    fun stopSynth() {
        synth.stop()
        updateChannelList()
    }

    fun toggleMute(channel: AudioChannel) {
        channel.isMuted = !channel.isMuted
        updateChannelList()
    }

    private fun updateChannelList() {
        channelList.clear()
        channelList.addAll(synth.channels)
    }

    override fun onCleared() {
        synth.stop()
    }
}
