package com.abakan.electronics.one.thousand

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource

@Composable
fun Tuner(viewModel: MainViewModel) {
    AlertDialog(onDismissRequest = { viewModel.tuningDismissed() },
        dismissButton = { TextButton(
        onClick = { viewModel.tuningDismissed() }) {
        Text(text = stringResource(id = R.string.close_tuner))
    } }, confirmButton = { }, text = { Text(text = viewModel.leadingFrequency.collectAsState().value.toString()) })
}