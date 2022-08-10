package com.abakan.electronics.one.thousand.utils.fourier_transform

import com.abakan.electronics.one.thousand.utils.toComplex
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class CooleyTurkeyFFT @Inject constructor() : FourierTransform {
    override fun transformToFrequencyDomain(bytes: ByteArray) =
        calculateFFTInComplexNumbers(bytes.map { it.toComplex() }).mapIndexed { index, complexNumber ->
            if (index == 0) 0.0 else complexNumber.toDouble()
        }

    private fun calculateFFTInComplexNumbers(input: List<ComplexNumber>): List<ComplexNumber> {
        val n = input.size
        if (n == 1) {
            return input
        }
        val wn = ComplexNumber(cos(2 * PI / n), sin(2 * PI / n))
        var w = ComplexNumber(1.0, 0.0)
        val a0 = List(n / 2) { input[2 * it] }
        val a1 = List(n / 2) { input[2 * it + 1] }
        val y0 = calculateFFTInComplexNumbers(a0)
        val y1 = calculateFFTInComplexNumbers(a1)
        val results = MutableList(n) { ComplexNumber(0.0, 0.0) }
        for (k in 0 until n / 2) {
            results[k] = y0[k] + w * y1[k]
            results[k + n / 2] = y0[k] - w * y1[k]
            w *= wn
        }
        return results
    }
}