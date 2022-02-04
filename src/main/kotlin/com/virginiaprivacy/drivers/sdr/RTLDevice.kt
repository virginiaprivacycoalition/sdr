package com.virginiaprivacy.drivers.sdr

import com.virginiaprivacy.drivers.sdr.plugins.Plugin
import com.virginiaprivacy.drivers.sdr.plugins.scope
import com.virginiaprivacy.drivers.sdr.usb.ResultStatus
import com.virginiaprivacy.drivers.sdr.usb.UsbIFace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.pow
import kotlin.math.roundToInt

abstract class RTLDevice(
    private val usbDevice: UsbIFace,
    private val bufferSize: Int = BUF_BYTES,
    private val numBuffers: Int = DEFAULT_ASYNC_BUF_COUNT
) : Closeable, TunableDevice {

    private var devLost: Int = 0

    val product by usbDevice::productName

    val serial by usbDevice::serialNumber

    val manufacturer by usbDevice::manufacturerName

    val bufferSampleSize = bufferSize / 2

    val tunerChip: Tuner by lazy { Tuner.tunerType(this) }

    val tunerGain by lazy { actualGain }

    private val context = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    private val scope = CoroutineScope(
        context
    )

    val tunerAutoGain: Boolean by ::tunerAutoGainEnabled

    var agcMode: Boolean
        get() {
            val result = demodulatorReadReg(0, 0x19, 1)
            return result == 0x25
        }
        set(value) {
            val i = if (value) 0x25 else 0x05
            demodulatorWriteReg(0, 0x19, i, 1)
        }

    val rawFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = numBuffers)


    private val bufferMutex = Mutex()

    private val buffers = mutableListOf<ByteBuffer>()

    fun writeTunerReg(reg: Int, value: Int) {
        writeReg(reg, value)
    }

    private fun readByte(block: Int, address: UShort): Byte {
        return read(block, 1, address.toInt()).first()
    }

    private fun readArray(block: Int, address: Int, length: Int): ByteArray {
        return read(block, length, address)
    }

    private fun read(block: Int, length: Int, address: Int): ByteArray {
        val index = block shl 8
        val buf = ByteBuffer.allocateDirect(length)
        usbDevice.controlTransfer(
            CTRL_IN,
            address.toShort(),
            index.toShort(),
            buf,
            length,
            CTRL_TIMEOUT
        )
        val bytes = ByteArray(length)
        buf.get(bytes)
        return bytes
    }

    /**
     *
     * Writes an array to the device using a control transfer.
     * @param block - the page/block of the register that is being written to.
     * @param address - the address to write directly to.
     * @param array - the array of bytes to be written.
     * @param length - the amount of bytes to be written.
     * @return Int - The number of bytes that was written.
     */
    private fun writeArray(block: Int, address: Short, array: ByteArray, length: Int) {
        val index = (block shl 8) or 0x10
        val buffer = ByteBuffer.allocateDirect(length)
        buffer.put(array)

        usbDevice.controlTransfer(
            CTRL_OUT,
            address,
            index.toShort(),
            buffer,
            length,
            CTRL_TIMEOUT
        )
    }

    private fun readReg(block: Int, address: Int, length: Int): Int {
        val data = ByteBuffer.allocateDirect(2)
        val index = block shl 8
        val result =
            usbDevice.controlTransfer(
                CTRL_IN,
                address.toShort(),
                index.toShort(),
                data,
                length,
                CTRL_TIMEOUT
            )
        return when (result.status) {
            is ResultStatus.Completed ->
                if (length == 1) (data.apply { order(ByteOrder.LITTLE_ENDIAN) }).get()
                    .toInt() and 0xff else data.short.toInt() and 0xffff
            else -> error(result)
        }
    }

    private fun writeReg(block: Int, address: Int, value: Int, length: Int) {
        val buffer = ByteBuffer.allocateDirect(length)

        val index = block.rotateLeft(8).toShort() or 16
        when (length) {
            1 -> buffer.put((value and 0xff).toByte())
            2 -> buffer.putShort((value and 0xFFFF).toShort())
            else -> throw IllegalArgumentException("Max size of data is 2 bytes")
        }

        val result = usbDevice.controlTransfer(
            CTRL_OUT,
            address.toShort(),
            index,
            buffer,
            length,
            CTRL_TIMEOUT
        )

        when (result.status) {
            is ResultStatus.Error -> println("Transfer failed: $result")
            else -> {
            }
        }
    }

    private fun demodulatorReadReg(page: Int, address: Int, length: Int): Int {
        val buffer = ByteBuffer.allocateDirect(length)
        val realAddress = (address shl 8) or 0x20
        val result = usbDevice.controlTransfer(
            CTRL_IN,
            realAddress.toShort(),
            page.toShort(),
            buffer,
            length,
            CTRL_TIMEOUT
        )
        if (result.status !is ResultStatus.Success) {
            error(result)
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return when (length) {
            1 -> (buffer.get() and 0xff).toInt()
            2 -> buffer.short.and(0xffff.toShort()).toInt()
            else -> throw IllegalArgumentException("Max size of data is 2 bytes")
        }
    }

    /**
     *
     */
    fun demodulatorWriteReg(page: Int, address: Int, value: Int, length: Int) {
        val buffer = ByteBuffer.allocateDirect(length)

        val index = 16 or page

        val realAddress = address shl 8 or 32

        when (length) {
            1 -> buffer.put((value and 0xff).toByte())
            2 -> buffer.putShort((value and 0xFFFF).toShort())
            else -> throw IllegalArgumentException("Max size of data is 2 bytes")
        }

        val result = usbDevice.controlTransfer(
            CTRL_OUT,
            realAddress.toShort(),
            index.toShort(),
            buffer,
            length,
            CTRL_TIMEOUT
        )
        demodulatorReadReg(0x0a, 0x01, 1)
        when (result.status) {
            is ResultStatus.Error -> {
                error(result)
            }
            else -> return
        }
    }

    fun automaticGainControl(enabled: Boolean) {
        setI2cRepeater(1)
        tunerAutoGainEnabled = enabled
        setI2cRepeater(0)
    }

    /**
     * Sets the tuners gain. Use setTunerGain() or setTunerGain(null) to set the tuner to
     * automatic gain mode.
     */
    @Deprecated("Use TunableGain.autoGainEnabled for now")
    fun setTunerGain(gain: Int? = null) {
        setI2cRepeater(1)
        if (gain != null) {
            setGain(true, gain)
        } else {
            setGain(false)
        }
        setI2cRepeater(0)
    }

    fun setLNAGain(gain: LNA_GAIN) {
        setI2cRepeater(1)
        (this as TunableGain).run {
            lnaGain = gain
        }
        setI2cRepeater(0)
    }

    fun setVGAGain(gain: VGA_GAIN) {
        setI2cRepeater(1)
        (this as TunableGain).run {
            vgaGain = gain
        }
        setI2cRepeater(0)
    }

    private fun setFir() {
        val fir = IntArray(20)
        for (i in 1..8) {
            val f = Companion.fir[i]
            if (f !in -128..127) {
                return
            }
            fir[i] = f
        }
        for (i in 0 until 8 step 2) {
            val first = Companion.fir[8 + i]
            val second = Companion.fir[8 + i + 1]
            if (first !in -2048..2047 || second !in -2048..2047) {
                throw IllegalArgumentException()
            }
            fir[8 + i * 3 / 2] = (first.shr(4))
            fir[8 + i * 3 / 2 + 1] =
                ((first shl 4) or (((second shr 8) and 15)))
            fir[8 + i * 3 / 2 + 2] = second
        }

        fir.forEachIndexed { index, uInt ->
            demodulatorWriteReg(1, 0x1c + index, uInt, 1)
        }
    }

    internal fun i2cReadReg(i2cAddress: Int, reg: Int): Int {
        val address = i2cAddress.toUShort()
        writeArray(Blocks.IICB, address.toShort(), byteArrayOf(reg.toByte()), 1)
        return readArray(Blocks.IICB, i2cAddress, 1)[0].toInt()
    }

    /**
     * Writes data to the I2C address.
     * @param i2cAddress - the starting i2c address.
     * @param buffer - the bytes to write.
     * @param len - the length of the bytes to be written.
     * @returns the number of bytes written to the i2c register.
     */
    internal fun i2cWrite(i2cAddress: Int, buffer: ByteArray, len: Int) {
        val address = i2cAddress.toShort()
        writeArray(Blocks.IICB, address, buffer, len)
    }

    internal fun i2cRead(i2cAddress: Int, length: Int): ByteArray {
        return readArray(Blocks.IICB, i2cAddress, length)
    }

    private fun initBaseband() {

        // initialize USB
        writeReg(Blocks.USBB, UsbReg.USB_SYSCTL, 0x09, 1)
        writeReg(Blocks.USBB, UsbReg.USB_EPA_MAXPKT, 0x0002, 2)
        writeReg(Blocks.USBB, UsbReg.USB_EPA_CTL, 0x1002, 2)

        // Poweron demod
        writeReg(Blocks.SYSB, Reg.DEMOD_CTL_1, 0x22, 1)
        writeReg(Blocks.SYSB, Reg.DEMOD_CTL, 0xe8, 1)

        // reset demod
        demodulatorWriteReg(1, 0x01, 0x14, 1)
        demodulatorWriteReg(1, 0x01, 0x10, 1)

        // disable spectrum inversion and adjacent channel rejection
        demodulatorWriteReg(1, 0x15, 0x00, 1)
        demodulatorWriteReg(1, 0x16, 0x0000, 2)

        // clear DDC shift and IF registers
        for (i in 0..6)
            demodulatorWriteReg(1, 0x16 + i, 0x00, 1)

        setFir()

        // enable SDR mode, disable DAGC (bit 5)
        demodulatorWriteReg(0, 0x19, 0x05, 1)

        // init FSM state-holding register
        demodulatorWriteReg(1, 0x93, 0xf0, 1)
        demodulatorWriteReg(1, 0x94, 0x0f, 1)

        // disable AGC(en_dagc, bit0)
        demodulatorWriteReg(1, 0x11, 0x00, 1)

        //disable RF/IF AGC loop
        demodulatorWriteReg(1, 0x04, 0x00, 1)

        // disable PID filter
        demodulatorWriteReg(0, 0x61, 0x60, 1)

        // opt_adc_iq = 0
        demodulatorWriteReg(0, 0x06, 0x80, 1)

        // enable 0-IF, DC cancellation, IQ estimation/compensation
        demodulatorWriteReg(1, 0xb1, 0x1b, 1)

        // disable 4.096 MHz clock output on pin TP_CK0
        demodulatorWriteReg(0, 0x0d, 0x83, 1)
    }

    fun setup() {
        repeat(2) {
            runCatching {
                writeReg(
                    Blocks.USBB,
                    UsbReg.USB_SYSCTL,
                    0x09,
                    1
                )
            }.fold(onFailure = {
                println("Resetting device. . .")
                usbDevice.resetDevice()
            }, onSuccess = {
                return@repeat
            })
        }
        initBaseband()
        devLost = 0
        setI2cRepeater(1)

        init()
        setI2cRepeater(0)

        println("Device successfully configured: $product by $manufacturer with serial no $serial")
    }

    fun setBiasTee(enabled: Boolean) {
        setgpioOutput(0)
        val intValue = if (enabled) 1 else 0
        setgpioBit(0, intValue)
        println("bias tee now is $enabled")
    }

    fun getTunerSampleRate(): Int {
        val high = demodulatorReadReg(0x1, 0x9F, 2)
        val low = demodulatorReadReg(0x1, 0xA1, 2)
        val ratio = Integer.rotateLeft(high, 16) or low
        val sampleRate = rtlXtal() * TWO_22_POW / ratio
        return sampleRate.roundToInt()
    }

    fun resetBuffer() {
        writeReg(Blocks.USBB, UsbReg.USB_EPA_CTL, 0x1002, 2)
        writeReg(Blocks.USBB, UsbReg.USB_EPA_CTL, 0x0000, 2)
    }

    fun stop() {
        println("Shutting down. . .")
        usbDevice.resetDevice()
        scope.cancel()
        close()
    }

    fun <T> runPlugin(plugin: Plugin<ByteArray, T>): Flow<T> {
        allocateBuffersAsync()
        buffers.forEach { _ ->
            usbDevice.submitBulkTransfer()
        }

        return plugin.invoke(
            usbDevice.readBytes().shareIn(scope, SharingStarted.WhileSubscribed())
        )
            .buffer()
    }


    private fun allocateBuffersAsync() {
        for (i in buffers.size until (numBuffers)) {
            val buffer = ByteBuffer.allocateDirect(
                bufferSize
            )
            buffers.add(buffer)
            usbDevice.prepareNewBulkTransfer(buffer)
        }
    }

    private fun setgpioBit(gpio: Int, value: Int) {
        val gpio2 = 1 shl gpio
        var result = readReg(Blocks.SYSB, Reg.GPO, 1)
        result = if (value == 1) {
            (result or gpio)
        } else {
            result and (gpio2.inv())
        }
        writeReg(Blocks.SYSB, Reg.GPO, result, 1)
    }

    private fun setgpioOutput(gpio: Int) {
        val gpio2 = 1 shl gpio
        val result = readReg(Blocks.SYSB, Reg.GPD, 1)
        writeReg(Blocks.SYSB, Reg.GPD, result and gpio2.inv(), 1)
        val result2 = readReg(Blocks.SYSB, Reg.GPOE, 1)
        writeReg(Blocks.SYSB, Reg.GPOE, result2 or gpio2, 1)
    }

    fun setI2cRepeater(on: Int) {
        val i = when (on == 1) {
            true -> 0x18
            else -> 0x10
        }
        demodulatorWriteReg(1, 0x01, i, 1)
    }

    fun setSampleFrequencyCorrection(ppm: Int) {
        val offset = (ppm * -1) * 2.0.pow(24) / 1000000
        var tmp = offset.toInt() and 0xff
        var r = 0
        demodulatorWriteReg(1, 0x3f, tmp, 1)
        tmp = (offset.toInt() shr 8) and 0x3f
        demodulatorWriteReg(1, 0x3e, tmp, 1)
    }

    fun setSampleRate(rate: Int) {
        var realRsampRatio = 0
        var realRate = 0.0
        if ((rate !in (22500..3200000)) || (rate in (300001..900000))) {
            val msg = "Invalid sample rate: ${rate}Hz"
            throw IllegalArgumentException(msg)
        }
        var rsampRatio: Int = (rtlXtal() * TWO_22_POW / rate).roundToInt()
        rsampRatio = rsampRatio and 0x0ffffffc
        realRsampRatio = rsampRatio or ((rsampRatio and 0x08000000) shl 1)
        realRate = (rtlXtal() * TWO_22_POW / realRsampRatio)
        println("Exact sample rate set to ${realRate}Hz")
        var tmp = rsampRatio shr 16
        demodulatorWriteReg(1, 0x9f, tmp, 2)
        tmp = rsampRatio and 0xffff
        demodulatorWriteReg(1, 0xa1, tmp, 2)
        setSampleFrequencyCorrection(ppmCorrection)
        demodulatorWriteReg(1, 0x01, 0x14, 1)
        demodulatorWriteReg(1, 0x01, 0x10, 1)
        this.rate = realRate.roundToInt()

    }

    fun setIFFreq(freq: Long) {
        val ifFreq = ((freq * TWO_22_POW) / getXtalFreq() * (-1)).toInt()

        // write byte 2
        var i = (ifFreq shr 16) and 0x3f
        demodulatorWriteReg(1, 0x19, i, 1)

        // write byte one
        i = (ifFreq shr 8) and 0xff
        demodulatorWriteReg(1, 0x1a, i, 1)

        // write byte 2
        i = ifFreq and 0xff
        demodulatorWriteReg(1, 0x1b, i, 1)
    }

    fun setCenterFreq(freq: Long) {
        if (directSampling) {
            setIFFreq(freq)
        }
        setI2cRepeater(1)
        setFrequency(freq)
        setI2cRepeater(0)
    }

    fun getCenterFreq() = getTunedFrequency()

    private fun checkError(result: Int, i: UInt? = null, i2: Int? = null) {
        if (result < 0) {
            var msg = "Result of transfer should be > 0. Actual result was $result"
            msg += "\n value $i value as int $i2"
            throw IOException(msg)
        }
    }


    companion object {


        const val CTRL_TIMEOUT = 300
        const val DEFAULT_RTL_XTAL_FREQ = 28800000
        private const val REQUEST_TYPE_VENDOR: Byte = 0x02 shl 5
        private const val ENDPOINT_IN: Byte = 0x80.toByte()
        private const val ENDPOINT_OUT: Byte = 0x00.toByte()
        const val EEPROM_ADDR = 0xa0
        const val R82XX_IF_FREQ: Long = 3570000
        const val R828D_XTAL_FREQ = 16000000

        val CTRL_IN = (REQUEST_TYPE_VENDOR or ENDPOINT_IN).toInt()
        val CTRL_OUT = (REQUEST_TYPE_VENDOR or ENDPOINT_OUT).toInt()

        const val DEFAULT_ASYNC_BUF_COUNT = 12
        const val BUF_BYTES = 262144

        val ENDPOINT_TYPES = arrayOf(
            "USB_ENDPOINT_XFER_CONTROL",
            "USB_ENDPOINT_XFER_ISOC",
            "USB_ENDPOINT_XFER_BULK",
            "USB_ENDPOINT_XFER_INT"
        )

        object Blocks {
            const val DEMODB = 0
            const val USBB = 1
            const val SYSB = 2
            const val TUNB = 3
            const val ROMB = 4
            const val IRB = 5
            const val IICB = 6
        }

        object Reg {
            const val DEMOD_CTL = 0x3000
            const val GPO = 0x3001
            const val GPI = 0x3002
            const val GPOE = 0x3003
            const val GPD = 0x3004
            const val SYSINTE = 0x3005
            const val SYSINTS = 0x3006
            const val GP_CFG0 = 0x3007
            const val GP_CFG1 = 0x3008
            const val SYSINTE_1 = 0x3009
            const val SYSINTS_1 = 0x300a
            const val DEMOD_CTL_1 = 0x300b
            const val IR_SUSPEND = 0x300c
        }

        object UsbReg {
            const val USB_SYSCTL = 0x2000
            const val USB_CTRL = 0x2010
            const val USB_STAT = 0x2014
            const val USB_EPA_CFG = 0x2144
            const val USB_EPA_CTL = 0x2148
            const val USB_EPA_MAXPKT = 0x2158
            const val USB_EPA_MAXPKT_2 = 0x215a
            const val USB_EPA_FIFO_CFG = 0x2160
        }

        private var fir = intArrayOf(
            -54, -36, -41, -40, -32, -14, 14, 53,    /* 8 bit signed */
            101, 156, 215, 273, 327, 372, 404, 421    /* 12 bit signed */
        )
        private val TWO_22_POW: Double = 2.0.pow(22)

    }


    override fun close() {
        usbDevice.releaseUsbDevice()
        scope.cancel()
    }


}

private infix fun Short.shr(i: Int): Short = (this.toInt() shr i).toShort()


