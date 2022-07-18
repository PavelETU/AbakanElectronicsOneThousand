package com.example.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.io.ByteSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.*

private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

class MainViewModel: ViewModel() {
    private var bluetoothSocket: BluetoothSocket? = null
    val messageToDisplay = MutableSharedFlow<String>()
    val outputMessage = MutableStateFlow("")

    @SuppressLint("MissingPermission")
    fun connectDevice(bluetoothAdapter: BluetoothAdapter, device: BluetoothDevice) {
        bluetoothAdapter.cancelDiscovery()
        bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                bluetoothSocket!!.connect()
                messageToDisplay.emit("Device Connected")
                val inputStream = bluetoothSocket!!.inputStream
                var char: Int
                while (true) {
                    char = try {
                        inputStream.read()
                    } catch (t: Throwable) {
                        messageToDisplay.emit("Steam was interrupted")
                        break
                    }
                    outputMessage.value = outputMessage.value + char.toChar()
                }
            } catch (t: Throwable) {
                messageToDisplay.emit("Error while connecting to the device")
            }
        }
    }

    fun sendCommand(command: String) {
        if (bluetoothSocket == null) {
            messageToDisplay.tryEmit("Connect your device first!")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val outputStream = bluetoothSocket!!.outputStream
            try {
                outputStream.write(command.toByteArray())
            } catch (t: Throwable) {
                messageToDisplay.emit("Error while sending the command")
            }
        }
    }

    override fun onCleared() {
        try {
            bluetoothSocket?.close()
            Log.i(javaClass.name, "Socket is closed")
        } catch (t: Throwable) {
            Log.e(javaClass.name, "Can't close the socket cause ${t.message}")
        }
    }
}