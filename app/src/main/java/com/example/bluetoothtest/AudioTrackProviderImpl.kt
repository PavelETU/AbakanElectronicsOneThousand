package com.example.bluetoothtest

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import javax.inject.Inject

class AudioTrackProviderImpl @Inject constructor(): AudioTrackProvider {
    override fun getAudioTrack(): AudioTrack {
        return AudioTrack.Builder().setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(
                AudioAttributes.CONTENT_TYPE_MUSIC
            )
                .build()
        ).setTransferMode(AudioTrack.MODE_STREAM).setAudioFormat(
            AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_8BIT).setSampleRate(4000)
                .build()
        )
            .setBufferSizeInBytes(160)
            .build()
    }
}