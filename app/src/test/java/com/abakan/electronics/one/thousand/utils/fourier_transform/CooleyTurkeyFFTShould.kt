package com.abakan.electronics.one.thousand.utils.fourier_transform

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class CooleyTurkeyFFTShould {
    private val fourierTransform: FourierTransform = CooleyTurkeyFFT()
    @Test
    fun `transform no input in time domain into no output in frequency domain`() {
        val byteArray = ByteArray(4) { 0 }
        val results = fourierTransform.transformToFrequencyDomain(byteArray)
        assertThat(results, CoreMatchers.`is`(listOf(0.0, 0.0, 0.0, 0.0)))
    }

    @Test
    fun `transform time domain into frequency domain zeroing first element`() {
        val byteArray = ByteArray(4) { (it + 1).toByte() }
        val results = fourierTransform.transformToFrequencyDomain(byteArray)
        assertThat(results[0], CoreMatchers.`is`(0.0))
        assertThat(results[1], CoreMatchers.`is`(2.8284271247461903))
        assertThat(results[2], CoreMatchers.`is`(2.0))
        assertThat(results[3], CoreMatchers.`is`(2.82842712474619))
    }

    @Test
    @Ignore("Ignoring due to the long execution time")
    fun `should be at least 10 times faster than n^2 implementation for 8000 samples`() {
        val simpleDFT = SimpleDFT()
        val byteArray = ByteArray(8000) { it.toByte() }
        var milliseconds = System.currentTimeMillis()
        fourierTransform.transformToFrequencyDomain(byteArray)
        val timeToTransformUsingCooleyTurkey = System.currentTimeMillis() - milliseconds
        milliseconds = System.currentTimeMillis()
        simpleDFT.transformToFrequencyDomain(byteArray)
        val timeToTransformUsingQuadraticAlgorithm = System.currentTimeMillis() - milliseconds
        println("It took simple algorithm $timeToTransformUsingQuadraticAlgorithm milliseconds to process ${byteArray.size} samples")
        println("It took FFT algorithm $timeToTransformUsingCooleyTurkey milliseconds to process ${byteArray.size} samples")
        assertTrue(timeToTransformUsingQuadraticAlgorithm > timeToTransformUsingCooleyTurkey * 10)
    }
}