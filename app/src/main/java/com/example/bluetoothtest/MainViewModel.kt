package com.example.bluetoothtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.media.AudioTrack
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothtest.utils.HeaderForWavFile
import com.example.bluetoothtest.utils.ResourceWithFormatting
import com.example.bluetoothtest.utils.shiftValuesByZeroOffset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("MissingPermission")
@HiltViewModel
class MainViewModel @Inject constructor(
    private val audioTrackProvider: AudioTrackProvider
) : ViewModel() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var audioTrack: AudioTrack? = null
    private val audioChannel = Channel<ByteArray>(4000)
    private var recording = false
    private var shouldSendBytesForRecord = false
    val toastMessage = MutableSharedFlow<ResourceWithFormatting>()
    val outputMessage = MutableStateFlow(ResourceWithFormatting(R.string.app_name, " for $READABLE_NAME_OF_THE_DEVICE"))
    val recordingButtonResource = MutableStateFlow(R.string.record)

    fun onBluetoothEnabledOrDeviceBonded(bluetoothAdapter: BluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter
        if (bluetoothAdapter.bondedDevices.none { it.name.contains(NAME_OF_THE_DEVICE) }) {
            outputMessage.tryEmit(ResourceWithFormatting(R.string.no_device_paired, READABLE_NAME_OF_THE_DEVICE))
        } else {
            outputMessage.tryEmit(ResourceWithFormatting(R.string.ready_to_connect, READABLE_NAME_OF_THE_DEVICE))
        }
    }

    // Suppress warning since Android Studio doesn't know ioDispatcher is always ioDispatcher when running the app
    @Suppress("BlockingMethodInNonBlockingContext")
    fun connectDevice(ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
        viewModelScope.launch(ioDispatcher) {
            if (bluetoothAdapter == null) {
                toastMessage.emit(ResourceWithFormatting(R.string.bluetooth_was_not_enabled, null))
                return@launch
            }
            bluetoothAdapter!!.cancelDiscovery()
            val device =
                bluetoothAdapter!!.bondedDevices.filter { it.name.contains(NAME_OF_THE_DEVICE) }
                    .getOrNull(0)
            if (device == null) {
                toastMessage.emit(ResourceWithFormatting(R.string.no_device_found))
                return@launch
            }
            prepareAudioTrack()
            bluetoothSocket =
                device.createRfcommSocketToServiceRecord(DEFAULT_UUID_FOR_CUSTOM_DEVICES)
            try {
                outputMessage.emit(ResourceWithFormatting(R.string.connecting_device))
                bluetoothSocket!!.connect()
                outputMessage.emit(ResourceWithFormatting(R.string.device_connected))
                val inputStream = bluetoothSocket!!.inputStream
                var overallBytes = 0L
                val bytes = ByteArray(128)
                var seconds = System.currentTimeMillis() / 1000
                while (true) {
                    try {
                        inputStream.read(bytes)
                    } catch (t: Throwable) {
                        toastMessage.emit(ResourceWithFormatting(R.string.stream_was_interrupted, null))
                        outputMessage.emit(ResourceWithFormatting(R.string.device_not_connected))
                        break
                    }
                    if (shouldSendBytesForRecord) {
                        audioChannel.send(bytes.copyOf())
                    }
                    overallBytes++
                    val newSeconds = System.currentTimeMillis() / 1000
                    if (newSeconds > seconds) {
                        Log.i("BytesPerSecond", overallBytes.toString())
                        overallBytes = 0
                        seconds = newSeconds
                    }
                    audioTrack!!.write(bytes, 0, bytes.size)
                }
            } catch (t: Throwable) {
                outputMessage.emit(ResourceWithFormatting(R.string.device_not_connected))
                toastMessage.emit(ResourceWithFormatting(R.string.error_while_connecting, null))
            }
        }
    }

    private fun prepareAudioTrack() {
        audioTrack = audioTrackProvider.getAudioTrack()
        audioTrack!!.play()
    }

    fun startStopRecording(dir: File) {
        if (recording) {
            recording = false
            recordingButtonResource.tryEmit(R.string.record)
        } else {
            recordingButtonResource.tryEmit(R.string.stop_recording)
            startRecording(dir)
        }
    }

    private fun startRecording(dir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            outputMessage.emit(ResourceWithFormatting(R.string.recording_started, null))
            recording = true
            shouldSendBytesForRecord = true
            var amountOfBytes = 0
            var dataByteArray = ByteArray(0)
            while (recording || (!recording && !audioChannel.isEmpty)) {
                val newByteArray = audioChannel.receive()
                dataByteArray += newByteArray.shiftValuesByZeroOffset()
                amountOfBytes += newByteArray.size
                if (!recording) {
                    shouldSendBytesForRecord = false
                }
            }
            val wavByteArray = HeaderForWavFile.getHeaderForWavFile(amountOfBytes) + dataByteArray
            try {
                val file = File(dir, "BluetoothMusic${System.currentTimeMillis() / 1000}.wav")
                val fileOutputStream = FileOutputStream(file)
                fileOutputStream.write(wavByteArray)
                fileOutputStream.close()
                outputMessage.emit(ResourceWithFormatting(R.string.recording_completed, null))
            } catch (t: Throwable) {
                toastMessage.emit(ResourceWithFormatting(R.string.error_while_saving, null))
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