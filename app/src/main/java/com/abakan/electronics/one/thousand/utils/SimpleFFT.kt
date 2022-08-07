package com.abakan.electronics.one.thousand.utils

import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class SimpleFFT @Inject constructor(): FFTHelper {
    override fun getPeakFrequency(bytesToStream: ByteArray): Double {
        TODO("Not yet implemented")
    }

    override fun transformToFrequencyDomain(bytes: ByteArray): List<Double> {
        val result = mutableListOf<Double>()
        val n = bytes.size
        for (i in bytes.indices) {
            var firstSum = ComplexNumber(0.0, 0.0)
            bytes.forEachIndexed { index, byte ->
                firstSum += byte.toComplex() * (ComplexNumber(
                    cos((2 * PI * i * index) / n),
                    0.0
                ) - ComplexNumber(0.0, sin((2 * PI * i * index) / n)))
            }
            result.add(firstSum.toDouble())
        }
        return result
    }
}