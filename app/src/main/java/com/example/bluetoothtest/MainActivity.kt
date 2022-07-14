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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var device: BluetoothDevice
    private lateinit var viewModel: MainViewModel

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
    val registerToPairDevice = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        val deviceToPair: BluetoothDevice? = it.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
        deviceToPair?.createBond()
    }

    private fun showEnableBluetoothNextTime() {
        Toast.makeText(this, "Enable bluetooth next time!", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        lifecycleScope.launchWhenResumed {
            viewModel.messageToDisplay.collect {
                Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
            }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.outputMessage.collect {
                findViewById<TextView>(R.id.output).text = it
            }
        }
        findViewById<Button>(R.id.pair_button).setOnClickListener { pairNewDevice() }
        findViewById<Button>(R.id.connect).setOnClickListener { viewModel.connectDevice(bluetoothAdapter, device) }
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
        device = bluetoothAdapter.bondedDevices.filter { it.name == "HC-05" }[0]
        findViewById<TextView>(R.id.output).text = bluetoothAdapter.bondedDevices.filter { it.name == "HC-05" }.joinToString {
            "Name: ${it.name}\nBluetooth Class: ${it.bluetoothClass.majorDeviceClass.toString(16)}\nAddress: ${it.address}\nType: ${it.type}\nBond State: ${it.bondState}\n"
        }
    }

    private fun pairNewDevice() {
        val pairingRequest =
            AssociationRequest.Builder().addDeviceFilter(BluetoothDeviceFilter.Builder().build())
                .build()
        val companionDeviceManager = getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
        companionDeviceManager.associate(pairingRequest, object : CompanionDeviceManager.Callback() {
            override fun onDeviceFound(chooserLauncher: IntentSender?) {
                registerToPairDevice.launch(IntentSenderRequest.Builder(chooserLauncher!!).build())
            }
            override fun onFailure(error: CharSequence?) {
            }
        }, null)
    }
}