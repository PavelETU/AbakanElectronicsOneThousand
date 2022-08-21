package com.abakan.electronics.one.thousand

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abakan.electronics.one.thousand.theme.AbakanElectronicsTheme
import com.abakan.electronics.one.thousand.utils.getStringForCompose
import com.abakan.electronics.one.thousand.utils.getStringFromResource
import dagger.hilt.android.AndroidEntryPoint
import java.util.regex.Pattern

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val viewModel: MainViewModel by viewModels()

    private val registerToGrantPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it[Manifest.permission.BLUETOOTH_CONNECT] == true && it[Manifest.permission.BLUETOOTH_SCAN] == true) {
                onBluetoothPermissionsGranted()
            } else {
                showEnableBluetoothNextTime()
            }
        }
    private val registerToEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                onBluetoothEnabled()
            } else {
                showEnableBluetoothNextTime()
            }
        }

    private fun showEnableBluetoothNextTime() {
        Toast.makeText(this, getString(R.string.enable_bluetooth_next_time), Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkBluetoothPermissions()
        if (intent.action == Intent.ACTION_VIEW) {
            viewModel.connectDevice()
        }
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (bluetoothConnectIsGranted() && bluetoothScanIsGranted()) {
                onBluetoothPermissionsGranted()
            } else {
                registerToGrantPermission.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            }
        } else {
            onBluetoothPermissionsGranted()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun bluetoothConnectIsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

    @RequiresApi(Build.VERSION_CODES.S)
    private fun bluetoothScanIsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

    private fun onBluetoothPermissionsGranted() {
        val bluetoothManager: BluetoothManager =
            getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (!bluetoothAdapter.isEnabled) {
            registerToEnableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            onBluetoothEnabled()
        }
    }

    @SuppressLint("MissingPermission")
    private fun onBluetoothEnabled() {
        setContent {
            AbakanElectronicsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ControlPanel(bluetoothAdapter, viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlPanel(
    bluetoothAdapter: BluetoothAdapter,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var onBluetoothEnabledCalled by rememberSaveable { mutableStateOf(false) }
    if (!onBluetoothEnabledCalled) {
        viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        onBluetoothEnabledCalled = true
    }
    LaunchedEffect(Unit) {
        viewModel.disappearingMessage.collect {
            snackbarHostState.showSnackbar(context.getStringFromResource(it))
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, content = {
        ControlPanelInsideScaffold(bluetoothAdapter, viewModel, it)
    })
}

@Composable
private fun ControlPanelInsideScaffold(bluetoothAdapter: BluetoothAdapter,
                                       viewModel: MainViewModel = viewModel(), paddingValues: PaddingValues) {
    val context = LocalContext.current
    @SuppressLint("MissingPermission")
    val registerToPairDevice =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            val deviceToPair: BluetoothDevice? =
                it.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
            deviceToPair?.createBond()
            viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        }

    ShowTunerWhenNeeded(viewModel)
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)) {
        val resourceWithFormatting = viewModel.outputMessage.collectAsState().value
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 20.dp), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { viewModel.startStopRecording(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)) },
                modifier = Modifier
                    .weight(1.0F)
                    .padding(8.dp)) {
                Text(text = stringResource(id = viewModel.recordingButtonResource.collectAsState().value))
            }
            Button(onClick = { viewModel.startTuning() },
                modifier = Modifier
                    .weight(1.0F)
                    .padding(8.dp)) {
                Text(text = stringResource(if (SPECTROGRAM_OVER_TUNER) R.string.spectrogram else R.string.tune))
            }
        }
        Text(text = resourceWithFormatting.getStringForCompose(), Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)) {
            Button(onClick = { pairNewDevice(context, registerToPairDevice) },
                Modifier
                    .weight(1f)
                    .padding(8.dp)) {
                Text(text = stringResource(id = R.string.pair_new_device))
            }
            Button(onClick = { viewModel.connectDevice() },
                Modifier
                    .weight(1f)
                    .padding(8.dp)) {
                Text(text = stringResource(id = R.string.connect))
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

private fun pairNewDevice(
    context: Context,
    registerToPairDevice: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>
) {
    val pairingRequest =
        AssociationRequest.Builder().addDeviceFilter(
            BluetoothDeviceFilter.Builder().setNamePattern(
                Pattern.compile(NAME_OF_THE_DEVICE)
            ).build()
        )
            .build()
    val companionDeviceManager =
        context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
    companionDeviceManager.associate(
        pairingRequest,
        object : CompanionDeviceManager.Callback() {
            override fun onDeviceFound(chooserLauncher: IntentSender) {
                registerToPairDevice.launch(
                    IntentSenderRequest.Builder(chooserLauncher).build()
                )
            }

            override fun onFailure(error: CharSequence?) {
            }
        },
        null
    )
}