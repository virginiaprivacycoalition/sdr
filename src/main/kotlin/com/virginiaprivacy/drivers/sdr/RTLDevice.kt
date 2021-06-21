package com.virginiaprivacy.drivers.sdr

import com.virginiaprivacy.drivers.sdr.data.Status
import com.virginiaprivacy.drivers.sdr.r2xx.R82XX
import com.virginiaprivacy.drivers.sdr.usb.UsbIFace
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.Closeable
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.experimental.or
import kotlin.math.pow

@ExperimentalCoroutinesApi
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
open class RTLDevice internal constructor(private val usbDevice: UsbIFace) : Closeable {

    private val bufferSize = 12

    var devLost: Int = 0

    val product by usbDevice::productName

    val tunerChip: Tuner by lazy { Tuner.tunerType(this) }

    var tunableDevice: TunableDevice? = null

    val scope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    private fun ioStatus() = Status.getIOStatus().value

    val flow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 12)

    val requestFlow = channelFlow<ByteArray> {
        while (ioStatus() == IOStatus.ACTIVE) {
            for (i in 0.until(buffer.size)) {
                usbDevice.submitBulkTransfer(i)
                if (ioStatus() == IOStatus.ACTIVE) {
                    val transferResult = usbDevice.waitForTransferResult()
                    if (transferResult == null) {
                        // Read failed
                        Status.setIOStatus(IOStatus.EXIT)
                        return@channelFlow
                    }
                    val bytes = ByteArray(transferResult.position())
                    transferResult.rewind()
                    transferResult.get(bytes)
                    Status.bytesRead += bytes.size.toLong()
                    flow.emit(bytes)
                } else {
                    return@channelFlow
                }
            }
        }
    }

    private val buffer = List(bufferSize) {
        ByteArray(DEFAULT_BUF_LENGTH)
    }

    init {
        buffer.forEachIndexed { index, bytes ->
            usbDevice.prepareNewBulkTransfer(index, ByteBuffer.allocateDirect(bufferSize))
        }
    }

    fun setFrequency(freq: Int) {
        tunableDevice?.setFrequency(freq)
    }

    fun setGain(manualGain: Boolean, gain: Int? = null) {
        tunableDevice?.setGain(manualGain, gain)
    }

    fun writeRegMask(reg: Int, value: Int, bitMask: Int) {
        tunableDevice?.writeRegMask(reg, value, bitMask)
    }

    fun writeTunerReg(reg: Int, value: Int) {
        tunableDevice?.writeReg(reg, value)
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
        val data = UByteArray(2).toByteArray()
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
        return (data[1].toInt() shl 8) or data[0].toInt()
    }

    fun writeReg(block: Int, address: Int, value: Int, length: Int): Int {
        val data = UByteArray(2)

        val index = (block shl 8) or 0x10
        if (length == 1) {
            data[0] = (value and 0xff).toUByte()
        } else {
            data[0] = ((value.toUInt() shr 8).toUByte())
        }
        data[1] = (value.toUInt() and 255u).toUByte()

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
        val data = UByteArray(2)
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
        return (data[1].toInt() shl 8) or data[0].toInt()
    }

    fun demodWriteReg(page: Int, address: Int, value: Int, length: Int): Int {
        val data = UByteArray(2)

        val index = 16 or page

        val realAddress = (address shl 8) or 32

        if (length == 1) {
            data[0] = (value and 0xff).toUByte()
        } else {
            data[0] = (value shr 8).toUByte()
        }
        data[1] = (value and 0xff).toUByte()
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
        return if (result == length) {
            0
        } else {
            -1
        }
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
            val first = Companion.fir[8 + i].toInt()
            val second = Companion.fir[8 + i + 1].toInt()
            if (first !in -2048..2047 || second !in -2048..2047) {
                throw IllegalArgumentException()
            }
            fir[8 + i * 3 / 2] = (first shr 4).toUInt()
            fir[8 + i * 3 / 2 + 1] =
                ((first shl 4) or (((second shr 8) and 0x0f))).toUInt()
            fir[8 + i * 3 / 2 + 2] = first.toUInt()
        }

        fir.filterNot { it == 0u }.forEach { i ->
            if (i == null) {
                throw (RuntimeException("i is null?!? $i"))
            }
            if (i.toInt() < 0) {
                println(fir.asList())
            }
            demodWriteReg(1, 0x1c + i.toInt(), i.toInt(), 1)
        }
    }

    internal fun i2cReadReg(i2cAddress: Int, reg: Int): Int {
        val address = i2cAddress.toUShort()
        writeArray(Blocks.IICB, address, byteArrayOf(reg.toByte()), 1)
        return readByte(Blocks.IICB, address).toInt()
    }

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

    fun setBiasTee(enabled: Boolean) {
        setgpioOutput(0)
        val intValue = if (enabled) 1 else 0
        setgpioBit(0, intValue)
        println("bias tee now is $enabled")
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
        close()
    }

    val runningPlugins = mutableSetOf<Plugin>()

    fun runPlugin(plugin: Plugin) {
        plugin.setup()
        readAsync()
        plugin.run()
        runningPlugins.add(plugin)
    }

