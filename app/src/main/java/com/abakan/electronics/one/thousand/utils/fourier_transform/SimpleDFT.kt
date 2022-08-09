package com.abakan.electronics.one.thousand.utils.fourier_transform

import com.abakan.electronics.one.thousand.utils.toComplex
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class SimpleDFT @Inject constructor(): FourierTransform {
    override fun transformToFrequencyDomain(bytes: ByteArray): List<Double> {
        val result = mutableListOf<Double>()
        val n = bytes.size
        for (i in bytes.indices) {
            var xOfK = ComplexNumber(0.0, 0.0)
            bytes.forEachIndexed { index, byte ->
                xOfK += byte.toComplex() * (ComplexNumber(
                    cos((2 * PI * i * index) / n),
                    0.0
                ) - ComplexNumber(0.0, sin((2 * PI * i * index) / n)))
            }
            result.add(xOfK.toDouble())
        }
        result[0] = 0.0
        return result
    }
}