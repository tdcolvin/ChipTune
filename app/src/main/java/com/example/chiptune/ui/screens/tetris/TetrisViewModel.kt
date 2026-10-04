package com.example.chiptune.ui.screens.tetris

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chiptune.AudioChannel
import com.example.chiptune.ChiptuneSynthesizer
import com.example.chiptune.SynthType
import com.example.chiptune.TetrisSong
import com.example.chiptune.WaveformData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TetrisViewModel : ViewModel() {
    private val synth = ChiptuneSynthesizer()

    val tetrisBass = TetrisSong.tetrisBass
    val tetris = TetrisSong.tetrisMelody

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

    var isPlaying = false

    fun startPercussion() {
        if (!isPlaying) {
            startSynthSilent()
        }
        setChannelMute("Percussion", false)
    }

    fun startBass() {
        if (!isPlaying) {
            startSynthSilent()
        }
        setChannelMute("Bass", false)
    }

    fun startMelody() {
        if (!isPlaying) {
            startSynthSilent()
        }
        setChannelMute("Lead", false)
    }

    private fun startSynthSilent() {
        synth.start(SynthType.Square, leadSequence = tetris, bassSequence = tetrisBass)
        isPlaying = true
        synth.channels.forEach { it.isMuted = true }
        updateChannelList()
    }

    fun stopSynth() {
        synth.stop()
        isPlaying = false
        updateChannelList()
    }

    fun toggleMute(channel: AudioChannel) {
        channel.isMuted = !channel.isMuted
        updateChannelList()
    }

    private fun setChannelMute(name: String, muted: Boolean) {
        synth.channels.find { it.name == name }?.isMuted = muted
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
