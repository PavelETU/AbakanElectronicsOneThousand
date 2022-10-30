@file:OptIn(ExperimentalMaterial3Api::class)

package com.abakan.electronics.one.thousand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abakan.electronics.one.thousand.utils.fourier_transform.CooleyTurkeyFFT
import com.abakan.electronics.one.thousand.utils.fourier_transform.FourierTransformHelperImpl

@Composable
fun Tuner(viewModel: MainViewModel) {
    AlertDialog(onDismissRequest = { viewModel.tuningDismissed() },
        dismissButton = {
            TextButton(
                onClick = { viewModel.tuningDismissed() }) {
                Text(
                    text = stringResource(
                        id = if (SPECTROGRAM_OVER_TUNER)
                            R.string.close_spectrogram
                        else R.string.close_tuner
                    )
                )
            }
        }, confirmButton = { }, text = {
            if (SPECTROGRAM_OVER_TUNER) {
                Spectrogram(viewModel)
            } else DrawTuner(viewModel)
        })
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Spectrogram(viewModel: MainViewModel) {
    val frequencies = viewModel.spectrogram.collectAsState().value
    if (frequencies.isEmpty()) {
        return
    }
    var minFrequency by rememberSaveable { mutableStateOf("0.0") }
    var maxFrequency by rememberSaveable { mutableStateOf("4000.0") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = minFrequency,
                onValueChange = { minFrequency = it },
                Modifier
                    .weight(1.0f)
                    .padding(5.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.setMinSpectrumFrequency(minFrequency)
                }),
                label = {
                    Text(
                        "Min Frequency",
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                })
            OutlinedTextField(
                value = maxFrequency,
                onValueChange = { maxFrequency = it },
                Modifier
                    .weight(1.0f)
                    .padding(5.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.setMaxSpectrumFrequency(maxFrequency)
                }),
                label = {
                    Text(
                        "Max Frequency",
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                })
        }
        Text(text = "Max Frequency at ${viewModel.maxFrequency.collectAsState().value}")
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val sampleSize = frequencies.size
            val maxMagnitudeValue = frequencies.max()
            val frequencyOffset = canvasWidth / (sampleSize - 1)
            val heightForOneValueOfMag = canvasHeight / maxMagnitudeValue

            frequencies.forEachIndexed { index, value ->
                drawLine(
                    start = Offset(
                        x = index * frequencyOffset,
                        y = (canvasHeight - (value * heightForOneValueOfMag)).toFloat()
                    ),
                    end = Offset(x = index * frequencyOffset, y = canvasHeight),
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
fun DrawTuner(viewModel: MainViewModel) {
    Text(text = viewModel.leadingFrequency.collectAsState().value.toString())
}

//@Preview
//@Composable
//fun Preview() {
//    val current = LocalContext.current
//    Spectrogram(
//        viewModel = MainViewModel(
//            current,
//            AudioTrackProviderImpl(),
//            FourierTransformHelperImpl(CooleyTurkeyFFT())
//        )
//    )
//}