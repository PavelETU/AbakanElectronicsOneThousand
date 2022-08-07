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

    @Test
    fun `return index of max element in iterable`() {
        val listOfDoubles = listOf(0.0, 15.9, 85.0, 3.33333, 96.612)
        assertEquals(4, listOfDoubles.indexOfMax())
    }

    @Test
    fun `return 0 as index of max element in iterable if there is no max element`() {
        val listOfDoubles = listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        assertEquals(0, listOfDoubles.indexOfMax())
    }

    private fun ByteArray.allElementsHaveAValueOf(value: Int) =
        all { it.toInt() == value }
}