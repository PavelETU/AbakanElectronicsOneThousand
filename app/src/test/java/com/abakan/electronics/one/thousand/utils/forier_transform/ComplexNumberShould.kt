package com.abakan.electronics.one.thousand.utils.forier_transform

import com.abakan.electronics.one.thousand.utils.fourier_transform.ComplexNumber
import junit.framework.Assert.assertEquals
import org.junit.Test

class ComplexNumberShould {

    @Test
    fun `return absolute value (or magnitude) when converting to real numbers`() {
        assertEquals(6.324555320336759, ComplexNumber(2.0, 6.0).toDouble())
        assertEquals(6, ComplexNumber(2.0, 6.0).toByte())
        assertEquals(6.toChar(), ComplexNumber(2.0, 6.0).toChar())
        assertEquals(6.3245554F, ComplexNumber(2.0, 6.0).toFloat())
        assertEquals(6, ComplexNumber(2.0, 6.0).toInt())
        assertEquals(6, ComplexNumber(2.0, 6.0).toLong())
        assertEquals(6, ComplexNumber(2.0, 6.0).toShort())
    }

    @Test
    fun `do addition`() {
        assertEquals(ComplexNumber(14.0, 30.0), ComplexNumber(2.0, 8.0) + ComplexNumber(12.0, 22.0))
    }

    @Test
    fun `do subtraction`() {
        assertEquals(ComplexNumber(14.0, 30.0), ComplexNumber(20.0, 40.0) - ComplexNumber(6.0, 10.0))
    }

    @Test
    fun `do multiplication`() {
        assertEquals(ComplexNumber(30.0, 58.0), ComplexNumber(4.0, 5.0) * ComplexNumber(10.0, 2.0))
    }
}