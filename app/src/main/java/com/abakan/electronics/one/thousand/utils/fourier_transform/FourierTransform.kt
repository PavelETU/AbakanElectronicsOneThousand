package com.abakan.electronics.one.thousand.utils.fourier_transform

interface FourierTransform {
    fun transformToFrequencyDomain(bytes: ByteArray): List<Double>
}
