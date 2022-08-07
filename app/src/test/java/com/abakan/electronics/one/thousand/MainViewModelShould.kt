package com.abakan.electronics.one.thousand

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.media.AudioTrack
import app.cash.turbine.test
import com.abakan.electronics.one.thousand.utils.FourierTransformHelper
import com.abakan.electronics.one.thousand.utils.ResourceWithFormatting
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.io.InputStream

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
    fun `do not start FT computation while there are less than 256 bytes during tuning`() = runTest {
        SPECTROGRAM_OVER_TUNER = false
        val inputStream = mockk<InputStream>(relaxed = true)
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        stream128Bytes(inputStream = inputStream, times = 1)
        advanceUntilIdle()
        verify(exactly = 0) { fourierTransformHelper.getPeakFrequency(any()) }
    }

    @Test
    fun `display peak frequency using fft utils given tuning started and 256 bytes received`() = runTest {
        SPECTROGRAM_OVER_TUNER = false
        val inputStream = mockk<InputStream>(relaxed = true)
        val peakFrequency = 40.0
        every { fourierTransformHelper.getPeakFrequency(any()) } returns peakFrequency
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        stream128Bytes(inputStream = inputStream, times = 2)
        advanceUntilIdle()
        verify(exactly = 1) { fourierTransformHelper.getPeakFrequency(any()) }
        assertThat(viewModel.leadingFrequency.value, `is`(40.0))
    }

    @Test
    fun `do not start FT computation while there are less than 256 bytes during spectrogram`() = runTest {
        SPECTROGRAM_OVER_TUNER = true
        val inputStream = mockk<InputStream>(relaxed = true)
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        stream128Bytes(inputStream = inputStream, times = 1)
        advanceUntilIdle()
        verify(exactly = 0) { fourierTransformHelper.getSpectrogram(any()) }
    }

    @Test
    fun `display spectrogram using fft utils given spectrogram started and 256 bytes received`() = runTest {
        SPECTROGRAM_OVER_TUNER = true
        val inputStream = mockk<InputStream>(relaxed = true)
        every { fourierTransformHelper.getSpectrogram(any()) } returns List(256) { 0.0 }
        viewModel.startTuning(StandardTestDispatcher(testScheduler))
        stream128Bytes(inputStream = inputStream, times = 2)
        advanceUntilIdle()
        verify(exactly = 1) { fourierTransformHelper.getSpectrogram(any()) }
        assertThat(viewModel.spectrogram.value, `is`(List(256) { 0.0 } ))
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