//    private suspend fun requestToBytes(
//        transferIndex: Int
//    )  {
//        if (transferIndex >= 0 && transferIndex <= bufferArray.size) {
//        val buf = bufferArray[transferIndex]
//            val bytes = ByteArray(buf.position())
//            buf.rewind()
//            buf.get(bytes)
//            buf.clear()
//            bytesRead.addAndGet(bytes.size.toLong())
//
//            usbDevice.submitBulkTransfer(transferIndex, buf)
//        } else {
//            throw IOException("Invalid transferIndex of $transferIndex")
//        }
//    }

    private fun readAsync() {
        Status.startTime = System.currentTimeMillis()
        Status.bytesRead = 0
        Status.setIOStatus(IOStatus.ACTIVE)
        scope.launch {
            for (i in 0.until(buffer.size)) {
                usbDevice.prepareNewBulkTransfer(
                    i, ByteBuffer.allocateDirect(
                        DEFAULT_BUFFER_SIZE
                    )
                )
            }
            delay(2000)

            while (ioStatus() == IOStatus.ACTIVE) {
                for (i in 0.until(buffer.size)) {
                    usbDevice.submitBulkTransfer(i)
                    if (ioStatus() == IOStatus.ACTIVE) {
                        val transferResult = usbDevice.waitForTransferResult()
                        if (transferResult == null) {
                            // Read failed
                            Status.setIOStatus(IOStatus.EXIT)
                            cancel()
                            return@launch
                        }
                        val bytes = ByteArray(transferResult.position())
                        transferResult.rewind()
                        transferResult.get(bytes)
                        Status.bytesRead += bytes.size.toLong()
                        flow.emit(bytes)
                    } else {
                        cancel()
                    }
                }
            }
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

    fun setSampleRate(rate: Int) {
        var r = 0
        var rsampRatio = 0
        var realRsampRatio = 0
        var realRate = 0.0
        if (rate !in 225001..3200000 && rate in 300001..900000) {
            val msg = "Invalid sample rate: ${rate}Hz"
            throw IllegalArgumentException(msg)
        }
        rsampRatio = ((tunableDevice!!.rtlXtal() * 2.0.pow(22) / rate).toInt())
        rsampRatio = rsampRatio and 0x0ffffffc
        realRsampRatio = rsampRatio or ((rsampRatio and 0x08000000) shl 1)
        realRate = (tunableDevice!!.rtlXtal() * 2.0.pow(22) / realRsampRatio)
        println("Exact sample rate set to ${realRate}Hz")
        //dev.rate = realRate.roundToInt()
    }

    fun setAGCMode(enabled: Boolean) {
        val i = if (enabled) 0x25 else 0x05
        demodWriteReg(0, 0x19, i, 1)
    }

    fun setIFFreq(dev: TunableDevice, freq: Int) {
        val ifFreq = ((freq * 2.0.pow(22)) / dev.getXtalFreq() * (-1)).toInt()
        var i = (ifFreq shr 16) and 0x3f
        demodWriteReg(1, 0x19, i, 1)
        i = (ifFreq shr 8) and 0xff
        demodWriteReg(1, 0x1a, i, 1)
        i = ifFreq and 0xff
        demodWriteReg(1, 0x1b, i, 1)
    }

    fun setCenterFreq(dev: TunableDevice, freq: Int) {

    }

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
            dev.setup()
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
        const val R82XX_IF_FREQ = 3570000
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
    }


    override fun close() {
        usbDevice.releaseUsbDevice()
        scope.cancel()
    }


}

