package com.virginiaprivacy.drivers.sdr

import com.virginiaprivacy.drivers.sdr.plugins.Plugin
import com.virginiaprivacy.drivers.sdr.usb.ResultStatus
import com.virginiaprivacy.drivers.sdr.usb.UsbIFace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val scope = CoroutineScope(
        context
    )

    val tunerAutoGain: Boolean by ::tunerAutoGainEnabled

    var agcMode: Boolean
        get() {
            val result = readDemodRegister(0, 0x19, 1)
            return result == 0x25
        }
        set(value) {
            val i = if (value) 0x25 else 0x05
            writeDemodRegister(0, 0x19, i, 1)
        }

    val rawFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = numBuffers)


    private val buffers = mutableListOf<ByteBuffer>()


    fun automaticGainControl(enabled: Boolean) {
        enableI2CRepeater(true)
        tunerAutoGainEnabled = enabled
        enableI2CRepeater(false)
    }

    /**
     * Sets the tuners gain. Use setTunerGain() or setTunerGain(null) to set the tuner to
     * automatic gain mode.
     */
    @Deprecated("Use TunableGain.autoGainEnabled for now")
    fun setTunerGain(gain: Int? = null) {
        enableI2CRepeater(true)
        if (gain != null) {
            setGain(true, gain)
        } else {
            setGain(false)
        }
        enableI2CRepeater(false)
    }

    fun setLNAGain(gain: LNA_GAIN) {
        enableI2CRepeater(true)
        (this as TunableGain).run {
            lnaGain = gain
        }
        enableI2CRepeater(false)
    }

    fun setVGAGain(gain: VGA_GAIN) {
        enableI2CRepeater(true)
        (this as TunableGain).run {
            vgaGain = gain
        }
        enableI2CRepeater(false)
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
            writeDemodRegister(1, (0x1c + index).toShort(), uInt, 1)
        }
    }

    fun i2cReadRegister(i2cAddress: Byte, i2cRegister: Byte): Int {
        val address = (i2cAddress and 0xFF).toShort()
        val buffer = ByteBuffer.allocateDirect(1)
        buffer.put(i2cRegister)
        buffer.rewind()
        val output = ByteBuffer.allocateDirect(1)
        write(address, Blocks.IICB, buffer)
        read(address, Blocks.IICB, output)
        return output.get().toInt() and 0xFF
    }

    fun i2cWriteRegister(i2cAddress: Byte, i2cRegister: Byte, value: Byte) {
        val address = (i2cAddress.toShort() and 0xFF)
        val buffer = ByteBuffer.allocateDirect(2)
            .apply {
                put(i2cRegister)
                put(value)
                rewind()
            }
        enableI2CRepeater(true)
        write(address, (Blocks.IICB.rotateLeft(8) or 0x10), buffer)
        enableI2CRepeater(false)
    }

    fun writeDemodRegister(page: Int, address: Short, value: Int, length: Int) {
        val buffer = ByteBuffer.allocateDirect(length)
            .apply {
                order(ByteOrder.BIG_ENDIAN)
                when (length) {
                    1 -> put((value and 0xFF).toByte())
                    2 -> putShort((value and 0xFFFF).toShort())
                    else -> throw IOException("Cannot write $length bytes to the demod register. Max is 2 bytes")
                }
            }
        val index = (0x10 or page).toShort()
        val newAddress = (address.toInt().shl(8) or 0x20).toShort()
        write(newAddress, index, buffer)

        readDemodRegister(0xA, 1.toShort(), length)
    }

    fun readDemodRegister(page: Byte, address: Short, length: Int): Int {
        val newAddress = (address.toInt() shl 8 or 0x20).toShort()
        val buffer = ByteBuffer.allocateDirect(length)
        read(newAddress, page.toShort(), buffer)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return if (length == 2) { buffer.short.toInt() and 0xFFFF } else { buffer.get().toInt() and 0xFF }
    }

    fun writeRegister(block: Short, address: Short, value: Int, length: Int) {
        val buffer = ByteBuffer.allocateDirect(length).apply {
            order(ByteOrder.BIG_ENDIAN)
            when (length) {
                1 -> put((value and 0xFF).toByte())
                2 -> putShort((value).toShort())
                else -> throw IOException("Cannot write $length bytes to the register. Max is 2 bytes")
            }
            rewind()
        }
        write(address, (block.rotateLeft(8) or 0x10), buffer)
    }

    fun readRegister(block: Short, address: Short, length: Int): Int {
        val buffer = ByteBuffer.allocateDirect(2)
        read(address, block, buffer)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return if (length == 2) {
            (buffer.short and 0xFFFF.toShort()).toInt()
        } else {
            buffer.get().toInt() and 0xff
        }
    }

    internal fun read(address: Short, index: Short, buffer: ByteBuffer) {
        val transfer = usbDevice.controlTransfer(CTRL_IN, address, index, buffer)
        if (transfer.status is ResultStatus.Error) {
            throw IOException("Reading from device failed: ${transfer.status::class.simpleName}")
        }
    }

    private fun write(value: Short, index: Short, buffer: ByteBuffer) {
        val transfer = usbDevice.controlTransfer(CTRL_OUT, value, index, buffer)
        if (transfer.status is ResultStatus.Error) {
            throw IOException("Writing to device failed: ${transfer.status::class.simpleName}")
        }
    }

    private fun initBaseband() {

        // initialize USB
        writeRegister(Blocks.USBB, UsbReg.USB_SYSCTL, 0x09, 1)
        writeRegister(Blocks.USBB, UsbReg.USB_EPA_MAXPKT, 0x0002, 2)
        writeRegister(Blocks.USBB, UsbReg.USB_EPA_CTL, 0x1002, 2)

        // Poweron demod
        writeRegister(Blocks.SYSB, Reg.DEMOD_CTL_1, 0x22, 1)
        writeRegister(Blocks.SYSB, Reg.DEMOD_CTL, 0xe8, 1)

        // reset demod
        writeDemodRegister(1, 0x01, 0x14, 1)
        writeDemodRegister(1, 0x01, 0x10, 1)

        // disable spectrum inversion and adjacent channel rejection
        writeDemodRegister(1, 0x15, 0x00, 1)
        writeDemodRegister(1, 0x16, 0x0000, 2)

        // clear DDC shift and IF registers
        for (i in 0..6)
            writeDemodRegister(1, (0x16 + i).toShort(), 0x00, 1)

        setFir()

        // enable SDR mode, disable DAGC (bit 5)
        writeDemodRegister(0, 0x19, 0x05, 1)

        // init FSM state-holding register
        writeDemodRegister(1, 0x93, 0xf0, 1)
        writeDemodRegister(1, 0x94, 0x0f, 1)

        // disable AGC(en_dagc, bit0)
        writeDemodRegister(1, 0x11, 0x00, 1)

        //disable RF/IF AGC loop
        writeDemodRegister(1, 0x04, 0x00, 1)

        // disable PID filter
        writeDemodRegister(0, 0x61, 0x60, 1)

        // opt_adc_iq = 0
        writeDemodRegister(0, 0x06, 0x80, 1)

        // enable 0-IF, DC cancellation, IQ estimation/compensation
        writeDemodRegister(1, 0xb1, 0x1b, 1)

        // disable 4.096 MHz clock output on pin TP_CK0
        writeDemodRegister(0, 0x0d, 0x83, 1)
    }

    fun setup() {
        repeat(2) {
            runCatching {
                writeRegister(
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
        enableI2CRepeater(true)

        init()
        enableI2CRepeater(false)

        println("Device successfully configured: $product by $manufacturer with serial no $serial")
    }

    fun setBiasTee(enabled: Boolean) {
        setgpioOutput(0)
        val intValue = if (enabled) 1 else 0
        setgpioBit(0, intValue)
        println("bias tee now is $enabled")
    }

    fun getTunerSampleRate(): Int {
        val high = readDemodRegister(0x1, 0x9F, 2)
        val low = readDemodRegister(0x1, 0xA1, 2)
        val ratio = Integer.rotateLeft(high, 16) or low
        val sampleRate = rtlXtal() * TWO_22_POW / ratio
        return sampleRate.roundToInt()
    }

    fun resetBuffer() {
        writeRegister(Blocks.USBB, UsbReg.USB_EPA_CTL, 0x1002, 2)
        writeRegister(Blocks.USBB, UsbReg.USB_EPA_CTL, 0x0000, 2)
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
        var result = readRegister(Blocks.SYSB, Reg.GPO, 1)
        result = if (value == 1) {
            (result or gpio)
        } else {
            result and (gpio2.inv())
        }
        writeRegister(Blocks.SYSB, Reg.GPO, result, 1)
    }

    private fun setgpioOutput(gpio: Int) {
        val gpio2 = 1 shl gpio
        val result = readRegister(Blocks.SYSB, Reg.GPD, 1)
        writeRegister(Blocks.SYSB, Reg.GPD, result and gpio2.inv(), 1)
        val result2 = readRegister(Blocks.SYSB, Reg.GPOE, 1)
        writeRegister(Blocks.SYSB, Reg.GPOE, result2 or gpio2, 1)
    }

    @Volatile private var i2cRepeaterEnabled = false

    fun enableI2CRepeater(enabled: Boolean) {
        if (i2cRepeaterEnabled == enabled) {
            return
        }
        val i = when (enabled) {
            true -> 0x18
            else -> 0x10
        }
        writeDemodRegister(1, 0x01, i, 1)
        i2cRepeaterEnabled = enabled
    }

    fun setSampleFrequencyCorrection(ppm: Int) {
        val offset = (ppm * -1) * 2.0.pow(24) / 1000000
        var tmp = offset.toInt() and 0xff
        var r = 0
        writeDemodRegister(1, 0x3f, tmp, 1)
        tmp = (offset.toInt() shr 8) and 0x3f
        writeDemodRegister(1, 0x3e, tmp, 1)
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
        writeDemodRegister(1, 0x9f, tmp, 2)
        tmp = rsampRatio and 0xffff
        writeDemodRegister(1, 0xa1, tmp, 2)
        setSampleFrequencyCorrection(ppmCorrection)
        writeDemodRegister(1, 0x01, 0x14, 1)
        writeDemodRegister(1, 0x01, 0x10, 1)
        this.rate = realRate.roundToInt()

    }

    fun setIFFreq(freq: Long) {
        val ifFreq = ((freq * TWO_22_POW) / getXtalFreq() * (-1)).toInt()

        // write byte 2
        var i = (ifFreq shr 16) and 0x3f
        writeDemodRegister(1, 0x19, i, 1)

        // write byte one
        i = (ifFreq shr 8) and 0xff
        writeDemodRegister(1, 0x1a, i, 1)

        // write byte 2
        i = ifFreq and 0xff
        writeDemodRegister(1, 0x1b, i, 1)
    }

    fun setCenterFreq(freq: Long) {
        if (directSampling) {
            setIFFreq(freq)
        }
        enableI2CRepeater(true)
        scope.launch {
            setFrequency(freq)
        }
        enableI2CRepeater(false)
    }

    fun getCenterFreq() = 0

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
            const val DEMODB: Short = 0
            const val USBB: Short = 1
            const val SYSB: Short = 2
            const val TUNB: Short = 3
            const val ROMB: Short = 4
            const val IRB: Short = 5
            const val IICB: Short = 6
        }

        object Reg {
            const val DEMOD_CTL: Short = 0x3000
            const val GPO: Short = 0x3001
            const val GPI: Short = 0x3002
            const val GPOE: Short = 0x3003
            const val GPD: Short = 0x3004
            const val SYSINTE: Short = 0x3005
            const val SYSINTS: Short = 0x3006
            const val GP_CFG0: Short = 0x3007
            const val GP_CFG1: Short = 0x3008
            const val SYSINTE_1: Short = 0x3009
            const val SYSINTS_1: Short = 0x300a
            const val DEMOD_CTL_1: Short = 0x300b
            const val IR_SUSPEND: Short = 0x300c
        }

        object UsbReg {
            const val USB_SYSCTL: Short = 0x2000
            const val USB_CTRL: Short = 0x2010
            const val USB_STAT: Short = 0x2014
            const val USB_EPA_CFG: Short = 0x2144
            const val USB_EPA_CTL: Short = 0x2148
            const val USB_EPA_MAXPKT: Short = 0x2158
            const val USB_EPA_MAXPKT_2: Short = 0x215a
            const val USB_EPA_FIFO_CFG: Short = 0x2160
        }

        private var fir = intArrayOf(
            -54, -36, -41, -40, -32, -14, 14, 53,    /* 8 bit signed */
            101, 156, 215, 273, 327, 372, 404, 421    /* 12 bit signed */
        )
        private val TWO_22_POW: Double = 2.0.pow(22)


        val byteArrayChannel = Channel<ByteArray>(0).cl

    }


    override fun close() {
        usbDevice.releaseUsbDevice()
        scope.cancel()
    }


}


