package com.abakan.electronics.one.thousand.utils

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class FFTShould {
    private val fftHelper: FFTHelper = SimpleFFT()
    @Test
    fun `transform no input in time domain into no output in frequency domain`() {
        val byteArray = ByteArray(4) { 0 }
        val results = fftHelper.transformToFrequencyDomain(byteArray)
        assertThat(results, `is`(listOf(0.0, 0.0, 0.0, 0.0)))
    }

    @Test
    fun `transform simple time domain input into frequency domain output`() {
        val byteArray = ByteArray(4) { (it + 1).toByte() }
        val results = fftHelper.transformToFrequencyDomain(byteArray)
        assertThat(results[0], `is`(10.0))
        assertThat(results[1], `is`(2.8284271247461903))
        assertThat(results[2], `is`(2.0))
        assertThat(results[3], `is`(2.8284271247461894))
    }
}