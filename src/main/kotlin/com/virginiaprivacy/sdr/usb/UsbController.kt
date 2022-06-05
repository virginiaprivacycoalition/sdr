package com.virginiaprivacy.sdr.usb

import com.virginiaprivacy.sdr.exceptions.DeviceException
import com.virginiaprivacy.sdr.exceptions.UsbException
import com.virginiaprivacy.sdr.sample.SampleRate
import com.virginiaprivacy.sdr.toSampleRate
import com.virginiaprivacy.sdr.tuner.RTL2832TunerController
import com.virginiaprivacy.sdr.tuner.TunerGain
import kotlinx.coroutines.channels.ReceiveChannel
import java.io.Closeable
import java.nio.ByteBuffer
import kotlin.reflect.KClass

/**
 *
 */
abstract class UsbController: Closeable {

    abstract var deviceOpened: Boolean

    open lateinit var controller: RTL2832TunerController


    @Throws(DeviceException::class)
    /**
     * This function should open the USB device currently associated with this controller for reading/writing or throw
     * an exception when opening the device fails
     * @throws DeviceException
     * @throws UsbException
     */
    abstract fun open()

    abstract override fun close()

    @kotlin.jvm.Throws(UsbException::class)
    /**
     * This function should perform a control transfer on the control endpoint (endpoint 0).
     * The transfer must be of the OUT direction to write data from the host to the USB Device.
     * @return returns the number of bytes transferred by the control transfer.
     * @throws UsbException if there was an error performing the transfer
     */
    abstract fun write(value: Short, index: Short, buffer: ByteBuffer): Int

    @Throws(UsbException::class)
    abstract fun start(): ReceiveChannel<*>

    abstract fun stop()

    @kotlin.jvm.Throws(UsbException::class)
    /**
     * This function should perform a control transfer on the control endpoint (endpoint 0).
     * The transfer must be of the IN direction for to read data from the device to the host.
     * @return returns the number of bytes transferred by the control transfer.
     * @throws UsbException if there was an error performing the transfer
     */
    abstract fun read(address: Short, index: Short, buffer: ByteBuffer): Int

    @Throws(UsbException::class)
    /**
     * This function should claim exclusive access to read/write to the specified interface of the USB device.
     * @return return 0 if the interface was successfully claimed, otherwise return the error code that will be used to
     * get the correct error message with [getErrorMessage]
     */
    abstract fun claimInterface(interfaceNumber: Int): Int

    @Throws(UsbException::class)
    abstract fun releaseInterface(interfaceNumber: Int): Int

    @Throws(DeviceException::class)
    abstract fun release(interfaceNumber: Int): Int

    @Throws(DeviceException::class)
    abstract fun kernelDriverActive(interfaceNumber: Int): Boolean

    @Throws(DeviceException::class)
    abstract fun detachKernelDriver(interfaceNumber: Int)

    abstract fun getErrorMessage(errorCode: Int): String

    abstract fun handleEventsTimeout(): Int

    abstract fun resetDevice()

    open fun setGain(level: TunerGain) {
        controller.setGain(level)
    }

    open fun setSampleRate(rate: SampleRate) {
        controller.setSampleRate(rate)
    }

    open fun setSampleRate(rate: Int) {
        controller.setSampleRate(rate.toSampleRate())
    }

    open fun currentSampleRate() = controller.mSampleRate.rate

    open fun setCenterFrequency(frequency: Long) {
        controller.tunedFrequency = frequency
    }

    open fun getCenterFrequency() = controller.tunedFrequency

    companion object {
        private val instanceMap = mutableMapOf<String, UsbController>()

        @JvmStatic
        fun <T : UsbController> getInstance(kClass: KClass<T>): T = instanceMap.getOrPut(kClass.qualifiedName!!) {
            val instance =
                kClass.javaObjectType.constructors.firstOrNull { it.parameterCount == 0 && it.trySetAccessible() }
                    ?.newInstance(null) ?: throw IllegalStateException("No default constructor found for ${kClass.qualifiedName}")
            instance as T
        } as T

    }

}

