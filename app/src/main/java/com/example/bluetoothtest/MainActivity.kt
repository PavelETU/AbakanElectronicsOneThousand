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
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bluetoothtest.utils.getString
import dagger.hilt.android.AndroidEntryPoint
import java.util.regex.Pattern

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_main)
        observeViewModel()
        setUpViews()
        checkBluetoothPermissions()
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenResumed {
            viewModel.toastMessage.collect {
                Toast.makeText(this@MainActivity, getString(it), Toast.LENGTH_LONG).show()
            }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.outputMessage.collect {
                findViewById<TextView>(R.id.output).text = getString(it)
            }
        }
    }

    private fun setUpViews() {
        findViewById<Button>(R.id.pair_button).setOnClickListener { pairNewDevice() }
        findViewById<Button>(R.id.connect).setOnClickListener {
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
        viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
    }

    private fun pairNewDevice() {
        val pairingRequest =
            AssociationRequest.Builder().addDeviceFilter(
                BluetoothDeviceFilter.Builder().setNamePattern(
                    Pattern.compile(NAME_OF_THE_DEVICE)
                ).build()
            )
                .build()
        val companionDeviceManager =
            getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
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
}