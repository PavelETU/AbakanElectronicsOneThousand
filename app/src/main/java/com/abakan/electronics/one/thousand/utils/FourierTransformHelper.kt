package com.abakan.electronics.one.thousand.utils

import androidx.annotation.VisibleForTesting

interface FourierTransformHelper {
    fun getPeakFrequency(dataInTimeDomain: ByteArray): Double
    @VisibleForTesting
    fun getPeakFrequencyIndex(bytesToStream: ByteArray): Int
    fun getSpectrogram(dataInTimeDomain: ByteArray): List<Double>
}
