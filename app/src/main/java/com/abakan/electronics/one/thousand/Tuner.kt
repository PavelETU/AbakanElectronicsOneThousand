package com.abakan.electronics.one.thousand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun Tuner(viewModel: MainViewModel) {
    AlertDialog(onDismissRequest = { viewModel.tuningDismissed() },
        dismissButton = {
            TextButton(
                onClick = { viewModel.tuningDismissed() }) {
                Text(text = stringResource(id = R.string.close_tuner))
            }
        }, confirmButton = { }, text = { if (SPECTROGRAM_OVER_TUNER) {
            DrawSpectrogram(viewModel)
        } else DrawTuner(viewModel) })
}

@Composable
private fun DrawSpectrogram(viewModel: MainViewModel) {
    val frequencies = viewModel.spectrogram.collectAsState().value
    Spectrogram(frequencies)
}

@Composable
private fun Spectrogram(frequencies: List<Double>) {
    if (frequencies.isEmpty()) {
        return
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val sampleSize = frequencies.size
        val maxMagnitudeValue = frequencies.max()
        val frequencyOffset = canvasWidth/ (sampleSize - 1)
        val heightForOneValueOfMag = canvasHeight / maxMagnitudeValue

        frequencies.forEachIndexed { index, value ->
            drawLine(
                start = Offset(x = index * frequencyOffset, y = (canvasHeight - (value * heightForOneValueOfMag)).toFloat()),
                end = Offset(x = index * frequencyOffset, y = canvasHeight),
                color = Color.Red
            )
        }
    }
}

@Composable
fun DrawTuner(viewModel: MainViewModel) {
    Text(text = viewModel.leadingFrequency.collectAsState().value.toString())
}

@Preview
@Composable
fun Preview() {
    Spectrogram(frequencies = listOf(0.0, 1.0, 2.0, 3.0, 10.0, 8.0))
}