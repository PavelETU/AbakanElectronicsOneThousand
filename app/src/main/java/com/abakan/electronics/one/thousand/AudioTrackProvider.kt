package com.abakan.electronics.one.thousand

import android.media.AudioTrack

interface AudioTrackProvider {
    fun getAudioTrack(): AudioTrack
}