package com.abakan.electronics.one.thousand

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.media.AudioTrack
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abakan.electronics.one.thousand.utils.*
import com.abakan.electronics.one.thousand.utils.fourier_transform.FourierTransformHelper
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
    private val audioTrackProvider: AudioTrackProvider,
    private val fourierTransformHelper: FourierTransformHelper
) : ViewModel() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var audioTrack: AudioTrack? = null
    private val audioChannel = Channel<ByteArray>(4000)
    private var recording = false
    private var shouldSendBytesForRecord = false
    val disappearingMessage = MutableSharedFlow<ResourceWithFormatting>()
    val outputMessage = MutableStateFlow(ResourceWithFormatting(R.string.app_name, " for $READABLE_NAME_OF_THE_DEVICE"))
    val recordingButtonResource = MutableStateFlow(R.string.record)
    val showTuner = MutableStateFlow(false)
    val leadingFrequency = MutableStateFlow(0.0)
    val spectrogram = MutableStateFlow(listOf<Double>())
    @VisibleForTesting
    val fftChannel = Channel<ByteArray>(4000)
    private var tuning = false

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
                disappearingMessage.emit(ResourceWithFormatting(R.string.bluetooth_was_not_enabled, null))
                return@launch
            }
            bluetoothAdapter!!.cancelDiscovery()
            val device =
                bluetoothAdapter!!.bondedDevices.filter { it.name.contains(NAME_OF_THE_DEVICE) }
                    .getOrNull(0)
            if (device == null) {
                disappearingMessage.emit(ResourceWithFormatting(R.string.no_device_found))
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
                        disappearingMessage.emit(ResourceWithFormatting(R.string.stream_was_interrupted, null))
                        outputMessage.emit(ResourceWithFormatting(R.string.device_not_connected))
                        break
                    }
                    if (shouldSendBytesForRecord) {
                        audioChannel.send(bytes.copyOf())
                    }
                    if (tuning) {
                        fftChannel.send(bytes.copyOf())
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
                disappearingMessage.emit(ResourceWithFormatting(R.string.error_while_connecting, null))
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
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = File(dir, "BluetoothMusic${System.currentTimeMillis() / 1000}.wav")
                val fileOutputStream = FileOutputStream(file)
                fileOutputStream.write(wavByteArray)
                fileOutputStream.close()
                outputMessage.emit(ResourceWithFormatting(R.string.recording_completed, null))
            } catch (t: Throwable) {
                disappearingMessage.emit(ResourceWithFormatting(R.string.error_while_saving, null))
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    public override fun onCleared() {
        try {
            audioTrack?.stop()
            bluetoothSocket?.close()
        } catch (t: Throwable) {
            Log.e(javaClass.name, "Can't close the socket cause ${t.message}")
        }
    }

    fun startTuning(ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
        viewModelScope.launch(ioDispatcher) {
            showTuner.emit(true)
            tuning = true
            var dataInTimeDomain = ByteArray(0)
            while (tuning) {
                dataInTimeDomain += fftChannel.receive()
                if (dataInTimeDomain.size == 256) {
                    if (SPECTROGRAM_OVER_TUNER) {
                        val spector = fourierTransformHelper.getSpectrogram(dataInTimeDomain)
                        spectrogram.emit(spector)
                    } else {
                        val peakFrequency = fourierTransformHelper.getPeakFrequency(dataInTimeDomain)
                        leadingFrequency.emit(peakFrequency)
                    }
                    dataInTimeDomain = ByteArray(0)
                }
            }
        }
    }

    fun tuningDismissed() {
        showTuner.tryEmit(false)
        tuning = false
    }
}