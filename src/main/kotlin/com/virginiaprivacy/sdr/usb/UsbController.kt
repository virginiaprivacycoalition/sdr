package com.virginiaprivacy.sdr.usb

import com.virginiaprivacy.sdr.exceptions.DeviceException
import com.virginiaprivacy.sdr.exceptions.UsbException
import kotlinx.coroutines.channels.ReceiveChannel
import java.io.Closeable
import java.nio.ByteBuffer
import kotlin.reflect.KClass

/**
 *
 */
interface UsbController: Closeable {

    var deviceOpened: Boolean

    @Throws(DeviceException::class)
    fun open()

    override fun close()

    @kotlin.jvm.Throws(UsbException::class)
    fun write(value: Short, index: Short, buffer: ByteBuffer): Int

    @Throws(UsbException::class)
    fun start(): ReceiveChannel<FloatArray>

    fun stop()

    @kotlin.jvm.Throws(UsbException::class)
    fun read(address: Short, index: Short, buffer: ByteBuffer): Int

    @Throws(UsbException::class)
    fun claimInterface(interfaceNumber: Int): Int

    @Throws(UsbException::class)
    fun releaseInterface(interfaceNumber: Int): Int

    @Throws(DeviceException::class)
    fun release(interfaceNumber: Int): Int

    @Throws(DeviceException::class)
    fun kernelDriverActive(interfaceNumber: Int): Boolean

    @Throws(DeviceException::class)
    fun detachKernelDriver(interfaceNumber: Int)

    fun getErrorMessage(errorCode: Int): String

    fun handleEventsTimeout(): Int

    fun resetDevice()

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

