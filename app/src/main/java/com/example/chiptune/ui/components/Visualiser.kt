package com.example.chiptune.ui.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun Visualiser(
    modifier: Modifier = Modifier,
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
