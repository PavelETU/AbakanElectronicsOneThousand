package com.abakan.electronics.one.thousand.utils.fourier_transform

import com.abakan.electronics.one.thousand.SAMPLE_RATE
import com.abakan.electronics.one.thousand.utils.indexOfMax
import javax.inject.Inject

class FourierTransformHelperImpl @Inject constructor(private val fourierTransform: FourierTransform):
    FourierTransformHelper {
    override fun getPeakFrequency(dataInTimeDomain: ByteArray): Double {
        val peakFrequencyIndex = getPeakFrequencyIndex(dataInTimeDomain)
        val frequencyResolutionInHz = SAMPLE_RATE.toDouble() / dataInTimeDomain.size
        return peakFrequencyIndex * frequencyResolutionInHz
    }

    override fun getPeakFrequencyIndex(bytesToStream: ByteArray): Int {
        val dataInFrequencyDomain = fourierTransform.transformToFrequencyDomain(bytesToStream)
        return dataInFrequencyDomain.indexOfMax()
    }

    override fun getSpectrogram(dataInTimeDomain: ByteArray): List<Double> {
        return fourierTransform.transformToFrequencyDomain(dataInTimeDomain)
    }
}