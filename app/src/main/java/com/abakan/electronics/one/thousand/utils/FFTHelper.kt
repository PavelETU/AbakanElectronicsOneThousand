package com.abakan.electronics.one.thousand.utils

import androidx.annotation.VisibleForTesting

interface FFTHelper {
    fun getPeakFrequency(bytesToStream: ByteArray): Double
    @VisibleForTesting
    fun transformToFrequencyDomain(bytes: ByteArray): List<Double>
}
