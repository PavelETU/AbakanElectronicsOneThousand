package com.example.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.media.AudioTrack
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("MissingPermission")
@HiltViewModel
class MainViewModel @Inject constructor(
    private val audioTrackProvider: AudioTrackProvider
) : ViewModel() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var audioTrack: AudioTrack? = null
    val toastMessage = MutableSharedFlow<String>()
    val outputMessage = MutableStateFlow("Device not connected")

    fun onBluetoothEnabledOrDeviceBonded(bluetoothAdapter: BluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter
        if (bluetoothAdapter.bondedDevices.none { it.name.contains(NAME_OF_THE_DEVICE) }) {
            outputMessage.tryEmit("You don't have any $READABLE_NAME_OF_THE_DEVICE devices paired. Turn on the device and click Pair New Device")
        } else {
            outputMessage.tryEmit("$READABLE_NAME_OF_THE_DEVICE ready to connect. Turn the device on and click Connect")
        }
    }

    fun connectDevice(ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
        viewModelScope.launch(ioDispatcher) {
            if (bluetoothAdapter == null) {
                toastMessage.emit("Bluetooth was not enabled. Restart the app and accept all permissions and turn Bluetooth on")
                return@launch
            }
            bluetoothAdapter!!.cancelDiscovery()
            val device =
                bluetoothAdapter!!.bondedDevices.filter { it.name.contains(NAME_OF_THE_DEVICE) }
                    .getOrNull(0)
            if (device == null) {
                toastMessage.emit("No $READABLE_NAME_OF_THE_DEVICE device found")
                return@launch
            }
            prepareAudioTrack()
            bluetoothSocket =
                device.createRfcommSocketToServiceRecord(DEFAULT_UUID_FOR_CUSTOM_DEVICES)
            try {
                outputMessage.emit("Connecting ${device.name} device...")
                bluetoothSocket!!.connect()
                outputMessage.emit("Device ${device.name} connected")
                val inputStream = bluetoothSocket!!.inputStream
                var numberOfByte = 0
                var overallBytes = 0L
                val bytes = ByteArray(160)
                var seconds = System.currentTimeMillis() / 1000
                while (true) {
                    val currentUByte = try {
                        inputStream.read().toUByte()
                    } catch (t: Throwable) {
                        toastMessage.emit("Steam was interrupted")
                        outputMessage.emit("Device ${device.name} not connected")
                        break
                    }
                    overallBytes++
                    val newSeconds = System.currentTimeMillis() / 1000
                    if (newSeconds > seconds) {
                        Log.i("BytesPerSecond", overallBytes.toString())
                        overallBytes = 0
                        seconds = newSeconds
                    }
                    bytes[numberOfByte] = (currentUByte.toInt() - 128).toByte()
                    numberOfByte++
                    if (numberOfByte == 160) {
                        audioTrack!!.write(bytes, 0, bytes.size)
                        numberOfByte = 0
                    }
                }
            } catch (t: Throwable) {
                outputMessage.emit("Device ${device.name} not connected")
                toastMessage.emit("Error while connecting to the device")
            }
        }
    }

    private fun prepareAudioTrack() {
        audioTrack = audioTrackProvider.getAudioTrack()
        audioTrack!!.play()
    }

    override fun onCleared() {
        try {
            audioTrack?.stop()
            bluetoothSocket?.close()
            Log.i(javaClass.name, "Socket is closed")
        } catch (t: Throwable) {
            Log.e(javaClass.name, "Can't close the socket cause ${t.message}")
        }
    }
}