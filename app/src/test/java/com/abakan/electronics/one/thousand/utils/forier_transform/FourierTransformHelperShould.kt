package com.abakan.electronics.one.thousand.utils.forier_transform

import com.abakan.electronics.one.thousand.utils.fourier_transform.FourierTransform
import com.abakan.electronics.one.thousand.utils.fourier_transform.FourierTransformHelperImpl
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FourierTransformHelperShould {
    @MockK
    private lateinit var fourierTransform: FourierTransform
    @InjectMockKs
    private lateinit var fourierTransformHelper: FourierTransformHelperImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `return 0 as peak frequency index given max value of FT is first`() {
        every { fourierTransform.transformToFrequencyDomain(any()) } returns listOf(10.0, 6.53423, 0.0, 8.3)
        val indexOfHighestFrequency = fourierTransformHelper.getPeakFrequencyIndex(ByteArray(4))
        assertEquals(0, indexOfHighestFrequency)
    }

    @Test
    fun `return last index as peak frequency index given max value of FT is last`() {
        every { fourierTransform.transformToFrequencyDomain(any()) } returns listOf(10.0, 6.53423, 0.0, 20.0)
        val indexOfHighestFrequency = fourierTransformHelper.getPeakFrequencyIndex(ByteArray(4))
        assertEquals(3, indexOfHighestFrequency)
    }

    @Test
    fun `return 0Hz given peak index is zero`() {
        every { fourierTransform.transformToFrequencyDomain(any()) } returns listOf(10.0, 6.53423, 0.0, 8.3)
        val peakFrequency = fourierTransformHelper.getPeakFrequency(ByteArray(4))
        assertEquals(0.0, peakFrequency, 0.0)
    }

    @Test
    fun `return 3000Hz given peak index is last`() {
        every { fourierTransform.transformToFrequencyDomain(any()) } returns listOf(10.0, 6.53423, 0.0, 20.0)
        val peakFrequency = fourierTransformHelper.getPeakFrequency(ByteArray(4))
        assertEquals(3000.0, peakFrequency, 0.0)
    }

    @Test
    fun `return 3999Hz given peak index is last in 4000 array`() {
        every { fourierTransform.transformToFrequencyDomain(any()) } returns List(4000) { it.toDouble() }
        val peakFrequency = fourierTransformHelper.getPeakFrequency(ByteArray(4000))
        assertEquals(3999.0, peakFrequency, 0.0)
    }

    @Test
    fun `return 3999 point 99Hz given peak index is last in 400000 array`() {
        every { fourierTransform.transformToFrequencyDomain(any()) } returns List(400000) { it.toDouble() }
        val peakFrequency = fourierTransformHelper.getPeakFrequency(ByteArray(400000))
        assertEquals(3999.99, peakFrequency, 0.0001)
    }

    @Test
    fun `provide spectrogram`() {
        val valueInFrequencyDomain = List(4000) { it.toDouble() }
        every { fourierTransform.transformToFrequencyDomain(any()) } returns valueInFrequencyDomain
        assertEquals(valueInFrequencyDomain, fourierTransformHelper.getSpectrogram(ByteArray(4000)))
    }
}