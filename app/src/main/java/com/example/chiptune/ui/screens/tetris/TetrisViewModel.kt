package com.example.chiptune.ui.screens.tetris

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chiptune.AudioChannel
import com.example.chiptune.ChiptuneSynthesizer
import com.example.chiptune.SynthType
import com.example.chiptune.WaveformData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TetrisViewModel : ViewModel() {
    private val synth = ChiptuneSynthesizer()

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
        synth.start(synthType)
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
