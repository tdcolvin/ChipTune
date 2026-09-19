package com.example.chiptune

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.chiptune.ui.theme.ChipTuneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChipTuneTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val synth = remember { ChiptuneSynthesizer() }
    val waveData by synth.currentWaveform.collectAsState(initial = ShortArray(0))
    val channelList = remember { mutableStateListOf<AudioChannel>() }

    fun updateChannelList() {
        channelList.clear()
        channelList.addAll(synth.channels)
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(onClick = { synth.start(SynthType.Sine); updateChannelList() }) { Text("Sine") }
            Button(onClick = { synth.start(SynthType.Square); updateChannelList() }) { Text("Square") }
            Button(onClick = { synth.start(SynthType.Sawtooth); updateChannelList() }) { Text("Saw") }
            Button(onClick = { synth.start(SynthType.Fm2op); updateChannelList() }) { Text("FM") }
            Button(onClick = { synth.start(SynthType.Opl2); updateChannelList() }) { Text("OPL2") }
        }
        Button(
            onClick = {
                synth.stop()
                updateChannelList()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Stop")
        }

        if (channelList.isNotEmpty()) {
            Text("Channels:", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                channelList.forEach { ch ->
                    OutlinedButton(
                        onClick = {
                            ch.isMuted = !ch.isMuted
                            updateChannelList()
                        }
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

@Composable
fun Visualiser(
    modifier: Modifier,
    wavedata: ShortArray
) {
    val samples = 100
    val firstNegative = wavedata.indexOfFirst { it < 0 }
    val positiveCrossing = if (firstNegative < 0) -1 else wavedata.drop(firstNegative).indexOfFirst { it >= 0 }
    val zeroCrossing = if (positiveCrossing < 0) -1 else positiveCrossing + firstNegative

    Log.v("plot", "firstNegative=$firstNegative, positiveCrossing=$positiveCrossing, zeroCrossing=$zeroCrossing")

    Canvas(modifier = modifier) {
        val zeroY = size.height / 2
        val path = Path()
        path.moveTo(0f, zeroY)
        val plotdata = if (zeroCrossing < 0) listOf() else wavedata.drop(zeroCrossing).take(samples)
        plotdata.forEachIndexed { index, amplitude ->
            path.lineTo(index * size.width / plotdata.size.toFloat(), (amplitude / Short.MAX_VALUE.toFloat()) * size.height + zeroY)
        }
        drawPath(path, Color.Green, style = Stroke(width = 7f))

        drawLine(Color.Red, Offset(0f, zeroY), Offset(size.width, zeroY))
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChipTuneTheme {
        Greeting()
    }
}
