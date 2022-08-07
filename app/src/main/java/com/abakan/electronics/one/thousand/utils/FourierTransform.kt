package com.abakan.electronics.one.thousand.utils

interface FourierTransform {
    fun transformToFrequencyDomain(bytes: ByteArray): List<Double>
}
