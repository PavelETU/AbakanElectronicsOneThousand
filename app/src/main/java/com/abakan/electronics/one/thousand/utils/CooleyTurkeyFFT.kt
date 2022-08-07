package com.abakan.electronics.one.thousand.utils

import javax.inject.Inject

class CooleyTurkeyFFT @Inject constructor(): FourierTransform {
    override fun transformToFrequencyDomain(bytes: ByteArray): List<Double> {
        TODO("Not yet implemented")
    }
}