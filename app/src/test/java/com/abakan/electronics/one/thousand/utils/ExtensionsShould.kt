package com.abakan.electronics.one.thousand.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionsShould {
    @Test
    fun `shift all values by 74 in ByteArray`() {
        val shiftedValues = ByteArray(200) { 74 }.shiftValuesByZeroOffset()
        assertTrue(shiftedValues.allElementsHaveAValueOf(0))
    }

    @Test
    fun `convert byte to complex number`() {
        val byte: Byte = 74
        assertEquals(ComplexNumber(74.0, 0.0), byte.toComplex())
    }

    private fun ByteArray.allElementsHaveAValueOf(value: Int) =
        all { it.toInt() == value }
}