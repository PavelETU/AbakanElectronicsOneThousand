package com.abakan.electronics.one.thousand.utils

import androidx.annotation.VisibleForTesting

interface FourierTransform {
    fun getPeakFrequencyIndex(bytesToStream: ByteArray): Int

    @VisibleForTesting
    fun transformToFrequencyDomain(bytes: ByteArray): List<Double>
}
