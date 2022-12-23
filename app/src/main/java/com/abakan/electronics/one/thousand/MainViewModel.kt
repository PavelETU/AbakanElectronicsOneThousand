package com.abakan.electronics.one.thousand

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abakan.electronics.one.thousand.utils.HeaderForWavFile
import com.abakan.electronics.one.thousand.utils.ResourceWithFormatting
import com.abakan.electronics.one.thousand.utils.fourier_transform.FourierTransformHelper
import com.abakan.electronics.one.thousand.utils.shiftValuesByZeroOffset
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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


// For Samsung SM-A125F speaker Sample rate: 48,000
// FramesPerBuffer: 256
// For Pixel 6a speaker Sample rate: 48,000
// FramesPerBuffer: 128

// AE Receiver

private const val USB_DEVICE_NAME = "USB-Audio - AE Receiver"


@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("MissingPermission")
@HiltViewModel
class MainViewModel @Inject constructor(application: Application,
    private val audioTrackProvider: AudioTrackProvider,
    private val fourierTransformHelper: FourierTransformHelper
) : AndroidViewModel(application) {
    private val audioChannel = Channel<ByteArray>(4000)
    private var recording = false
    val disappearingMessage = MutableSharedFlow<ResourceWithFormatting>()
    val outputMessage =
        MutableStateFlow(ResourceWithFormatting(R.string.ready_to_connect, "$NAME_OF_THE_DEVICE"))
    val recordingButtonResource = MutableStateFlow(R.string.record)
    val showTuner = MutableStateFlow(false)
    val leadingFrequency = MutableStateFlow(0.0)
    val spectrogram = MutableStateFlow(listOf<Double>())
    val spectrumStart = MutableStateFlow("0.0")
    val spectrumEnd =
        MutableStateFlow("${SAMPLE_RATE.toDouble() - (SAMPLE_RATE.toDouble() / FFT_SAMPLE_SIZE.toDouble())}")
    val maxFrequency = MutableStateFlow(0.0)
    private var minSpectrumIndex = 0
    private var maxSpectrumIndex = FFT_SAMPLE_SIZE - 1
    private var audioRecord: AudioRecord? = null

    @VisibleForTesting
    val fftChannel = Channel<ByteArray>(4000)
    private var tuning = false
    private var connectionPending = false
    private var streaming = false
    private val ampLibrary = AmpLibrary()
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val mainButtonTitle = MutableStateFlow("Connect")


    init {
        val sampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val framesPerBuffer = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        ampLibrary.setDefaultParams(sampleRate.toInt(), framesPerBuffer.toInt())
    }

    fun connectDisconnectDevice() {
        if (!streaming) {
            val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val aeReceiver = inputDevices.find { it.productName == USB_DEVICE_NAME }
            if (aeReceiver == null) {
                outputMessage.tryEmit(ResourceWithFormatting(R.string.not_attached, null))
                return
            }
            val streamStarted = ampLibrary.startStreamingFrom(aeReceiver.id)
            if (!streamStarted) {
                outputMessage.tryEmit(ResourceWithFormatting(R.string.error_creating_stream, null))
                return
            }
            mainButtonTitle.tryEmit("Disconnect")
        } else {
            stopStream()
            mainButtonTitle.tryEmit("Connect")
        }
        streaming = !streaming
    }

    private fun stopStream() {
        ampLibrary.stopStreaming()
    }

    override fun onCleared() {
        stopStream()
    }

    fun startStopRecording(dir: File) {
        if (!streaming) {
            connectDisconnectDevice()
        }
        if (recording) {
            recording = false
            recordingButtonResource.tryEmit(R.string.record)
        } else {
            recordingButtonResource.tryEmit(R.string.stop_recording)
            startRecording(dir)
        }
    }

    private fun startRecording(dir: File) {
        audioRecord = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder().setSampleRate(
                    SAMPLE_RATE
                ).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO).build()
            ).setBufferSizeInBytes(
                BUFFER_SIZE
            ).build()
        val buffer = ByteArray(BUFFER_SIZE)
        audioRecord!!.startRecording()
        viewModelScope.launch(Dispatchers.IO) {
            outputMessage.emit(ResourceWithFormatting(R.string.recording_started, null))
            recording = true
            var amountOfBytes = 0
            var dataByteArray = ByteArray(0)
            while (recording) {
                audioRecord!!.read(buffer, 0, BUFFER_SIZE)
                dataByteArray += buffer
                amountOfBytes += buffer.size
            }
            audioRecord!!.stop()
            val wavByteArray = HeaderForWavFile.getHeaderForWavFile(amountOfBytes) + dataByteArray
            try {
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val uri = Uri.fromFile(dir).buildUpon()
                    .appendPath(
                        "Record from " + SimpleDateFormat(
                            "h.mm a, dd.MM.yy",
                            Locale.getDefault()
                        ).format(Calendar.getInstance().time) + ".wav"
                    )
                    .build()
                val file = File(uri.path!!)
                val fileOutputStream = FileOutputStream(file)
                fileOutputStream.write(wavByteArray)
                fileOutputStream.close()
                outputMessage.emit(ResourceWithFormatting(R.string.recording_completed, null))
            } catch (t: Throwable) {
                disappearingMessage.emit(ResourceWithFormatting(R.string.error_while_saving, null))
            }
        }
    }

    fun startTuning(ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
        viewModelScope.launch(ioDispatcher) {
            showTuner.emit(true)
            tuning = true
            var dataInTimeDomain = ByteArray(0)
            while (tuning) {
                dataInTimeDomain += fftChannel.receive()
                if (dataInTimeDomain.size == FFT_SAMPLE_SIZE) {
                    if (SPECTROGRAM_OVER_TUNER) {
                        val spector = fourierTransformHelper.getSpectrogram(dataInTimeDomain)
                        spectrogram.emit(spector.subList(minSpectrumIndex, maxSpectrumIndex + 1))
                        maxFrequency.emit((spectrogram.value.indexOf(spectrogram.value.max()) + minSpectrumIndex) * (SAMPLE_RATE.toDouble() / FFT_SAMPLE_SIZE.toDouble()))
                    } else {
                        val peakFrequency =
                            fourierTransformHelper.getPeakFrequency(dataInTimeDomain)
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

    fun setMinSpectrumFrequency(minFrequency: String) {
        val newMinFrequency = minFrequency.toDouble()
        val resolution = SAMPLE_RATE.toDouble() / FFT_SAMPLE_SIZE.toDouble()
        val closestIndex = (newMinFrequency / resolution).toInt()
        minSpectrumIndex =
            if (closestIndex * resolution <= newMinFrequency && (closestIndex + 1) * resolution > newMinFrequency) {
                closestIndex
            } else if ((closestIndex + 1) * resolution <= newMinFrequency) {
                closestIndex + 1
            } else {
                (closestIndex - 1).takeIf { it >= 0 } ?: 0
            }
        spectrumStart.tryEmit((minSpectrumIndex * resolution).toString())
    }

    fun setMaxSpectrumFrequency(maxFrequency: String) {
        val newMaxFrequency = maxFrequency.toDouble()
        val resolution = SAMPLE_RATE.toDouble() / FFT_SAMPLE_SIZE.toDouble()
        val closestIndex = (newMaxFrequency / resolution + 1).toInt()
        maxSpectrumIndex =
            if (closestIndex * resolution >= newMaxFrequency && (closestIndex - 1) * resolution < newMaxFrequency) {
                closestIndex
            } else if ((closestIndex - 1) * resolution >= newMaxFrequency) {
                closestIndex - 1
            } else {
                (closestIndex + 1).takeIf { it <= FFT_SAMPLE_SIZE } ?: FFT_SAMPLE_SIZE
            }
        spectrumEnd.tryEmit(((maxSpectrumIndex) * resolution).toString())
    }

    fun onConnectionActionReceived() {
        connectionPending = true
    }
}