package com.example.chiptune.ui.screens.smb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chiptune.ChiptuneSynthesizer
import com.example.chiptune.SynthType
import com.example.chiptune.WaveformData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class SmbViewModel : ViewModel() {
    private val synth = ChiptuneSynthesizer()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val waveformData: StateFlow<WaveformData> = synth.waveformData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WaveformData()
        )

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true

        synth.playOnce(
            sequence = synth.smbMelody,
            synthType = SynthType.Square,
            onComplete = {
                _isPlaying.value = false
            }
        )
    }

    override fun onCleared() {
        synth.stop()
    }
}
