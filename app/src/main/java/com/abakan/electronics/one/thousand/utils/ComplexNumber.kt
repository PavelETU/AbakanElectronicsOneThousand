package com.abakan.electronics.one.thousand.utils

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class ComplexNumber(private val r: Double, private val i: Double): Number() {

    private fun calculateMagnitude(): Double {
        return sqrt(r.pow(2) + i.pow(2))
    }

    // Number Overrides
    override fun toByte() = calculateMagnitude().roundToInt().toByte()
    override fun toChar() = calculateMagnitude().roundToInt().toChar()
    override fun toDouble() = calculateMagnitude()
    override fun toFloat() = calculateMagnitude().toFloat()
    override fun toInt() = calculateMagnitude().roundToInt()
    override fun toLong() = calculateMagnitude().toLong()
    override fun toShort() = calculateMagnitude().roundToInt().toShort()

    // Operators
    operator fun plus(other: ComplexNumber): ComplexNumber {
        return ComplexNumber(r + other.r, i + other.i)
    }

    operator fun minus(other: ComplexNumber): ComplexNumber {
        return ComplexNumber(r - other.r, i - other.i)
    }

    operator fun times(other: ComplexNumber): ComplexNumber {
        return ComplexNumber(r * other.r - i * other.i, r * other.i + i * other.r)
    }

    // Object's functions
    override fun equals(other: Any?): Boolean {
        return (other is ComplexNumber) && other.r == r && other.i == i
    }

    override fun toString(): String {
        return "$r + ${i}i"
    }

    override fun hashCode(): Int {
        var result = r.hashCode()
        result = 31 * result + i.hashCode()
        return result
    }
}
