package com.abakan.electronics.one.thousand

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.media.AudioTrack
import app.cash.turbine.test
import com.abakan.electronics.one.thousand.utils.ResourceWithFormatting
import com.abakan.electronics.one.thousand.utils.fourier_transform.FourierTransformHelper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelShould {
    private lateinit var viewModel: MainViewModel

    @MockK(relaxed = true)
    private lateinit var bluetoothAdapter: BluetoothAdapter
    @MockK
    private lateinit var audioTrackProvider: AudioTrackProvider
    @MockK
    private lateinit var myDevice: BluetoothDevice
    @MockK(relaxed = true)
    private lateinit var fourierTransformHelper: FourierTransformHelper

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel = MainViewModel(audioTrackProvider, fourierTransformHelper)
    }

    @Test
    fun `display no devices paired given no bondedDevices`() = runTest {
        every { bluetoothAdapter.bondedDevices } returns emptySet()
        viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        assertThat(
            viewModel.outputMessage.first(),
            `is`(ResourceWithFormatting(R.string.no_device_paired))
        )
    }

    @Test
    fun `display no devices paired given no devices contain NAME_OF_THE_DEVICE in the name`() =
        runTest {
            val fakeDevice = mockk<BluetoothDevice>()
            every { fakeDevice.name } returns "NOT_MY_DEVICE"
            every { bluetoothAdapter.bondedDevices } returns setOf(fakeDevice)
            viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
            assertThat(
                viewModel.outputMessage.first(),
                `is`(ResourceWithFormatting(R.string.no_device_paired))
            )
        }

    @Test
    fun `display device ready to connect given device with NAME_OF_THE_DEVICE name in bondedDevices`() =
        runTest {
            addMyDeviceToBondedDevices()
            viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
            assertThat(
                viewModel.outputMessage.first(),
                `is`(ResourceWithFormatting(R.string.ready_to_connect))
            )
        }

    @Test
    fun `display bluetooth not available message given onBluetoothEnabled was not called`() = runTest {
        viewModel.connectDevice()
        assertThat(
            viewModel.disappearingMessage.first(),
            `is`(ResourceWithFormatting(R.string.bluetooth_was_not_enabled, null))
        )
    }

    @Test
    fun `cancel discovery on BluetoothAdapter when connecting device`() = runTest {
        viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        viewModel.connectDevice(StandardTestDispatcher(testScheduler))
        advanceUntilIdle()
        verify(exactly = 1) { bluetoothAdapter.cancelDiscovery() }
    }

    @Test
    fun `show no device found message given no device found`() = runTest {
        viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        viewModel.connectDevice()
        assertThat(viewModel.disappearingMessage.first(), `is`(ResourceWithFormatting(R.string.no_device_found)))
    }

    @Test
    fun `show error connecting device given connect threw exception`() = runTest {
        val socket = mockk<BluetoothSocket>()
        addMyDeviceToBondedDevices()
        every { myDevice.createRfcommSocketToServiceRecord(any()) } returns socket
        every { socket.connect() } throws IOException()
        every { audioTrackProvider.getAudioTrack() } returns mockk(relaxed = true)

        viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        viewModel.connectDevice(StandardTestDispatcher(testScheduler))

        assertThat(viewModel.disappearingMessage.first(), `is`(ResourceWithFormatting(R.string.error_while_connecting, null)))
        assertThat(viewModel.outputMessage.first(), `is`(ResourceWithFormatting(R.string.device_not_connected)))
    }

    @Test
    fun `show proper statuses while connecting device given connection successful but inputStream threw an error`() = runTest {
        val socket = mockk<BluetoothSocket>()
        addMyDeviceToBondedDevices()
        every { myDevice.createRfcommSocketToServiceRecord(any()) } returns socket
        every { audioTrackProvider.getAudioTrack() } returns mockk(relaxed = true)
        every { socket.connect() } returns Unit
        every { socket.inputStream } throws RuntimeException()

        viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        viewModel.connectDevice(StandardTestDispatcher(testScheduler))

        viewModel.outputMessage.test {
            assertThat(awaitItem(), `is`(ResourceWithFormatting(R.string.ready_to_connect)))
            assertThat(awaitItem(), `is`(ResourceWithFormatting(R.string.connecting_device)))
            assertThat(awaitItem(), `is`(ResourceWithFormatting(R.string.device_connected)))
            assertThat(awaitItem(), `is`(ResourceWithFormatting(R.string.device_not_connected)))
        }
    }

    @Test
    fun `write 128 bytes from BluetoothSocket into audioTrack given connection is established`() = runTest {
        val audioTrack: AudioTrack = mockk(relaxed = true)
        val byteToStream: Byte = 8
        stream128Bytes(audioTrack, bytesToWrite = ByteArray(128) { byteToStream })

        advanceUntilIdle()
        val byteArray = ByteArray(128) { byteToStream }
        verify { audioTrack.write(byteArray, 0,128) }
    }

    @Test
    fun `disconnect device and stop playing given onCleared called`() = runTest {
        val socket: BluetoothSocket = mockk(relaxed = true)
        val audioTrack: AudioTrack = mockk(relaxed = true)
        addMyDeviceToBondedDevices()
        every { myDevice.createRfcommSocketToServiceRecord(any()) } returns socket
        every { audioTrackProvider.getAudioTrack() } returns audioTrack

        viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        viewModel.connectDevice(StandardTestDispatcher(testScheduler))
        advanceUntilIdle()
        viewModel.onCleared()

        verify(exactly = 1) { audioTrack.stop() }
        verify(exactly = 1) { socket.close() }
    }

    @Test
    fun `do not display tuner by default`() {
        assertThat(viewModel.showTuner.value, `is`(false))
    }

    @Test
    fun `display tuner given tune click`() = runTest {
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        advanceUntilIdle()
        assertThat(viewModel.showTuner.value, `is`(true))
    }

    @Test
    fun `close tuner given it was open and dismissed`() = runTest {
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        viewModel.tuningDismissed()
        assertThat(viewModel.showTuner.value, `is`(false))
    }

    @Test
    fun `pass data for tuning given tuning has started`() = runTest {
        val inputStream = mockk<InputStream>(relaxed = true)
        val bytesToStream = ByteArray(128) { 22 }
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        stream128Bytes(inputStream = inputStream, bytesToWrite = bytesToStream)
        assertThat(viewModel.fftChannel.receive(), `is`(bytesToStream))
    }

    @Test
    fun `do not pass data for tuning given tuning dismissed`() = runTest {
        val inputStream = mockk<InputStream>(relaxed = true)
        val bytesToStream = ByteArray(128) { 33 }
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        viewModel.startTuning(ioDispatcher)
        advanceUntilIdle()
        viewModel.tuningDismissed()
        stream128Bytes(inputStream = inputStream, bytesToWrite = bytesToStream, testDispatcher = ioDispatcher)
        assertTrue(viewModel.fftChannel.isEmpty)
    }

    @Test
    fun `do not start FT computation while there are less than 6400 bytes during tuning`() = runTest {
        SPECTROGRAM_OVER_TUNER = false
        val inputStream = mockk<InputStream>(relaxed = true)
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        stream128Bytes(inputStream = inputStream, times = 49)
        advanceUntilIdle()
        verify(exactly = 0) { fourierTransformHelper.getPeakFrequency(any()) }
    }

    @Test
    fun `display peak frequency using fft utils given tuning started and 6400 bytes received`() = runTest {
        SPECTROGRAM_OVER_TUNER = false
        val inputStream = mockk<InputStream>(relaxed = true)
        val peakFrequency = 40.0
        every { fourierTransformHelper.getPeakFrequency(any()) } returns peakFrequency
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        stream128Bytes(inputStream = inputStream, times = 50)
        advanceUntilIdle()
        verify(exactly = 1) { fourierTransformHelper.getPeakFrequency(any()) }
        assertThat(viewModel.leadingFrequency.value, `is`(40.0))
    }

    @Test
    fun `do not start FT computation while there are less than 6400 bytes during spectrogram`() = runTest {
        SPECTROGRAM_OVER_TUNER = true
        val inputStream = mockk<InputStream>(relaxed = true)
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        stream128Bytes(inputStream = inputStream, times = 49)
        advanceUntilIdle()
        verify(exactly = 0) { fourierTransformHelper.getSpectrogram(any()) }
    }

    @Test
    fun `display spectrogram using fft utils given spectrogram started and 6400 bytes received`() = runTest {
        SPECTROGRAM_OVER_TUNER = true
        val inputStream = mockk<InputStream>(relaxed = true)
        every { fourierTransformHelper.getSpectrogram(any()) } returns List(6400) { 0.0 }
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        stream128Bytes(inputStream = inputStream, times = 50)
        advanceUntilIdle()
        verify(exactly = 1) { fourierTransformHelper.getSpectrogram(any()) }
        assertThat(viewModel.spectrogram.value.size, `is`(6400))
        assertThat(viewModel.spectrogram.value, `is`(List(6400) { 0.0 } ))
    }

    @Test
    fun `display all frequencies in spectrogram by default`() = runTest {
        SPECTROGRAM_OVER_TUNER = true
        val inputStream = mockk<InputStream>(relaxed = true)
        every { fourierTransformHelper.getSpectrogram(any()) } returns List(FFT_SAMPLE_SIZE) { 0.0 }
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        stream128Bytes(inputStream = inputStream, times = 50)
        advanceUntilIdle()
        assertEquals("0.0", viewModel.spectrumStart.value)
        assertEquals("3999.375", viewModel.spectrumEnd.value)
        assertThat(viewModel.spectrogram.value.size, `is`(FFT_SAMPLE_SIZE))
    }

    @Test
    fun `display half of the spectrum given 2000Hz selected as a min value`() = runTest {
        SPECTROGRAM_OVER_TUNER = true
        FFT_SAMPLE_SIZE = 10
        BUFFER_SIZE = 10
        val viewModel = MainViewModel(audioTrackProvider, fourierTransformHelper)
        this@MainViewModelShould.viewModel = viewModel
        val inputStream = mockk<InputStream>(relaxed = true)
        val bytes = ByteArray(10) { 0 }
        every { fourierTransformHelper.getSpectrogram(any()) } returns List(10) { it.toDouble() }
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        viewModel.setMinSpectrumFrequency("2000.0")
        stream128Bytes(inputStream = inputStream, bytesToWrite = bytes)
        advanceUntilIdle()
        FFT_SAMPLE_SIZE = 6400
        BUFFER_SIZE = 128
        assertEquals("2000.0", viewModel.spectrumStart.value)
        assertEquals("3600.0", viewModel.spectrumEnd.value)
        assertThat(viewModel.spectrogram.value.size, `is`(5))
        assertEquals(5.0, viewModel.spectrogram.value[0], 0.0)
        assertEquals(6.0, viewModel.spectrogram.value[1], 0.0)
        assertEquals(7.0, viewModel.spectrogram.value[2], 0.0)
        assertEquals(8.0, viewModel.spectrogram.value[3], 0.0)
        assertEquals(9.0, viewModel.spectrogram.value[4], 0.0)
    }

    @Test
    fun `display half of the spectrum + 1 sample given 2000Hz selected as a max value`() = runTest {
        SPECTROGRAM_OVER_TUNER = true
        FFT_SAMPLE_SIZE = 10
        BUFFER_SIZE = 10
        val viewModel = MainViewModel(audioTrackProvider, fourierTransformHelper)
        this@MainViewModelShould.viewModel = viewModel
        val inputStream = mockk<InputStream>(relaxed = true)
        val bytes = ByteArray(10) { 0 }
        every { fourierTransformHelper.getSpectrogram(any()) } returns List(10) { it.toDouble() }
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        viewModel.setMaxSpectrumFrequency("2000.0")
        stream128Bytes(inputStream = inputStream, bytesToWrite = bytes)
        advanceUntilIdle()
        FFT_SAMPLE_SIZE = 6400
        BUFFER_SIZE = 128
        assertEquals("0.0", viewModel.spectrumStart.value)
        assertEquals("2000.0", viewModel.spectrumEnd.value)
        assertThat(viewModel.spectrogram.value.size, `is`(6))
        assertEquals(0.0, viewModel.spectrogram.value[0], 0.0)
        assertEquals(1.0, viewModel.spectrogram.value[1], 0.0)
        assertEquals(2.0, viewModel.spectrogram.value[2], 0.0)
        assertEquals(3.0, viewModel.spectrogram.value[3], 0.0)
        assertEquals(4.0, viewModel.spectrogram.value[4], 0.0)
        assertEquals(4.0, viewModel.spectrogram.value[4], 0.0)
        assertEquals(5.0, viewModel.spectrogram.value[5], 0.0)
    }

    @Test
    fun `preselect previous frequency value given frequency between samples chosen as a min value - right edge case`() = runTest {
        SPECTROGRAM_OVER_TUNER = true
        FFT_SAMPLE_SIZE = 10
        BUFFER_SIZE = 10
        val viewModel = MainViewModel(audioTrackProvider, fourierTransformHelper)
        this@MainViewModelShould.viewModel = viewModel
        val inputStream = mockk<InputStream>(relaxed = true)
        val bytes = ByteArray(10) { 0 }
        every { fourierTransformHelper.getSpectrogram(any()) } returns List(10) { it.toDouble() }
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        viewModel.setMinSpectrumFrequency("799.9999")
        stream128Bytes(inputStream = inputStream, bytesToWrite = bytes)
        advanceUntilIdle()
        FFT_SAMPLE_SIZE = 6400
        BUFFER_SIZE = 128
        assertEquals("400.0", viewModel.spectrumStart.value)
        assertEquals("3600.0", viewModel.spectrumEnd.value)
        assertThat(viewModel.spectrogram.value.size, `is`(9))
        assertEquals(1.0, viewModel.spectrogram.value[0], 0.0)
    }

    @Test
    fun `preselect previous frequency value given frequency between samples chosen as a min value - left edge case`() = runTest {
        SPECTROGRAM_OVER_TUNER = true
        FFT_SAMPLE_SIZE = 10
        BUFFER_SIZE = 10
        val viewModel = MainViewModel(audioTrackProvider, fourierTransformHelper)
        this@MainViewModelShould.viewModel = viewModel
        val inputStream = mockk<InputStream>(relaxed = true)
        val bytes = ByteArray(10) { 0 }
        every { fourierTransformHelper.getSpectrogram(any()) } returns List(10) { it.toDouble() }
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        viewModel.setMinSpectrumFrequency("400.001")
        stream128Bytes(inputStream = inputStream, bytesToWrite = bytes)
        advanceUntilIdle()
        FFT_SAMPLE_SIZE = 6400
        BUFFER_SIZE = 128
        assertEquals("400.0", viewModel.spectrumStart.value)
        assertEquals("3600.0", viewModel.spectrumEnd.value)
        assertThat(viewModel.spectrogram.value.size, `is`(9))
        assertEquals(1.0, viewModel.spectrogram.value[0], 0.0)
    }

    @Test
    fun `preselect next frequency value given frequency between samples chosen as a max value - right edge case`() = runTest {
        SPECTROGRAM_OVER_TUNER = true
        FFT_SAMPLE_SIZE = 10
        BUFFER_SIZE = 10
        val viewModel = MainViewModel(audioTrackProvider, fourierTransformHelper)
        this@MainViewModelShould.viewModel = viewModel
        val inputStream = mockk<InputStream>(relaxed = true)
        val bytes = ByteArray(10) { 0 }
        every { fourierTransformHelper.getSpectrogram(any()) } returns List(10) { it.toDouble() }
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        viewModel.setMaxSpectrumFrequency("3199.999")
        stream128Bytes(inputStream = inputStream, bytesToWrite = bytes)
        advanceUntilIdle()
        FFT_SAMPLE_SIZE = 6400
        BUFFER_SIZE = 128
        assertEquals("0.0", viewModel.spectrumStart.value)
        assertEquals("3200.0", viewModel.spectrumEnd.value)
        assertThat(viewModel.spectrogram.value.size, `is`(9))
        assertEquals(8.0, viewModel.spectrogram.value.last(), 0.0)
    }

    @Test
    fun `connect device after obtaining manager given GA connect action triggered`() = runTest {
        val socket = mockk<BluetoothSocket>()
        addMyDeviceToBondedDevices()
        every { audioTrackProvider.getAudioTrack() } returns mockk(relaxed = true)
        every { myDevice.createRfcommSocketToServiceRecord(any()) } returns socket
        every { socket.connect() } throws ConnectException()

        viewModel.onConnectionActionReceived()
        viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter, StandardTestDispatcher(testScheduler))
        advanceUntilIdle()

        verify(exactly = 1) { socket.connect() }
    }

    private fun TestScope.stream128Bytes(audioTrack: AudioTrack = mockk(relaxed = true),
                                         inputStream: InputStream = mockk(relaxed = true),
                                         bytesToWrite: ByteArray = ByteArray(128),
                                         testDispatcher: TestDispatcher = StandardTestDispatcher(testScheduler),
                                         times: Int = 1) {
        val socket = mockk<BluetoothSocket>()
        val slot = slot<ByteArray>()
        addMyDeviceToBondedDevices()
        every { myDevice.createRfcommSocketToServiceRecord(any()) } returns socket
        every { socket.connect() } returns Unit
        every { socket.inputStream } returns inputStream
        transferByteArrayNTimesAndFinishStream(inputStream, slot, bytesToWrite, times)
        every { audioTrackProvider.getAudioTrack() } returns audioTrack

        viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        viewModel.connectDevice(testDispatcher)
    }

    private fun transferByteArrayNTimesAndFinishStream(
        inputStream: InputStream,
        slot: CapturingSlot<ByteArray>,
        bytesToWrite: ByteArray,
        n: Int
    ) {
        when(n) {
            1 -> every { inputStream.read(capture(slot)) } answers {
                bytesToWrite.copyInto(slot.captured); 128
            } andThenThrows IOException("Stream interrupted")
            else -> every { inputStream.read(capture(slot)) } returnsMany (List(n) { 128 }) andThenThrows IOException("Stream interrupted")
        }
    }

    private fun addMyDeviceToBondedDevices() {
        every { myDevice.name } returns NAME_OF_THE_DEVICE
        every { bluetoothAdapter.bondedDevices } returns setOf(myDevice)
    }
}