package com.example.bluetoothtest

import android.media.AudioTrack

interface AudioTrackProvider {
    fun getAudioTrack(): AudioTrack
}