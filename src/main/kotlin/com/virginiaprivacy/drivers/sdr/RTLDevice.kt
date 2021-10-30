package com.virginiaprivacy.drivers.sdr

import com.virginiaprivacy.drivers.sdr.data.Status
import com.virginiaprivacy.drivers.sdr.plugins.Plugin
import com.virginiaprivacy.drivers.sdr.plugins.run
import com.virginiaprivacy.drivers.sdr.plugins.scope
import com.virginiaprivacy.drivers.sdr.r2xx.R82XX
import com.virginiaprivacy.drivers.sdr.usb.UsbIFace
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.experimental.or
import kotlin.math.pow
import kotlin.math.roundToInt

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalStdlibApi
open class RTLDevice internal constructor(private val usbDevice: UsbIFace, private val bufferSize: Int = DEFAULT_ASYNC_BUF_COUNT) : Closeable {

    var devLost: Int = 0

    val product by usbDevice::productName

    val serial by usbDevice::serialNumber

    val manufacturer by usbDevice::manufacturerName

    val tunerChip: Tuner by lazy { Tuner.tunerType(this) }

    lateinit var tunableDevice: TunableDevice

    private val scope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    private fun ioStatus() = Status.getIOStatus().value

    val rawFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 12)

    private val buffers = ArrayList<ByteBuffer>(bufferSize)

    fun writeRegMask(reg: Int, value: Int, bitMask: Int) {
        tunableDevice.writeRegMask(reg, value, bitMask)
    }

    fun writeTunerReg(reg: Int, value: Int) {
        tunableDevice.writeReg(reg, value)
    }

    private fun readByte(block: Int, address: UShort): Byte {
        return read(block, 1, address.toInt()).first()
    }

    private fun readArray(block: Int, address: Int, length: Int): ByteArray {
        return read(block, length, address)
    }

    private fun read(block: Int, length: Int, address: Int): ByteArray {
        val index = block shl 8
        val bytes = ByteArray(length)
        usbDevice.controlTransfer(
            CTRL_IN,
            0,
            address,
            index,
            bytes,
            length,
            CTRL_TIMEOUT
        )
        return bytes
    }

    /**
     * Writes an array to the device using a control transfer.
     * @param block - the page/block of the register that is being written to.
     * @param address - the address to write directly to.
     * @param array - the array of bytes to be written.
     * @param length - the amount of bytes to be written.
     * @return Int - The number of bytes that was written.
     */
    private fun writeArray(block: Int, address: UShort, array: ByteArray, length: Int): Int {
        val index = (block shl 8) or 0x10
        return usbDevice.controlTransfer(
            CTRL_OUT,
            0,
            address.toInt(),
            index,
            array,
            length,
            CTRL_TIMEOUT
        )
    }

    private fun readReg(block: Int, address: Int, length: Int): Int {
        val data = UByteArray(length).toByteArray()
        val index = block shl 8
        val result =
            usbDevice.controlTransfer(
                CTRL_IN,
                0,
                address,
                index,
                data,
                length,
                CTRL_TIMEOUT
            )
        checkError(result)
        return when (length) {
            0 -> 0
            1 -> data[0].toInt()
            else -> (data[1].toInt() shl 8 or data[0].toInt())
        }
    }

    fun writeReg(block: Int, address: Int, value: Int, length: Int): Int {
        val data = UByteArray(length)

        val index = (block shl 8) or 0x10
        if (length == 1) {
            data[0] = (value and 0xff).toUByte()
        } else {
            data[0] = ((value.toUInt() shr 8).toUByte())
            data[1] = (value.toUInt() and 255u).toUByte()
        }

        val result = usbDevice.controlTransfer(
            CTRL_OUT,
            0,
            address,
            index,
            data.asByteArray(),
            length,
            CTRL_TIMEOUT
        )
        checkError(result)
        return result
    }

    private fun demodReadReg(page: Int, address: Int, length: Int): Int {
        val data = UByteArray(length)
        val realAddress = (address shl 8) or 32

        val result = usbDevice.controlTransfer(
            CTRL_IN,
            0,
            realAddress,
            page,
            data.toByteArray(),
            length,
            CTRL_TIMEOUT
        )
        checkError(result)
        return when (length) {
            0 -> 0
            1 -> data[0].toInt()
            else -> (data[1].toInt() shl 8 or data[0].toInt())
        }
    }

    /**
     *
     */
    fun demodWriteReg(page: Int, address: Int, value: Int, length: Int): Int {
        val data = UByteArray(length)

        val index = 16 or page

        val realAddress = (address shl 8) or 32

        if (length == 1) {
            data[0] = (value and 0xff).toUByte()
        } else {
            data[0] = (value shr 8).toUByte()
            data[1] = (value and 0xff).toUByte()
        }
        val result = usbDevice.controlTransfer(
            CTRL_OUT,
            0,
            realAddress,
            index,
            data.toByteArray(),
            length,
            CTRL_TIMEOUT
        )
        checkError(result)
        demodReadReg(0x0a, 0x01, 1)
        return result
    }

    /**
     * Sets the tuners gain. Use setTunerGain() or setTunerGain(null) to set the tuner to
     * automatic gain mode.
     */
    fun setTunerGain(gain: Int? = null) {
        setI2cRepeater(1)
        if (gain != null) {
            tunableDevice.setGain(true, gain)
        } else {
            tunableDevice.setGain(false)
        }
        setI2cRepeater(0)
    }

    private fun setFir() {
        val fir = UIntArray(20)
        for (i in 1..8) {
            val f = Companion.fir[i]
            if (f.toInt() !in -128..127) {
                return
            }
            fir[i] = f.toUInt()
        }
        for (i in 0 until 8 step 2) {
            val first = Companion.fir[8 + i].toUInt()
            val second = Companion.fir[8 + i + 1].toUInt()
            if (first.toInt() !in -2048..2047 || second.toInt() !in -2048..2047) {
                throw IllegalArgumentException()
            }
            fir[8 + i * 3 / 2] = (first.shr(4))
            fir[8 + i * 3 / 2 + 1] =
                ((first shl 4) or (((second shr 8) and 15u)))
            fir[8 + i * 3 / 2 + 2] = second
        }

        fir.forEachIndexed { index, uInt ->
            if (demodWriteReg(1, 0x1c + index, uInt.toInt(), 1) != 1) {
                val msg = "FIR[$index]: $uInt did not return 0."
                throw IllegalArgumentException(msg)
            }

        }
    }

    internal fun i2cReadReg(i2cAddress: Int, reg: Int): Int {
        val address = i2cAddress.toUShort()
        writeArray(Blocks.IICB, address, byteArrayOf(reg.toByte()), 1)
        return readArray(Blocks.IICB, i2cAddress, 1)[0].toInt()
    }

    /**
     * Writes data to the I2C address.
     * @param i2cAddress - the starting i2c address.
     * @param buffer - the bytes to write.
     * @param len - the length of the bytes to be written.
     * @returns the number of bytes written to the i2c register.
     */
    internal fun i2cWrite(i2cAddress: Int, buffer: ByteArray, len: Int): Int {
        val address = i2cAddress.toUShort()
        return writeArray(Blocks.IICB, address, buffer, len)
    }

    internal fun i2cRead(i2cAddress: Int, length: Int): ByteArray {
        return readArray(Blocks.IICB, i2cAddress, length)
    }

    fun initBaseband() {

        // initialize USB
        writeReg(Blocks.USBB, UsbReg.USB_SYSCTL, 0x09, 1)
        writeReg(Blocks.USBB, UsbReg.USB_EPA_MAXPKT, 0x0002, 2)
        writeReg(Blocks.USBB, UsbReg.USB_EPA_CTL, 0x1002, 2)

        // Poweron demod
        writeReg(Blocks.SYSB, Reg.DEMOD_CTL_1, 0x22, 1)
        writeReg(Blocks.SYSB, Reg.DEMOD_CTL, 0xe8, 1)

        // reset demod
        demodWriteReg(1, 0x01, 0x14, 1)
        demodWriteReg(1, 0x01, 0x10, 1)

        // disable spectrum inversion and adjacent channel rejection
        demodWriteReg(1, 0x15, 0x00, 1)
        demodWriteReg(1, 0x16, 0x0000, 2)

        // clear DDC shift and IF registers
        for (i in 0..6)
            demodWriteReg(1, 0x16 + i, 0x00, 1)

        setFir()

        // enable SDR mode, disable DAGC (bit 5)
        demodWriteReg(0, 0x19, 0x05, 1)

        // init FSM state-holding register
        demodWriteReg(1, 0x93, 0xf0, 1)
        demodWriteReg(1, 0x94, 0x0f, 1)

        // disable AGC(en_dagc, bit0)
        demodWriteReg(1, 0x11, 0x00, 1)

        //disable RF/IF AGC loop
        demodWriteReg(1, 0x04, 0x00, 1)

        // disable PID filter
        demodWriteReg(0, 0x61, 0x60, 1)

        // opt_adc_iq = 0
        demodWriteReg(0, 0x06, 0x80, 1)

        // enable 0-IF, DC cancellation, IQ estimation/compensation
        demodWriteReg(1, 0xb1, 0x1b, 1)

        // disable 4.096 MHz clock output on pin TP_CK0
        demodWriteReg(0, 0x0d, 0x83, 1)
    }

    fun setup() {
        if (writeReg(
                Blocks.USBB,
                UsbReg.USB_SYSCTL,
                0x09,
                1
            ) < 0
        ) {
            error("Reset device")
        }
        initBaseband()
        devLost = 0
        setI2cRepeater(1)

        // For now and for easier testing, just assume the device is an R820T
        demodWriteReg(1, 0xb1, 0x1a, 1)
        demodWriteReg(0, 0x08, 0x4d, 1)
        setIFFreq(R82XX_IF_FREQ)
        demodWriteReg(1, 0x15, 0x01, 1)

        tunableDevice.init(this)
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
        val high = demodReadReg(0x1, 0x9F, 2)
        val low = demodReadReg(0x1, 0xA1, 2)
        val ratio = Integer.rotateLeft(high, 16) or low
        val sampleRate = tunableDevice.rtlXtal() * TWO_22_POW / ratio
        return sampleRate.roundToInt()
    }

    fun resetBuffer() {
        writeReg(Blocks.USBB, UsbReg.USB_EPA_CTL, 0x1002, 2)
        writeReg(Blocks.USBB, UsbReg.USB_EPA_CTL, 0x0000, 2)
    }

    fun stop() {
        println("Shutting down. . .")
        Status.setIOStatus(IOStatus.EXIT)
        runningPlugins.forEach {
            it.scope.cancel("Shutting down")
        }
        resetBuffer()
        scope.cancel()
        close()
    }

    private val runningPlugins = mutableSetOf<Plugin>()

    fun runPlugin(plugin: Plugin) {
        readAsync()
        plugin.run()
        runningPlugins.add(plugin)
    }

    private fun sampleAsync(samples: Int) {
        var samplesTaken = 0
        var samplesRemaining = samples

    }

    fun readSync() = flow {
        Status.startTime = System.currentTimeMillis()
        Status.bytesRead = 0
        Status.setIOStatus(IOStatus.ACTIVE)
        while (true) {
            val bytes = ByteArray(DEFAULT_BUF_LENGTH)
            val result = usbDevice.bulkTransfer(bytes, DEFAULT_BUF_LENGTH)
            if (result == 0) {
                println("Read empty buffer from device")
                continue
            } else if (result < 0) {
                throw IOException("Error reading from device: $result")
            }
            emit(bytes)
        }
    }

    private fun readAsync() {
        Status.startTime = System.currentTimeMillis()
        Status.bytesRead = 0
        Status.setIOStatus(IOStatus.ACTIVE)
        scope.launch {
            allocateBuffersAsync()

            var bufIndex = 0
            while (this.isActive) {
                if (ioStatus() == IOStatus.ACTIVE) {
                    usbDevice.submitBulkTransfer(buffers[bufIndex])
                    usbDevice.waitForTransferResult().let {
                        val b = buffers[it]
                        val bytesRead = b.position()
                        if (bytesRead == 0) {
                            error("Read 0 bytes on transfer $it")
                        }
                        val bytes = ByteArray(bytesRead)
                        b.rewind()
                        b.get(bytes)
                        rawFlow.emit(bytes)
                        b.clear()
                        Status.bytesRead += (bytesRead)
                    }

                    bufIndex++
                    if (bufIndex == DEFAULT_ASYNC_BUF_COUNT) {
                        bufIndex = 0
                    }
                } else {
                    cancel()
                }
            }
        }
    }


    private suspend fun allocateBuffersAsync() {
        for (i in 0.until(bufferSize)) {
            val buffer = ByteBuffer.allocateDirect(
                DEFAULT_BUF_LENGTH
            )
            buffers.add(buffer)
            usbDevice.prepareNewBulkTransfer(
                i, buffer
            )
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
        demodWriteReg(1, 0x01, i, 1)
    }

    fun setSampleFrequencyCorrection(ppm: Int): Int {
        val offset = (ppm * -1) * 2.0.pow(24) / 1000000
        var tmp = offset.toInt() and 0xff
        var r = 0
        r = r or demodWriteReg(1, 0x3f, tmp, 1)
        tmp = (offset.toInt() shr 8) and 0x3f
        r = r or demodWriteReg(1, 0x3e, tmp, 1)
        return r
    }

    fun setSampleRate(rate: Int): Int {
        var r = 0
        var rsampRatio = 0
        var realRsampRatio = 0
        var realRate = 0.0
        if (rate !in 225001..3200000 || rate in 300001..900000) {
            val msg = "Invalid sample rate: ${rate}Hz"
            throw IllegalArgumentException(msg)
        }
        rsampRatio = (tunableDevice.rtlXtal() * TWO_22_POW / rate).roundToInt()
        rsampRatio = rsampRatio and 0x0ffffffc
        realRsampRatio = rsampRatio or ((rsampRatio and 0x08000000) shl 1)
        realRate = (tunableDevice.rtlXtal() * TWO_22_POW / realRsampRatio)
        println("Exact sample rate set to ${realRate}Hz")
        var tmp = rsampRatio shr 16
        r = (r or demodWriteReg(1, 0x9f, tmp, 2))
        tmp = rsampRatio and 0xffff
        r = (r or demodWriteReg(1, 0xa1, tmp, 2))
        r = (r or setSampleFrequencyCorrection(tunableDevice.ppmCorrection))

        r = (r or demodWriteReg(1, 0x01, 0x14, 1))
        r = (r or demodWriteReg(1, 0x01, 0x10, 1))
        tunableDevice.rate = realRate.roundToInt()

        return r
    }

    fun setAGCMode(enabled: Boolean) {
        val i = if (enabled) 0x25 else 0x05
        demodWriteReg(0, 0x19, i, 1)
    }

    fun setIFFreq(freq: Long) {
        val ifFreq = ((freq * TWO_22_POW) / tunableDevice.getXtalFreq() * (-1)).toInt()
        var i = (ifFreq shr 16) and 0x3f
        demodWriteReg(1, 0x19, i, 1)
        i = (ifFreq shr 8) and 0xff
        demodWriteReg(1, 0x1a, i, 1)
        i = ifFreq and 0xff
        demodWriteReg(1, 0x1b, i, 1)
    }

    fun setCenterFreq(freq: Long) {
        if (tunableDevice.directSampling) {
            setIFFreq(freq)
        }
        setI2cRepeater(1)
        tunableDevice.setFrequency(freq)
        setI2cRepeater(0)
    }

    fun getCenterFreq() = tunableDevice.getTunedFrequency()

    private fun checkError(result: Int, i: UInt? = null, i2: Int? = null) {
        if (result < 0) {
            var msg = "Result of transfer should be > 0. Actual result was $result"
            msg += "\n value $i value as int $i2"
            throw IOException(msg)
        }
    }


    companion object {

        fun getDevice(usbIFace: UsbIFace): RTLDevice {
            usbIFace.claimInterface()
            val dev = R82XX(usbIFace)
            dev.dev.tunableDevice = dev
            return dev.dev
        }

        const val CTRL_TIMEOUT = 300
        const val DEFAULT_RTL_XTAL_FREQ = 288000000
        private const val REQUEST_TYPE_VENDOR: Byte = 0x02 shl 5
        private const val ENDPOINT_IN: Byte = 0x80.toByte()
        private const val ENDPOINT_OUT: Byte = 0x00.toByte()
        const val EEPROM_ADDR = 0xa0
        const val FIR_LEN = 16
        const val R82XX_IF_FREQ: Long = 3570000
        const val R828D_XTAL_FREQ = 16000000

        val CTRL_IN = (REQUEST_TYPE_VENDOR or ENDPOINT_IN).toInt()
        val CTRL_OUT = (REQUEST_TYPE_VENDOR or ENDPOINT_OUT).toInt()

        const val DEFAULT_BUF_COUNT = 15
        const val DEFAULT_ASYNC_BUF_COUNT = 12
        const val DEFAULT_BUF_LENGTH = 262144

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

        private var fir = shortArrayOf(
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

