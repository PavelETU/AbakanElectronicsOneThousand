package com.abakan.electronics.one.thousand.utils.fourier_transform

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test

class CooleyTurkeyFFTShould {
    private val fourierTransform: FourierTransform = CooleyTurkeyFFT()
    @Test
    fun `transform no input in time domain into no output in frequency domain`() {
        val byteArray = ByteArray(4) { 0 }
        val results = fourierTransform.transformToFrequencyDomain(byteArray)
        MatcherAssert.assertThat(results, CoreMatchers.`is`(listOf(0.0, 0.0, 0.0, 0.0)))
    }

    @Test
    fun `transform time domain into frequency domain zeroing first element`() {
        val byteArray = ByteArray(4) { (it + 1).toByte() }
        val results = fourierTransform.transformToFrequencyDomain(byteArray)
        MatcherAssert.assertThat(results[0], CoreMatchers.`is`(10.0))
        MatcherAssert.assertThat(results[1], CoreMatchers.`is`(2.8284271247461903))
        MatcherAssert.assertThat(results[2], CoreMatchers.`is`(2.0))
        MatcherAssert.assertThat(results[3], CoreMatchers.`is`(2.82842712474619))
    }
}