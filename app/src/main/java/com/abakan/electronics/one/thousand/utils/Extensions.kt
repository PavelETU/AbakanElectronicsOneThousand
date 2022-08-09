package com.abakan.electronics.one.thousand.utils

import com.abakan.electronics.one.thousand.ZERO_OFFSET
import com.abakan.electronics.one.thousand.utils.fourier_transform.ComplexNumber

fun ByteArray.shiftValuesByZeroOffset(): ByteArray {
    return map { (it.toInt() - ZERO_OFFSET).toByte() }.toByteArray()
}

fun Byte.toComplex(): ComplexNumber = ComplexNumber(toDouble(), 0.0)

fun <T: Comparable<T>> List<T>.indexOfMax(): Int {
    return indices.maxByOrNull { this[it] } ?: 0
}
