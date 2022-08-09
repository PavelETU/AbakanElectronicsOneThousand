package com.abakan.electronics.one.thousand.utils.forier_transform

import com.abakan.electronics.one.thousand.utils.fourier_transform.FourierTransform
import com.abakan.electronics.one.thousand.utils.fourier_transform.SimpleDFT
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class FFTShould {
    private val fourierTransform: FourierTransform = SimpleDFT()
    @Test
    fun `transform no input in time domain into no output in frequency domain`() {
        val byteArray = ByteArray(4) { 0 }
        val results = fourierTransform.transformToFrequencyDomain(byteArray)
        assertThat(results, `is`(listOf(0.0, 0.0, 0.0, 0.0)))
    }

    @Test
    fun `transform time domain into frequency domain zeroing first element`() {
        val byteArray = ByteArray(4) { (it + 1).toByte() }
        val results = fourierTransform.transformToFrequencyDomain(byteArray)
        assertThat(results[0], `is`(0.0))
        assertThat(results[1], `is`(2.8284271247461903))
        assertThat(results[2], `is`(2.0))
        assertThat(results[3], `is`(2.8284271247461894))
    }
}