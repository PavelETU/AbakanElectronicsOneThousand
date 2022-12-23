package com.abakan.electronics.one.thousand

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abakan.electronics.one.thousand.theme.AbakanElectronicsTheme
import com.abakan.electronics.one.thousand.utils.getStringForCompose
import com.abakan.electronics.one.thousand.utils.getStringFromResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AbakanElectronicsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ControlPanel(viewModel)
                }
            }
        }
        if (intent.extras?.getString("feature") == getString(R.string.connect_device_feature)) {
            viewModel.onConnectionActionReceived()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_VIEW) {
            when (intent.extras?.getString("feature")) {
                getString(R.string.connect_device_feature) -> viewModel.connectDisconnectDevice()
                getString(R.string.start_recording_feature),
                getString(R.string.stop_recording_feature) ->
                    viewModel.startStopRecording(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    )
                else -> Toast.makeText(this, getString(R.string.no_feature), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlPanel(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        viewModel.disappearingMessage.collect {
            snackbarHostState.showSnackbar(context.getStringFromResource(it))
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, content = {
        ControlPanelInsideScaffold(viewModel, it)
    })
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ControlPanelInsideScaffold(
    viewModel: MainViewModel = viewModel(), paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val recordPermission = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)

    ShowTunerWhenNeeded(viewModel)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        val resourceWithFormatting = viewModel.outputMessage.collectAsState().value
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 20.dp), horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    viewModel.startStopRecording(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_MUSIC
                        )
                    )
                },
                modifier = Modifier
                    .weight(1.0F)
                    .padding(8.dp)
            ) {
                Text(text = stringResource(id = viewModel.recordingButtonResource.collectAsState().value))
            }
            Button(
                onClick = { viewModel.startTuning() },
                modifier = Modifier
                    .weight(1.0F)
                    .padding(8.dp)
            ) {
                Text(text = stringResource(if (SPECTROGRAM_OVER_TUNER) R.string.spectrogram else R.string.tune))
            }
        }
        Text(
            text = resourceWithFormatting.getStringForCompose(),
            Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center
        )
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            val mainButtonTitleState = viewModel.mainButtonTitle.collectAsState()
            Button(
                onClick = { if (recordPermission.status.isGranted) {
                    viewModel.connectDisconnectDevice()
                } else {
                    recordPermission.launchPermissionRequest()
                } },
                Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                Text(text = mainButtonTitleState.value)
            }
        }
    }
}

@Composable
private fun ShowTunerWhenNeeded(viewModel: MainViewModel) {
    if (viewModel.showTuner.collectAsState().value) {
        Tuner(viewModel)
    }
}
