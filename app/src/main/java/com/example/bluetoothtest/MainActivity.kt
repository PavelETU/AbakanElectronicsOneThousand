package com.example.bluetoothtest

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
import android.widget.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bluetoothtest.theme.ComposeMaterial3TestTheme
import com.example.bluetoothtest.utils.getStringForCompose
import com.example.bluetoothtest.utils.getStringFromResource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

    @SuppressLint("MissingPermission")
    val registerToPairDevice =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            val deviceToPair: BluetoothDevice? =
                it.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
            deviceToPair?.createBond()
            viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        }

    private fun showEnableBluetoothNextTime() {
        Toast.makeText(this, getString(R.string.enable_bluetooth_next_time), Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkBluetoothPermissions()
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
        viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        setContent {
            ComposeMaterial3TestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ControlPanel(bluetoothAdapter)
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
    scope.launch {
        viewModel.toastMessage.collect {
            snackbarHostState.showSnackbar(context.getStringFromResource(it))
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, content = {
        ControlPanelInsideScaffold(bluetoothAdapter, viewModel)
    })
}

@Composable
private fun ControlPanelInsideScaffold(bluetoothAdapter: BluetoothAdapter,
                                       viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    @SuppressLint("MissingPermission")
    val registerToPairDevice =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            val deviceToPair: BluetoothDevice? =
                it.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
            deviceToPair?.createBond()
            viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        }

    Box(modifier = Modifier.fillMaxSize()) {
        val resourceWithFormatting = viewModel.outputMessage.collectAsState().value
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
            override fun onDeviceFound(chooserLauncher: IntentSender?) {
                registerToPairDevice.launch(
                    IntentSenderRequest.Builder(chooserLauncher!!).build()
                )
            }

            override fun onFailure(error: CharSequence?) {
            }
        },
        null
    )
}