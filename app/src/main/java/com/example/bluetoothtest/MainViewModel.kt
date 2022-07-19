package com.example.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_SPEECH
import android.media.AudioAttributes.USAGE_MEDIA
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.*

private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

class MainViewModel : ViewModel() {
    private var bluetoothSocket: BluetoothSocket? = null
    private var audioTrack: AudioTrack? = null
    val messageToDisplay = MutableSharedFlow<String>()
    val outputMessage = MutableStateFlow("Device not connected")

    @SuppressLint("MissingPermission")
    fun connectDevice(bluetoothAdapter: BluetoothAdapter, device: BluetoothDevice) {
        bluetoothAdapter.cancelDiscovery()
        bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
        audioTrack = AudioTrack.Builder().setAudioAttributes(
            AudioAttributes.Builder().setUsage(USAGE_MEDIA).setContentType(CONTENT_TYPE_SPEECH)
                .build()
        ).setTransferMode(AudioTrack.MODE_STREAM).setAudioFormat(
            AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_8BIT).setSampleRate(4000).build()
        )
            .setBufferSizeInBytes(160)
            .build()
        audioTrack!!.play()
        viewModelScope.launch(Dispatchers.IO) {
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
                        messageToDisplay.emit("Steam was interrupted")
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
                messageToDisplay.emit("Error while connecting to the device")
            }
        }
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