package com.abakan.electronics.one.thousand.utils

import com.abakan.electronics.one.thousand.SAMPLE_RATE
import javax.inject.Inject

class FourierTransformHelperImpl @Inject constructor(private val fourierTransform: FourierTransform): FourierTransformHelper {
    override fun getPeakFrequency(dataInTimeDomain: ByteArray): Double {
        val peakFrequencyIndex = getPeakFrequencyIndex(dataInTimeDomain)
        val frequencyResolutionInHz = SAMPLE_RATE.toDouble() / dataInTimeDomain.size
        return peakFrequencyIndex * frequencyResolutionInHz
    }

    override fun getPeakFrequencyIndex(bytesToStream: ByteArray): Int {
        val dataInFrequencyDomain = fourierTransform.transformToFrequencyDomain(bytesToStream)
        return dataInFrequencyDomain.indexOfMax()
    }
}