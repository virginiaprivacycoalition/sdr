package com.virginiaprivacy.sdr.tuner

import com.virginiaprivacy.sdr.exceptions.DeviceException
import com.virginiaprivacy.sdr.exceptions.UsbException
import com.virginiaprivacy.sdr.usb.Descriptor
import com.virginiaprivacy.sdr.sample.SampleMode
import com.virginiaprivacy.sdr.sample.SampleRate
import com.virginiaprivacy.sdr.usb.UsbController
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicInteger
import kotlin.experimental.inv

abstract class RTL2832TunerController(
    open val usbController: UsbController
) {
    var mSampleRate: SampleRate = DEFAULT_SAMPLE_RATE
    private val mSampleCounter: AtomicInteger = AtomicInteger()
    private var mOscillatorFrequency: Int = 28800000
    var mBufferSize: Int = 131072
    protected var mDescriptor: Descriptor? = null
    var devicePulled = false


    @kotlin.Throws(DeviceException::class)
    open fun init(sampleRate: SampleRate = DEFAULT_SAMPLE_RATE) {
        try {
            if (!usbController.deviceOpened) {
                usbController.open()
            }
        } catch (e: DeviceException) {
            throw e
        }
        try {
            setSampleRate(sampleRate)
        } catch (var6: Exception) {
            throw DeviceException("RTL2832 Tuner Controller - couldn't set default sample rate " + var6.message)
        }
        val eeprom: ByteArray = try {
            readEEPROM(0.toShort(), 256)
        } catch (var5: Exception) {
            throw DeviceException("error while reading the EEPROM device descriptor " + var5.message)
        }
        try {
            mDescriptor = Descriptor(eeprom)

        } catch (var7: Exception) {
            println(
                "error while constructing device descriptor using descriptor byte array " + (eeprom.contentToString()) + var7.message
            )
        }
    }


    val descriptor: Descriptor
        get() = if (mDescriptor != null && mDescriptor!!.isValid) mDescriptor!! else throw IllegalStateException("Invalid descriptor")

    @kotlin.Throws(UsbException::class)
    open fun setSamplingMode(mode: SampleMode) {
        when (mode.ordinal) {
            1 -> {
                setIFFrequency(0)
                writeDemodRegister(usbController, Page.ZERO, 8.toShort(), 205, 1)
                writeDemodRegister(usbController, Page.ONE, 177.toShort(), 27, 1)
                writeDemodRegister(usbController, Page.ZERO, 6.toShort(), 128, 1)
                return
            }
            2 -> throw UsbException("QUADRATURE mode is the only mode currently supported")
            else -> throw UsbException("QUADRATURE mode is the only mode currently supported")
        }
    }

    @kotlin.Throws(UsbException::class)
    fun setIFFrequency(frequency: Int) {
        val ifFrequency = 4194304L * frequency.toLong() / mOscillatorFrequency.toLong() * -1L
        writeDemodRegister(usbController
            , Page.ONE, 25.toShort(), (java.lang.Long.rotateRight(ifFrequency, 16) and 63L).toInt()
                .toShort().toInt(), 1
        )
        writeDemodRegister(usbController
            , Page.ONE, 26.toShort(), (java.lang.Long.rotateRight(ifFrequency, 8) and 255L).toInt()
                .toShort().toInt(), 1
        )
        writeDemodRegister(usbController
            , Page.ONE, 27.toShort(), (ifFrequency and 255L).toInt()
                .toShort().toInt(), 1
        )
    }

    @kotlin.Throws(UsbException::class)
    abstract fun initTuner(var1: Boolean)


    val uniqueID: String
        get() = if (mDescriptor != null && mDescriptor!!.hasSerial()) {
            mDescriptor!!.serial
        } else {
            val serial = (255 and descriptor.serial.toInt())
            "SER#$serial"
        }

    @kotlin.Throws(DeviceException::class)
    abstract fun setSampleRateFilters(bandwidth: Int)

    abstract val tunerType: TunerType?

    @kotlin.Throws(DeviceException::class)
    fun release() {
        try {
            usbController.release(0)
        } catch (var2: Exception) {
            throw DeviceException("attempt to release USB interface failed: " + var2.message)
        }
    }

    @kotlin.Throws(UsbException::class)
    fun resetUSBBuffer() {
        writeRegister(usbController, Block.USB, Address.USB_EPA_CTL.mAddress.toShort(), 4098, 2)
        writeRegister(usbController, Block.USB, Address.USB_EPA_CTL.mAddress.toShort(), 0, 2)
    }

    @kotlin.Throws(IllegalArgumentException::class, UsbException::class)
    protected fun deinitBaseband() {
        writeRegister(usbController, Block.SYS, Address.DEMOD_CTL.mAddress.toShort(), 32, 1)
    }

    @get:kotlin.Throws(DeviceException::class)
    protected val isI2CRepeaterEnabled: Boolean
        get() {
            val register = readDemodRegister(usbController, Page.ONE, 1.toShort(), 1)
            return register == 24
        }

    @kotlin.Throws(UsbException::class)
    protected fun writeI2CRegister(
        i2CAddress: Byte,
        i2CRegister: Byte,
        value: Byte,
        controlI2CRepeater: Boolean
    ) {
        val address = (i2CAddress.toInt() and 255).toShort()
        val buffer = ByteBuffer.allocateDirect(2)
        buffer.put(i2CRegister)
        buffer.put(value)
        buffer.rewind()
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, true)
            write(usbController, address, Block.I2C, buffer)
            enableI2CRepeater(usbController, false)
        } else {
            write(usbController, address, Block.I2C, buffer)
        }
    }

    @get:kotlin.Throws(DeviceException::class)
    val currentSampleRate: Int
        get() = mSampleRate.rate

    @get:kotlin.Throws(DeviceException::class)
    val sampleRateFromTuner: Int
        get() {
            try {
                val high = readDemodRegister(usbController, Page.ONE, 159.toShort(), 2)
                val low = readDemodRegister(usbController, Page.ONE, 161.toShort(), 2)
                val ratio = Integer.rotateLeft(high, 16) or low
                val rate = mOscillatorFrequency * 4194304 / ratio
                val sampleRate = SampleRate.getClosest(rate)
                if (sampleRate!!.rate != rate) {
                    setSampleRate(sampleRate)
                    return sampleRate.rate
                }
            } catch (var6: Exception) {
                throw DeviceException("RTL2832 Tuner Controller - cannot get current sample rate " + var6.message)
            }
            return DEFAULT_SAMPLE_RATE.rate
        }

    @kotlin.Throws(DeviceException::class)
    fun setSampleRate(sampleRate: SampleRate) {
        writeDemodRegister(usbController, Page.ONE, 159.toShort(), sampleRate.ratioHighBits, 2)
        writeDemodRegister(usbController, Page.ONE, 161.toShort(), 0, 2)
        sampleRateFrequencyCorrection = 0
        writeDemodRegister(usbController, Page.ONE, 1.toShort(), 20, 1)
        writeDemodRegister(usbController, Page.ONE, 1.toShort(), 16, 1)
        setSampleRateFilters(sampleRate.rate)
        mSampleRate = sampleRate
    }

    @get:kotlin.Throws(UsbException::class)
    @set:kotlin.Throws(DeviceException::class)
    var sampleRateFrequencyCorrection: Int
        get() {
            val high = readDemodRegister(usbController, Page.ONE, 62.toShort(), 1)
            val low = readDemodRegister(usbController, Page.ONE, 63.toShort(), 1)
            return Integer.rotateLeft(high, 8) or low
        }
        set(ppm) {
            val offset = -ppm * 4194304 / 1000000
            writeDemodRegister(usbController, Page.ONE, 63.toShort(), offset and 255, 1)
            writeDemodRegister(usbController, Page.ONE, 62.toShort(), Integer.rotateRight(offset, 8) and 255, 1)
        }

    abstract var tunedFrequency: Long

    @kotlin.Throws(IllegalArgumentException::class)
    fun readEEPROM(offset: Short, length: Int): ByteArray {
        return if (offset + length > 256) {
            throw IllegalArgumentException("cannot read more than 256 bytes from EEPROM - requested to read to byte [" + (offset + length) + "]")
        } else {
            val data = ByteArray(length)
            val buffer = ByteBuffer.allocateDirect(1)
            try {
                writeRegister(usbController, Block.I2C, ((-96).toShort()), offset.toByte().toInt(), 1)
            } catch (_: UsbException) {
            }
            var x = 0
            while (x < length) {
                try {
                    read(usbController, (-96).toShort(), Block.I2C, buffer)
                    data[x] = buffer.get()
                    buffer.rewind()
                } catch (var8: Exception) {
                    x = length
                }
                ++x
            }
            data
        }
    }

    @kotlin.Throws(IllegalArgumentException::class, UsbException::class)
    fun writeEEPROMByte(offset: Byte, value: Byte) {
        if (offset in 0..255) {
            val offsetAndValue = Integer.rotateLeft(255 and offset.toInt(), 8) or (255 and value.toInt())
            writeRegister(usbController, Block.I2C, ((-96).toShort()), offsetAndValue, 2)
        } else {
            throw IllegalArgumentException("RTL2832 Tuner Controller - EEPROM offset must be within range of 0 - 255")
        }
    }

    companion object {
        const val INT_NULL_VALUE = -1
        const val LONG_NULL_VALUE = -1L
        const val DOUBLE_NULL_VALUE = -1.0
        const val TWO_TO_22_POWER = 4194304
        const val USB_INTERFACE: Byte = 0
        const val USB_BULK_ENDPOINT: Byte = -127
        const val CONTROL_ENDPOINT_IN: Byte = -64
        const val CONTROL_ENDPOINT_OUT: Byte = 64
        const val TIMEOUT_US = 1000000L
        const val REQUEST_ZERO: Byte = 0
        const val TRANSFER_BUFFER_POOL_SIZE = 16
        private const val THREAD_POOL_SIZE = 1
        const val EEPROM_ADDRESS: Byte = -96
        val sFIR_COEFFICIENTS =
            byteArrayOf(-54, -36, -41, -40, -32, -14, 14, 53, 6, 80, -100, 13, 113, 17, 20, 113, 116, 25, 65, -91)
        val DEFAULT_SAMPLE_RATE: SampleRate = SampleRate.RATE_0_960MHZ
        private val mDecimalFormatter: DecimalFormat = DecimalFormat("###,###,###.0")
        private val mPercentFormatter: DecimalFormat = DecimalFormat("###.00")

        @kotlin.Throws(DeviceException::class)
        fun claimInterface(usbController: UsbController) {
            if (usbController.kernelDriverActive(0)) {
                try {
                    usbController.detachKernelDriver(0)
                } catch (e: DeviceException) {
                    throw DeviceException("Error detaching kernel driver")
                }
            }
            val result = usbController.claimInterface(0)
            if (result != 0) {
                throw DeviceException("couldn't claim usb interface [" + usbController.getErrorMessage(result) + "]")
            }
        }

        @kotlin.Throws(DeviceException::class)
        fun releaseInterface(controller: UsbController) {
            val result = controller.releaseInterface(0)
            if (result != 0) {
                throw DeviceException("couldn't release interface [" + controller.getErrorMessage(result) + "]")
            }
        }

        @kotlin.Throws(DeviceException::class)
        fun identifyTunerType(controller: UsbController): TunerType {
            try {
                controller.open()
            } catch (e: DeviceException) {
                throw e
            }
            try {
                claimInterface(controller)
                var resetRequired = false
                try {
                    writeRegister(controller, Block.USB, Address.USB_SYSCTL.mAddress.toShort(), 9, 1)
                } catch (e: UsbException) {
                    e.errorCode?.let {
                        if (it >= 0) {
                            throw DeviceException(
                                "Error performing dummy write to device [${
                                    controller.getErrorMessage(
                                        it
                                    )
                                }] " + e.message
                            )
                        }
                    }
                    resetRequired = true
                }
                if (resetRequired) {
                    controller.resetDevice()
                    try {
                        writeRegister(controller, Block.USB, Address.USB_SYSCTL.mAddress.toShort(), 9, 1)
                    } catch (var6: UsbException) {
                        throw DeviceException("device reset attempted, but lost device handle. Try restarting the application to use this device")
                    }
                }
                initBaseband(controller)
                enableI2CRepeater(controller, true)
                val controlI2CRepeater = false
                val tunerClass =
                    if (isTuner(TunerTypeCheck.E4K, controller, controlI2CRepeater)) {
                        TunerType.ELONICS_E4000
                    } else if (isTuner(TunerTypeCheck.FC0013, controller, controlI2CRepeater)) {
                        TunerType.FITIPOWER_FC0013
                    } else if (isTuner(TunerTypeCheck.R820T, controller, controlI2CRepeater)) {
                        TunerType.RAFAELMICRO_R820T
                    } else if (isTuner(TunerTypeCheck.R828D, controller, controlI2CRepeater)) {
                        TunerType.RAFAELMICRO_R828D
                    } else if (isTuner(TunerTypeCheck.FC2580, controller, controlI2CRepeater)) {
                        TunerType.FCI_FC2580
                    } else if (isTuner(TunerTypeCheck.FC0012, controller, controlI2CRepeater)) {
                        TunerType.FITIPOWER_FC0012
                    } else {
                        TunerType.UNKNOWN
                    }

                enableI2CRepeater(controller, false)
                releaseInterface(controller)
                controller.stop()
                return tunerClass
            } catch (var8: Exception) {
                throw DeviceException("error while determining tuner type: " + var8.message)
            }
        }


        @kotlin.Throws(UsbException::class)
        fun initBaseband(controller: UsbController) {
            writeRegister(controller, Block.USB, Address.USB_SYSCTL.mAddress.toShort(), 9, 1)
            writeRegister(controller, Block.USB, Address.USB_EPA_MAXPKT.mAddress.toShort(), 2, 2)
            writeRegister(controller, Block.USB, Address.USB_EPA_CTL.mAddress.toShort(), 4098, 2)
            writeRegister(controller, Block.SYS, Address.DEMOD_CTL_1.mAddress.toShort(), 34, 1)
            writeRegister(controller, Block.SYS, Address.DEMOD_CTL.mAddress.toShort(), 232, 1)
            writeDemodRegister(controller, Page.ONE, 1.toShort(), 20, 1)
            writeDemodRegister(controller, Page.ONE, 1.toShort(), 16, 1)
            writeDemodRegister(controller, Page.ONE, 21.toShort(), 0, 1)
            writeDemodRegister(controller, Page.ONE, 22.toShort(), 0, 2)
            writeDemodRegister(controller, Page.ONE, 22.toShort(), 0, 1)
            writeDemodRegister(controller, Page.ONE, 23.toShort(), 0, 1)
            writeDemodRegister(controller, Page.ONE, 24.toShort(), 0, 1)
            writeDemodRegister(controller, Page.ONE, 25.toShort(), 0, 1)
            writeDemodRegister(controller, Page.ONE, 26.toShort(), 0, 1)
            writeDemodRegister(controller, Page.ONE, 27.toShort(), 0, 1)
            for (x in sFIR_COEFFICIENTS.indices) {
                writeDemodRegister(controller, Page.ONE, (28 + x).toShort(), sFIR_COEFFICIENTS[x].toInt(), 1)
            }
            writeDemodRegister(controller, Page.ZERO, 25.toShort(), 5, 1)
            writeDemodRegister(controller, Page.ONE, 147.toShort(), 240, 1)
            writeDemodRegister(controller, Page.ONE, 148.toShort(), 15, 1)
            writeDemodRegister(controller, Page.ONE, 17.toShort(), 0, 1)
            writeDemodRegister(controller, Page.ONE, 4.toShort(), 0, 1)
            writeDemodRegister(controller, Page.ZERO, 97.toShort(), 96, 1)
            writeDemodRegister(controller, Page.ZERO, 6.toShort(), 128, 1)
            writeDemodRegister(controller, Page.ONE, 177.toShort(), 27, 1)
            writeDemodRegister(controller, Page.ZERO, 13.toShort(), 131, 1)
        }

        @kotlin.Throws(UsbException::class)
        protected fun setGPIOBit(controller: UsbController, bitMask: Byte, enabled: Boolean) {
            var value = readRegister(controller, Block.SYS, Address.GPO.mAddress.toShort(), 1)
            value = if (enabled) {
                value or bitMask.toInt()
            } else {
                value and bitMask.inv().toInt()
            }
            writeRegister(controller, Block.SYS, Address.GPO.mAddress.toShort(), value, 1)
        }

        @kotlin.Throws(UsbException::class)
        protected fun setGPIOOutput(controller: UsbController, bitMask: Byte) {
            var value = readRegister(controller, Block.SYS, Address.GPD.mAddress.toShort(), 1)
            writeRegister(controller, Block.SYS, Address.GPO.mAddress.toShort(), value and bitMask.inv().toInt(), 1)
            value = readRegister(controller, Block.SYS, Address.GPOE.mAddress.toShort(), 1)
            writeRegister(controller, Block.SYS, Address.GPOE.mAddress.toShort(), value or bitMask.toInt(), 1)
        }

        @JvmStatic
        @kotlin.Throws(UsbException::class)
        protected fun enableI2CRepeater(controller: UsbController, enabled: Boolean) {
            val page = Page.ONE
            val address: Short = 1
            val value: Byte = if (enabled) {
                24
            } else {
                16
            }
            writeDemodRegister(controller, page, address, value.toInt(), 1)
        }

        @JvmStatic
        @kotlin.Throws(UsbException::class)
        protected fun readI2CRegister(
            controller: UsbController,
            i2CAddress: Byte,
            i2CRegister: Byte,
            controlI2CRepeater: Boolean
        ): Int {
            val address = (i2CAddress.toInt() and 255).toShort()
            val buffer = ByteBuffer.allocateDirect(1)
            buffer.put(i2CRegister)
            buffer.rewind()
            val data = ByteBuffer.allocateDirect(1)
            if (controlI2CRepeater) {
                enableI2CRepeater(controller, true)
                write(controller, address, Block.I2C, buffer)
                read(controller, address, Block.I2C, data)
                enableI2CRepeater(controller, false)
            } else {
                write(controller, address, Block.I2C, buffer)
                read(controller, address, Block.I2C, data)
            }
            return data.get().toInt() and 255
        }

        @JvmStatic
        @kotlin.Throws(UsbException::class)
        protected fun writeDemodRegister(controller: UsbController, page: Page, address: Short, value: Int, length: Int) {
            val buffer = ByteBuffer.allocateDirect(length)
            buffer.order(ByteOrder.BIG_ENDIAN)
            if (length == 1) {
                buffer.put((value and 255).toByte())
            } else {
                require(length == 2) { "Cannot write value greater than 16 bits to the register - length [$length]" }
                buffer.putShort((value and '\uffff'.code).toShort())
            }
            val index = (16 or page.mPage).toShort()
            val newAddress = (address.toInt() shl 8 or 32).toShort()
            write(controller, newAddress, index, buffer)
            readDemodRegister(controller, Page.TEN, 1.toShort(), length)
        }

        @kotlin.Throws(UsbException::class)
        protected fun readDemodRegister(controller: UsbController, page: Page, address: Short, length: Int): Int {
            val index = page.mPage.toShort()
            val newAddress = (address.toInt() shl 8 or 32).toShort()
            val buffer = ByteBuffer.allocateDirect(length)
            read(controller, newAddress, index, buffer)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            return if (length == 2) buffer.short.toInt() and '\uffff'.code else buffer.get().toInt() and 255
        }

        @JvmStatic
        @kotlin.Throws(UsbException::class)
        protected fun writeRegister(controller: UsbController, block: Block, address: Short, value: Int, length: Int) {
            val buffer = ByteBuffer.allocateDirect(length)
            buffer.order(ByteOrder.BIG_ENDIAN)
            if (length == 1) {
                buffer.put((value and 255).toByte())
            } else {
                require(length == 2) { "Cannot write value greater than 16 bits to the register - length [$length]" }
                buffer.putShort(value.toShort())
            }
            buffer.rewind()
            write(controller, address, block, buffer)
        }

        @kotlin.Throws(UsbException::class)
        protected fun readRegister(controller: UsbController, block: Block, address: Short, length: Int): Int {
            val buffer = ByteBuffer.allocateDirect(2)
            read(controller, address, block, buffer)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            return if (length == 2) buffer.short.toInt() and '\uffff'.code else buffer.get().toInt() and 255
        }

        @kotlin.Throws(UsbException::class)
        protected fun write(controller: UsbController, address: Short, block: Block, buffer: ByteBuffer) {
            write(controller, address, block.writeIndex, buffer)
        }

        protected fun write(controller: UsbController, value: Short, index: Short, buffer: ByteBuffer) {
            val transferred =
                controller.write(value, index, buffer)
            if (transferred < 0) {
                throw UsbException("Error writing byte buffer: ${controller.getErrorMessage(transferred)}")
            } else if (transferred != buffer.capacity()) {
                throw UsbException("transferred bytes [" + transferred + "] is not what was expected [" + buffer.capacity() + "]")
            }
        }

        @kotlin.Throws(UsbException::class)
        protected fun read(controller: UsbController, address: Short, index: Short, buffer: ByteBuffer) {
            val transferred = controller.read(address, index, buffer)
            if (transferred < 0) {
                throw UsbException("Read Error: ${controller.getErrorMessage(transferred)}")
            } else if (transferred != buffer.capacity()) {
                throw UsbException(
                    "transferred bytes [" + transferred + "] is not what was expected [" + buffer.capacity() + "]"
                )
            }
        }

        @JvmStatic
        @kotlin.Throws(UsbException::class)
        protected fun read(controller: UsbController, address: Short, block: Block, buffer: ByteBuffer) {
            read(controller, address, block.readIndex, buffer)
        }

        protected fun isTuner(type: TunerTypeCheck, controller: UsbController, controlI2CRepeater: Boolean): Boolean {
            return try {
                if (type == TunerTypeCheck.FC0012 || type == TunerTypeCheck.FC2580) {
                    setGPIOOutput(controller, 32.toByte())
                    setGPIOBit(controller, 32.toByte(), true)
                    setGPIOBit(controller, 32.toByte(), false)
                }
                val value = readI2CRegister(controller, type.i2CAddress, type.checkAddress, controlI2CRepeater)
                if (type == TunerTypeCheck.FC2580) {
                    value and 127 == type.checkValue.toInt()
                } else {
                    value == type.checkValue.toInt()
                }
            } catch (var4: UsbException) {
                false
            }
        }

        fun getTransferStatus(status: Int): String {
            return when (status) {
                0 -> "TRANSFER COMPLETED (0)"
                1 -> "TRANSFER ERROR (1)"
                2 -> "TRANSFER TIMED OUT (2)"
                3 -> "TRANSFER CANCELLED (3)"
                4 -> "TRANSFER STALL (4)"
                5 -> "TRANSFER NO DEVICE (5)"
                6 -> "TRANSFER OVERFLOW (6)"
                else -> "UNKNOWN TRANSFER STATUS ($status)"
            }
        }
    }
}