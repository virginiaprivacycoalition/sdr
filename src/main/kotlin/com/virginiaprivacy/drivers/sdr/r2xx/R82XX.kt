package com.virginiaprivacy.drivers.sdr.r2xx

import com.virginiaprivacy.drivers.sdr.*
import com.virginiaprivacy.drivers.sdr.RTLDevice.Companion.DEFAULT_RTL_XTAL_FREQ
import com.virginiaprivacy.drivers.sdr.RTLDevice.Companion.R828D_XTAL_FREQ
import com.virginiaprivacy.drivers.sdr.RTLDevice.Companion.R82XX_IF_FREQ
import com.virginiaprivacy.drivers.sdr.exceptions.PllNotLockedException
import com.virginiaprivacy.drivers.sdr.r2xx.R82XX.Reg.*
import com.virginiaprivacy.drivers.sdr.r2xx.R82xxChip.*
import com.virginiaprivacy.drivers.sdr.usb.UsbIFace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.IOException
import kotlin.experimental.and

@ExperimentalCoroutinesApi
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
class R82XX @ExperimentalStdlibApi constructor(
private val usbIFace: UsbIFace) : TunableDevice, I2C {



    override val dev = RTLDevice(usbIFace)
    override val config = R82xxConfig(Tuner.RTLSDR_TUNER_R820T)
    private var xtalCapSel = R82xxXtalCapValue.XTAL_HIGH_CAP_0P
    private var regs = IntArray(NUM_REGS)
    private val buf = ByteArray(NUM_REGS + 1)
    private var filCalCode = 0
    private var input = 0
    private var hasLock: Boolean = false
    private var initDone: Boolean = false
    private var tunedFrequency: Long = 0
    var r82xxTunerType: R82xxTunerType? = null
    override var ppmCorrection: Int = 0
    override val ifFrequency = R82XX_IF_FREQ
    override var rate: Int = 0
    override var directSampling: Boolean = false
    override var bandwidth: Long = 0
    private var delsys: R82xxDeliverySystem? = null

    private fun shadowStore(reg: Int, value: ByteArray, len: Int) {
        var r = reg - REG_SHADOW_START
        var len2 = len
        if (r < 0) {
            len2 += r
            r = 0
        }
        if (len2 > NUM_REGS - r) {
            len2 = NUM_REGS - r
        }
        for ((count, i) in (r until len2).withIndex()) {
            regs[i] = value[count].toInt()
        }
    }

    private fun r82xxWrite(reg: Int, value: ByteArray, len: Int) {
        shadowStore(reg, value, len)
        var len2 = len
        var pos = 0
        var reg2 = reg
        do {
            val size = if (len > config.maxI2cMsgLen - 1) {
                config.maxI2cMsgLen - 1
            } else {
                len
            }
            val buffer = ByteArray(size)
            buf[0] = reg2.toUByte().toByte()
            buffer[0] = buf[0]
            for ((index, i) in (1 until size).withIndex()) {
                buffer[i] = value[index].toUByte().toByte()
            }

            if (buffer.size >= config.maxI2cMsgLen) {
                throw IOException("Buffer of size ${buffer.size} is bigger than max i2c message length of ${config.maxI2cMsgLen}")
            }

            when (val r = i2cWriteFun(config.i2cAddr, buffer, buffer.size)) {
                (size) -> {

                }
                else -> {
                    error("result: $r | size: $size")
                    throw IOException()
                }
            }
            reg2 += size
            len2 -= size
            pos += size
        } while (len2 > 0)
    }

    override fun writeReg(reg: Int, value: Int) {
        r82xxWrite(reg, byteArrayOf(value.toByte()), 1)
    }

    private fun r82xxReadCacheReg(reg: Int): Int {
        val reg2 = reg - REG_SHADOW_START
        if (reg2 in 0 until NUM_REGS) {
            return regs[reg2]
        }
        return -1
    }

    override fun writeRegMask(reg: Int, value: Int, bitMask: Int) {
        val rc = r82xxReadCacheReg(reg)
        if (rc < 0) return
        val value2 = (rc and bitMask.inv()) or (value and bitMask)
        return r82xxWrite(reg, byteArrayOf(value2.toUByte().toByte()), 1)
    }

    override fun init(dev: RTLDevice) {
        xtalCapSel = R82xxXtalCapValue.XTAL_HIGH_CAP_0P
        r82xxWrite(0x05, initArray.map { it.toByte() }.toByteArray(), initArray.size)
        setTVStandard(3, R82xxTunerType.TUNER_DIGITAL_TV, 0)
        sysFreqSelect(0)
        initDone = true
    }

    private fun bitRev(byte: UByte): Int {
        return (lut[byte.toUInt().toInt() and 0xf].toUInt().toInt() shl 4) or lut[byte.toUInt()
            .toInt() shr 4].toUInt().toInt()
    }

    override fun read(reg: Int, len: Int): ByteArray {
        buf[0] = reg.toByte()
        val buffer = ByteArray(1) {
            buf[0]
        }
        val rc = i2cWriteFun(config.i2cAddr, buffer, 1)
        if (rc != 1) {
            throw IOException("result: $rc")
        }
        val result = i2cReadFun(config.i2cAddr, len)
        return result.asUByteArray().map { bitRev(it).toUByte() }.toUByteArray().toByteArray()
    }

    private fun setMux(freq: Long) {
        frequencyRanges.firstOrNull { (freq / 1000000) < it.freq }
        val range =
            frequencyRanges.firstOrNull { (freq / 1000000) < it.freq } ?: frequencyRanges.last()
        OPEN_D.write(range.openD)
        RFMUX.write(range.rfMuxPloy)
        writeReg(0x1b, range.tfC)
        XTAL_CAP.write(range.xtalCap0p or 0x00)
        MIXER_BUFFER_POWER.write(0x00)
        IF_FILTER_POWER.write(0x00)
    }

    fun standBy() {
        if (!initDone) return
        writeReg(0x06, 0xb1)
        writeReg(0x05, 0xa0)
        writeReg(0x07, 0x3a)
        writeReg(0x08, 0x40)
        writeReg(0x09, 0xc0)
        writeReg(0x0a, 0x36)
        writeReg(0x0c, 0x35)
        writeReg(0x0f, 0x68)
        writeReg(0x11, 0x03)
        writeReg(0x17, 0xf4)
        writeReg(0x19, 0x0c)
    }

    private fun setPll(freq: Long) {
        val vcoMin = 1770000
        val vcoMax = vcoMin * 2
        var nSdm = 2
        var sdm = 0
        var mixDiv = 2
        var divBuf: Int
        var divNum = 0
        var vcoPowerRef = 2
        val refDiv2 = 0

        val freqKhz = (freq + 500) / 1000
        val pllRef = config.xtal
        val refKhz = (config.xtal + 500) / 1000

        PLL_REFDIV.write(0x00)
        PLL_AUTOTUNE_CLOCKRATE.write(0x00)
        VCO_CURRENT.write(0x80)
        while (mixDiv <= 64) {
            if ((freqKhz * mixDiv >= vcoMin) && (freqKhz * mixDiv < vcoMax)) {
                divBuf = mixDiv
                while (divBuf > 2) {
                    divBuf = divBuf shr 1
                    divNum++
                }
                break
            }
            mixDiv = mixDiv shl 1
        }
        val data = read(0x00, 5)
        if (config.rafaelChip == CHIP_R828D) {
            vcoPowerRef = 1
        }
        val vcoFineTune = (data[4].toInt() and 0x30) shr 4
        if (vcoFineTune > vcoPowerRef) {
            divNum -= 1
        } else if (vcoFineTune < vcoPowerRef) {
            divNum += 1
        }
        writeRegMask(0x10, divNum shl 5, 0xe0)
        val vcoFreq = freq * mixDiv.toLong()
        val nint = vcoFreq / (2 * pllRef)
        val vcoFra = (vcoFreq - 2 * pllRef * nint) / 1000
        if (nint > (128 / vcoPowerRef - 1)) {
            val s = "No valid PLL values for $freq Hz"
            error(s)
            return
        }

        val ni = (nint - 13) / 4
        val si = nint - 4 * ni - 13
        writeReg(0x14, (ni + (si.toInt() shl 6)).toInt())
        val value = if (vcoFra != 0L) {
            0x08
        } else
            0x00
        writeRegMask(0x12, value, 0x08)

        while (vcoFra > 1) {
            if (vcoFra > (2 * refKhz / nSdm)) {
                sdm += 32768 / (nSdm / 2)
                vcoFra - +2 * refKhz / nSdm
                if (nSdm >= 0x8000)
                    break
            }
            nSdm = nSdm shl 1
        }
        writeReg(0x16, sdm shr 8)
        writeReg(0x15, sdm and 0xff)

        var r: ByteArray? = null
        repeat(2) {
            r = read(0x00, 3)
            if ((r!![2].toInt() and 64) == 0) {
                if (it > 0) {
                    writeRegMask(0x12, 0x60, 0xe0)
                }
            }
        }

        if (r!![2].and(64) != 0.toByte()) {
            error("PLL not locked: bytes: ${r!!.toList()}, bytes[2]: ${r!![2].toInt()} ")
            hasLock = false
            return
        }
        hasLock = true

        writeRegMask(0x1a, 0x08, 0x08)


    }

    @Throws(PllNotLockedException::class)
    private fun setTVStandard(bw: Int, r82xxTunerType: R82xxTunerType, delsys: Long) {
        val filtGain = 0x10
        val imgR = 0x00
        val filtQ = 0x10
        val hpCor = 0x6b
        val extEnable = 0x60
        val loopThrough = 0x01
        val ltAtt = 0x00
        val fltExtWidest = 0x00
        val polyfilCur = 0x60
        initArray.forEachIndexed { index, i ->
            regs[index] = i
        }

        writeRegMask(0x0c, 0x00, 0x0f)
        writeRegMask(0x13, VER_NUM, 0x3f)
        writeRegMask(0x1d, 0x00, 0x38)



        val needCalibration = true

        if (needCalibration) {
            repeat(2) { i ->
                writeRegMask(11, hpCor, 0x60)
                writeRegMask(15, 4, 0x04)
                writeRegMask(16, 0, 0x03)
                setPll((56000 * 1000).toLong())
                if (!hasLock) {
                    if (i == 2) {
                        throw PllNotLockedException()
                    } else {
                        println("Pll not locked after first attempt. Trying again. . .")
                    }
                }

                writeRegMask(11, 16, 0x10)
                writeRegMask(11, 0x00, 0x10)
                writeRegMask(15, 0x00, 0x04)
                val data = read(0x00, 5)
                this.filCalCode = (data[4] and 0x0f).toInt()
                if (this.filCalCode != 0x0f) {

                }
            }
            if (this.filCalCode == 0x0f)
                this.filCalCode = 0
        }
        writeRegMask(0x00, filtQ or this.filCalCode, 0x1f)
        writeRegMask(0x0b, hpCor, 0xef)
        writeRegMask(0x07, imgR, 0x80)
        writeRegMask(0x06, filtGain, 0x30)
        writeRegMask(0x1e, extEnable, 0x60)
        writeRegMask(0x05, loopThrough, 0x80)
        writeRegMask(0x1f, ltAtt, 0x80)
        writeRegMask(0x0f, fltExtWidest, 0x80)
        writeRegMask(0x19, polyfilCur, 0x60)
        this.bandwidth = bw.toLong()
        this.delsys = R82xxDeliverySystem.values()[delsys.toInt()]
        this.r82xxTunerType = r82xxTunerType
    }

    private fun sysFreqSelect(freq: Long) {
        val flags = delsys!!.getFlags(freq)
        flags.run {
            if (config.usePreDetect)
                writeRegMask(0x06, pre_dect, 0x40)
            writeRegMask(0x1d, lna_top, 0xc7)
            writeRegMask(0x1c, mixer_top, 0xf8)
            writeReg(0x0d, lna_vth_l)
            writeReg(0x0e, mixer_vth_l)
            this@R82XX.input = air_cable1_in
            writeRegMask(0x05, air_cable1_in, 0x60)
            writeRegMask(0x06, cable2_in, 0x08)
            writeRegMask(0x11, cp_cur, 0x38)
            writeRegMask(0x17, div_buf_cur, 0x30)
            writeRegMask(0x0a, filter_cur, 0x60)
        }

            writeRegMask(0x1d, 0, 0x38)
            writeRegMask(0x1c, 0, 0x04)
            writeRegMask(0x06, 0, 0x40)
            writeRegMask(0x1a, 0x30, 0x30)
            writeRegMask(0x1d, 0x18, 0x38)
            writeRegMask(0x1c, flags.mixer_top, 0x04)
            writeRegMask(0x1e, flags.lna_discharge, 0x1f)
            writeRegMask(0x1a, 0x20, 0x30)
    }

    override fun setFrequency(freq: Long) {
        val loFreq = freq + this.ifFrequency
        setMux(loFreq)
        setPll(loFreq)
        val airCable1In = if (freq > 345.mhz()) 0x00 else 0x60
        if (config.rafaelChip == CHIP_R828D && airCable1In != input) {
            input = airCable1In
            writeRegMask(0x05, airCable1In, 0x60)
        }
        this.tunedFrequency = freq
        println("Set frequency to $freq")
    }

    override fun getTunedFrequency(): Long {
        return this.tunedFrequency
    }

    override fun setGain(manualGain: Boolean, gain: Int?) {
        if (manualGain) {
            if (gain == null) {
                error("When enabling manual gain, a value must be supplied")
                return
            }
            writeRegMask(0x05, 0x10, 0x10)
            writeRegMask(0x07, 0, 0x10)

            val data = read(0x00, 4)

            writeRegMask(0x0c, 0x08, 0x9f)

            var totalGain = 0
            var mixIndex = 0
            var lnaIndex = 0
            fun checkGain(): Boolean = totalGain >= gain

            for (i in 0..15) {
                if (checkGain()) break
                totalGain += LNA_GAIN_STEPS[++lnaIndex]
                if (checkGain()) break
                totalGain += MIXER_GAIN_STEPS[++mixIndex]
            }
            writeRegMask(0x05, lnaIndex, 0x0f)
            writeRegMask(0x07, mixIndex, 0x10)
        } else {
            writeRegMask(0x05, 0, 0x10)
            writeRegMask(0x07, 0x10, 0x10)
            writeRegMask(0x0c, 0x0b, 0x9f)
            println("Automatic gain mode enabled.")
        }
    }

    private fun error(text: String) {
        println("ERROR: $text")
    }

    fun Reg.write(value: Int) {
        this@R82XX.writeRegMask(this.address, value, mask)
    }

    enum class Reg(val address: Int, val mask: Int) {

        /**
         * LNA gain mode switch
         * 0: auto 1: manual
         */
        LNA_GAIN(0x05, 0X1F),

        /**
         * Open drain
         * 0: High-Z 1: Low-Z
         */
        OPEN_D(0x17, 0x08),

        /**
         * Tracking filter switch
         * 00: Tracking filter on
         * 01: Bypass tracking filter
         */
        RFMUX(0x1A, 0xC3),

        /**
         * Internal xtal cap settings
         * 00: no cap, 01: 10pF
         * 10: 20pF, 11: 30pF
         */
        XTAL_CAP(0x10, 0x0B),

        /**
         * Mixer buffer power
         * 0: off 1: on
         */
        MIXER_BUFFER_POWER(0x08, 0x3F),

        /**
         * IF filter power
         * 0: filter on
         * 1: off
         */
        IF_FILTER_POWER(0x09, 0x3F),

        /**
         * PLL reference frequency divider
         * 0: fref=xtal_freq
         * 1: fref=xtal_freq / 2
         */
        PLL_REFDIV(0x10, 0x10),

        /**
         * PLL autotune clockrate
         * 00: 128 khz
         * 01: 32 khz
         * 10: 8khz
         */
        PLL_AUTOTUNE_CLOCKRATE(0x1A, 0x0C),

        /**
         * ?
         */
        VCO_CURRENT(0x12, 0xE0),


    }


    companion object {

        const val R2XX_IF_FREQ = 3570000
        const val R820T_I2C_ADDR = 0x34
        const val R828D_I2C_ADDR = 0x74

        const val REG_SHADOW_START = 5
        const val NUM_REGS = 30
        const val VER_NUM = 49
        const val FILT_HP_BW1 = 350000
        const val FILT_HP_BW2 = 380000

//        private const val DIVIDER_0 =

        val initArray = intArrayOf(
            0x83, 0x32, 0x75,            /* 05 to 07 */
            0xc0, 0x40, 0xd6, 0x6c,            /* 08 to 0b */
            0xf5, 0x63, 0x75, 0x68,            /* 0c to 0f */
            0x6c, 0x83, 0x80, 0x00,            /* 10 to 13 */
            0x0f, 0x00, 0xc0, 0x30,            /* 14 to 17 */
            0x48, 0xcc, 0x60, 0x00,            /* 18 to 1b */
            0x54, 0xae, 0x4a, 0xc0
        )

        val frequencyRanges: List<R82xxFrequencyRange> by lazy {
            listOf(
                R82xxFrequencyRange(0, 0x08, 0x02, 0xdf, 0x02, 0x01, 0x00),
                R82xxFrequencyRange(50, 0x08, 0x02, 0xbe, 0x02, 0x01, 0x00),
                R82xxFrequencyRange(55, 0x08, 0x02, 0x8b, 0x02, 0x01, 0x00),
                R82xxFrequencyRange(60, 0x08, 0x02, 0x7b, 0x02, 0x01, 0x00),
                R82xxFrequencyRange(65, 0x08, 0x02, 0x69, 0x02, 0x01, 0x00),
                R82xxFrequencyRange(70, 0x08, 0x02, 0x58, 0x02, 0x01, 0x00),
                R82xxFrequencyRange(75, 0x00, 0x02, 0x44, 0x02, 0x01, 0x00),
                R82xxFrequencyRange(80, 0x00, 0x02, 0x44, 0x02, 0x01, 0x00),
                R82xxFrequencyRange(90, 0x00, 0x02, 0x34, 0x01, 0x01, 0x00),
                R82xxFrequencyRange(100, 0x00, 0x02, 0x34, 0x01, 0x01, 0x00),
                R82xxFrequencyRange(110, 0x00, 0x02, 0x24, 0x01, 0x01, 0x00),
                R82xxFrequencyRange(120, 0x00, 0x02, 0x24, 0x01, 0x01, 0x00),
                R82xxFrequencyRange(140, 0x00, 0x02, 0x14, 0x01, 0x01, 0x00),
                R82xxFrequencyRange(180, 0x00, 0x02, 0x13, 0x00, 0x00, 0x00),
                R82xxFrequencyRange(220, 0x00, 0x02, 0x13, 0x00, 0x00, 0x00),
                R82xxFrequencyRange(250, 0x00, 0x02, 0x11, 0x00, 0x00, 0x00),
                R82xxFrequencyRange(280, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00),
                R82xxFrequencyRange(310, 0x00, 0x41, 0x00, 0x00, 0x00, 0x00),
                R82xxFrequencyRange(450, 0x00, 0x41, 0x00, 0x00, 0x00, 0x00),
                R82xxFrequencyRange(588, 0x00, 0x40, 0x00, 0x00, 0x00, 0x00),
                R82xxFrequencyRange(650, 0x00, 0x40, 0x00, 0x00, 0x00, 0x00)
            )
        }

        val LNA_GAIN_STEPS by lazy {
            arrayOf(
                0,
                9,
                13,
                40,
                38,
                13,
                31,
                22,
                26,
                31,
                26,
                14,
                19,
                5,
                35,
                13
            )
        }

        val MIXER_GAIN_STEPS = arrayOf(0, 5, 10, 10, 19, 9, 10, 25, 17, 10, 8, 16, 13, 6, 3, -8)

        val IF_LOWPASS_BANDWIDTH_TABLE = arrayOf(
            1700000,
            1600000,
            1550000,
            1450000,
            1200000,
            900000,
            700000,
            550000,
            450000,
            350000
        )
        private val lut = byteArrayOf(
            0x0, 0x8, 0x4, 0xc, 0x2, 0xa, 0x6, 0xe, 0x1,
            0x9, 0x5, 0xd, 0x3, 0xb, 0x7, 0xf
        )
    }
}