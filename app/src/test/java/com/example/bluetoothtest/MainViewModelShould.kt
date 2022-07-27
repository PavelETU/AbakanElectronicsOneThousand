package com.example.bluetoothtest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.media.AudioTrack
import app.cash.turbine.test
import com.example.bluetoothtest.utils.ResourceWithFormatting
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
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

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel = MainViewModel(audioTrackProvider)
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
            viewModel.toastMessage.first(),
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
        assertThat(viewModel.toastMessage.first(), `is`(ResourceWithFormatting(R.string.no_device_found)))
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

        assertThat(viewModel.toastMessage.first(), `is`(ResourceWithFormatting(R.string.error_while_connecting, null)))
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
    fun `write 160 bytes from BluetoothSocket into audioTrack given connection is established`() = runTest {
        val socket = mockk<BluetoothSocket>()
        val inputStream: InputStream = mockk(relaxed = true)
        val audioTrack: AudioTrack = mockk(relaxed = true)
        addMyDeviceToBondedDevices()
        every { myDevice.createRfcommSocketToServiceRecord(any()) } returns socket
        every { audioTrackProvider.getAudioTrack() } returns mockk(relaxed = true)
        every { socket.connect() } returns Unit
        every { socket.inputStream } returns inputStream
        every { inputStream.read() } returns 0
        every { audioTrackProvider.getAudioTrack() } returns audioTrack

        viewModel.onBluetoothEnabledOrDeviceBonded(bluetoothAdapter)
        viewModel.connectDevice(StandardTestDispatcher(testScheduler))

        advanceUntilIdle()
        val byteArray = ByteArray(160) { 0 }
        verify { audioTrack.write(byteArray, 0,160) }
    }

    private fun addMyDeviceToBondedDevices() {
        every { myDevice.name } returns NAME_OF_THE_DEVICE
        every { bluetoothAdapter.bondedDevices } returns setOf(myDevice)
    }
}