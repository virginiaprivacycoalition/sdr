package com.virginiaprivacy.drivers.sdr.r2xx

import com.virginiaprivacy.drivers.sdr.*
import com.virginiaprivacy.drivers.sdr.exceptions.PllNotLockedException
import com.virginiaprivacy.drivers.sdr.exceptions.TunerNotInitializedException
import com.virginiaprivacy.drivers.sdr.r2xx.R82xxChip.CHIP_R828D
import com.virginiaprivacy.drivers.sdr.usb.UsbIFace
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.experimental.and


class R82XX constructor(
    private val usbIFace: UsbIFace
) : TunableDevice, TunableGain, RTLDevice(usbIFace) {


    /**
     * The gain reported by the device.
     */
    override val actualGain: Int
        get() {
            if (!initDone) throwUninitializedError()
            return readGain()
        }

    /**
     * Use this to enable/disable the tuner's AGC
     */
    override var tunerAutoGainEnabled: Boolean
        get() {
            if (!initDone) throwUninitializedError()
            return autoGain
        }
        set(value) {
            if (!initDone) throwUninitializedError()
            if (value && !autoGain) {
                autoGain = value
            } else if (!value && autoGain) {
                autoGain = false
            }
        }

    private val lock = ReentrantLock()

    private var autoGain = false
        set(value) {
            lock.withLock {
                if (value) {
                    R82XXRegister.LNA_GAIN_AUTO.write(0)
                    R82XXRegister.MIXER_AUTO_GAIN_ENABLED.write(0x10)
                    println("Tuner automatic gain enabled.")
                } else {
                    R82XXRegister.LNA_GAIN_AUTO.write(0x10)
                    R82XXRegister.MIXER_AUTO_GAIN_ENABLED.write(0)
                    println("Tuner automatic gain disabled.")
                }
            }
            field = value
        }
    override val config = R82xxConfig(Tuner.RTLSDR_TUNER_R820T)
    private var xtalCapSel = R82xxXtalCapValue.XTAL_HIGH_CAP_0P
    private var regs = IntArray(NUM_REGS)
    private val buf = ByteArray(NUM_REGS + 1)
    private var filCalCode = 0
    private var input = 0
    private var hasLock: Boolean = false
    override var initDone: Boolean = false
    private var _currentFrequencyRange: R82xxFrequencyRange? = null
    private var currentFrequencyRange: R82xxFrequencyRange?
        get() = _currentFrequencyRange
        set(value) {
            value?.let {
                if (value == _currentFrequencyRange) return
                R82XXRegister.OPEN_D.write(it.openD)
                R82XXRegister.RFMUX.write(it.rfMuxPloy)
                R82XXRegister.TF_BAND.write(it.tfC)
                R82XXRegister.XTAL_CAP.write(it.xtalCap0p)
                R82XXRegister.MIXER_BUFFER_POWER.write(0x00)
                R82XXRegister.IF_FILTER_POWER.write(0x00)
            }
            _currentFrequencyRange = value
        }
    private var tunedFrequency: Long = 0
    var r82xxTunerType: R82xxTunerType? = null
    override var ppmCorrection: Int = 0
    override val ifFrequency = R82XX_IF_FREQ
    override var rate: Int = 0
    override var directSampling: Boolean = false
    override var bandwidth: Long = 0
    private var delsys: R82xxDeliverySystem? = null
    private var mLnaGain: LNA_GAIN = LNA_GAIN.AUTOMATIC
    private var mVgaGain: VGA_GAIN = VGA_GAIN._0
    private var mMixerGain: MIXER_GAIN = MIXER_GAIN.AUTOMATIC

    override var lnaGain: LNA_GAIN
        get() = mLnaGain
        set(value) {
            setI2cRepeater(1)
            lock.withLock {
                if (mLnaGain != value) {
                    mLnaGain = value
                    writeRegMask(0x05, value.value, 0x0f)
                }
            }
            setI2cRepeater(0)
        }

    override var vgaGain: VGA_GAIN
        get() = mVgaGain
        set(value) {
            setI2cRepeater(1)
            lock.withLock {
                if (mVgaGain != value) {
                    mVgaGain = value
                    R82XXRegister.VGA_GAIN.write(mVgaGain.value)
                }
            }
            setI2cRepeater(0)
        }
    override var mixerGain: MIXER_GAIN
        get() = mMixerGain
        set(value) {
            setI2cRepeater(1)
            lock.withLock {
                if (mMixerGain != value) {
                    mMixerGain = value
                    R82XXRegister.MIXER_GAIN.write(mMixerGain.value)
                }
            }
            setI2cRepeater(0)
        }

    private fun throwUninitializedError() {
        throw TunerNotInitializedException(this::class.qualifiedName + " has not been setup yet.")
    }

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

            i2cWrite(config.i2cAddr, buffer, buffer.size)


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

    override fun init() {

        // disable 0-if mode
        super.demodulatorWriteReg(1, 0xb1, 0x1a, 1)

        // enable in-phase ADC
        super.demodulatorWriteReg(0, 0x08, 0x4D, 1)

        super.setIFFreq(R82XX_IF_FREQ)

        // enable spectrum inversion
        super.demodulatorWriteReg(1, 0x15, 0x01, 1)


        xtalCapSel = R82xxXtalCapValue.XTAL_HIGH_CAP_0P
        r82xxWrite(0x05, initArray.map { it.toByte() }.toByteArray(), initArray.size)
        setTVStandard(3, R82xxTunerType.TUNER_DIGITAL_TV, 0)
        sysFreqSelect(0)
        initDone = true
    }

    private fun bitrev(byte: Byte): Byte =
        ((lut[(byte and 0xFF) and 0xf].shl(4)) or lut[(byte and 0xFF) shr 4].toInt()).toByte()

    private fun bitRev(byte: UByte): Int {
        return (lut[byte.toUInt().toInt() and 0xf].toUInt().toInt() shl 4) or lut[byte.toUInt()
            .toInt() shr 4].toUInt().toInt()
    }

    override fun read(reg: Int, len: Int): ByteArray {
        buf[0] = reg.toByte()
        val buffer = ByteArray(1) {
            buf[0]
        }

        i2cWrite(config.i2cAddr, buffer, 1)

        val result = i2cRead(config.i2cAddr, len)
        return result.map { it and 0XFF }.map { bitRev(it.toUByte()).toByte() }.toByteArray()
    }


    private fun setMux(freq: Long) {
        val mhz = (freq * 4295).shr(32)
        currentFrequencyRange = frequencyRanges[getFrequencyIndex(mhz)]
    }

    private fun readGain(): Int {
        val result = read(0x00, 4)
        return (result[3] and 0x0F shl 1) + ((result[3] and 0xf0) shr 4)
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

        R82XXRegister.PLL_REFDIV.write(0x00)
        R82XXRegister.PLL_AUTOTUNE_CLOCKRATE.write(0x00)
        R82XXRegister.VCO_CURRENT.write(0x80)
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
                    println("Fill code is ${this.filCalCode.toByte()} but should be 0x0f")
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
        lock.withLock {
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

            R82XXRegister.LNA_GAIN_AUTO.write(0x10)
            R82XXRegister.MIXER_AUTO_GAIN_ENABLED.write(0)
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
            R82XXRegister.LNA_GAIN_AUTO.write(0)
            R82XXRegister.MIXER_AUTO_GAIN_ENABLED.write(0x10)
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

    sealed interface Reg {
        val address: Int
        val mask: Int
    }

    enum class R82XXRegister(override val address: Int, override val mask: Int) : Reg {


        /**
         * LNA AGC control
         * 0: Enable LNA auto gain mode (automatic)
         * 0x10: Disable LNA auto gain mode (manual gain)
         */
        LNA_GAIN_AUTO(0x05, 0x10),

        /**
         * LNA gain power
         * Used in combination with [LNA_GAIN] to control the LNA Gain setting
         */
        LNA_GAIN_POWER(0x05, 0x1f),

        /**
         *
         */
        LNA_GAIN(0x05, 0x0f),

        MIXER_GAIN(0x07, 0x1F),

        /**
         * AGC for mixer
         * 0: mixer AGC disabled (manual mode)
         * 0x10: mixer AGC enabled (automatic gain mode_
         */
        MIXER_AUTO_GAIN_ENABLED(0x07, 0x10),

        /**
         *  Mixer gain level:
         *  [MIXER_GAIN]
         */
        MIXER_GAIN_SETTINGS(0x07, 0x0f),

        /**
         * Mixer buffer power
         * 0: off 1: on
         */
        MIXER_BUFFER_POWER(0x08, 0x3f),

        /**
         * Mixer gain power
         * Used in combination with [MIXER_GAIN] to control the mixer Gain setting
         */

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
        RFMUX(0x1a, 0xc3),

        /**
         * Internal xtal cap settings
         * 00: no cap, 01: 10pF
         * 10: 20pF, 11: 30pF
         */
        XTAL_CAP(0x10, 0x0b),

        /**
         * IF filter power
         * 0: filter on
         * 1: off
         */
        IF_FILTER_POWER(0x09, 0x3f),

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

        TF_BAND(0x01b, 0x00),

        /**
         * Sets the amount of VGA gain. Used in combination with [VGA_GAIN]
         */
        VGA_GAIN(0x0C, 0x1f)


    }

    companion object {

        //        private const val DIVIDER_0 =

        const val R2XX_IF_FREQ = 3570000
        const val R820T_I2C_ADDR = 0x34
        const val R828D_I2C_ADDR = 0x74
        const val REG_SHADOW_START = 5
        const val NUM_REGS = 30
        const val VER_NUM = 49
        const val FILT_HP_BW1 = 350000
        const val FILT_HP_BW2 = 380000
        val freqToIndex = arrayOf( /* 50 */ 1,/* 51 */
            1,/* 52 */
            1,/* 53 */
            1,/* 54 */
            1,
            /* 55 */
            2,/* 56 */
            2,/* 57 */
            2,/* 58 */
            2,/* 59 */
            2,
            /* 60 */
            3,/* 61 */
            3,/* 62 */
            3,/* 63 */
            3,/* 64 */
            3,
            /* 65 */
            4,/* 66 */
            4,/* 67 */
            4,/* 68 */
            4,/* 69 */
            4,
            /* 70 */
            5,/* 71 */
            5,/* 72 */
            5,/* 73 */
            5,/* 74 */
            5,
            /* 75 */
            6,/* 76 */
            6,/* 77 */
            6,/* 78 */
            6,/* 79 */
            6,
            /* 80 */
            7,/* 81 */
            7,/* 82 */
            7,/* 83 */
            7,/* 84 */
            7,/* 85 */
            7,/* 86 */
            7,/* 87 */
            7,/* 88 */
            7,/* 89 */
            7,
            /* 90 */
            8,/* 91 */
            8,/* 92 */
            8,/* 93 */
            8,/* 94 */
            8,/* 95 */
            8,/* 96 */
            8,/* 97 */
            8,/* 98 */
            8,/* 99 */
            8,
            /* 100 */
            9,/* 101 */
            9,/* 102 */
            9,/* 103 */
            9,/* 104 */
            9,/* 105 */
            9,/* 106 */
            9,/* 107 */
            9,/* 108 */
            9,/* 109 */
            9,
            /* 110 */
            10,/* 111 */
            10,/* 112 */
            10,/* 113 */
            10,/* 114 */
            10,/* 115 */
            10,/* 116 */
            10,/* 117 */
            10,/* 118 */
            10,/* 119 */
            10,
            /* 120 */
            11,/* 121 */
            11,/* 122 */
            11,/* 123 */
            11,/* 124 */
            11,/* 125 */
            11,/* 126 */
            11,/* 127 */
            11,/* 128 */
            11,/* 129 */
            11,
            /* 130 */
            11,/* 131 */
            11,/* 132 */
            11,/* 133 */
            11,/* 134 */
            11,/* 135 */
            11,/* 136 */
            11,/* 137 */
            11,/* 138 */
            11,/* 139 */
            11,
            /* 140 */
            12,/* 141 */
            12,/* 142 */
            12,/* 143 */
            12,/* 144 */
            12,/* 145 */
            12,/* 146 */
            12,/* 147 */
            12,/* 148 */
            12,/* 149 */
            12,
            /* 150 */
            12,/* 151 */
            12,/* 152 */
            12,/* 153 */
            12,/* 154 */
            12,/* 155 */
            12,/* 156 */
            12,/* 157 */
            12,/* 158 */
            12,/* 159 */
            12,
            /* 160 */
            12,/* 161 */
            12,/* 162 */
            12,/* 163 */
            12,/* 164 */
            12,/* 165 */
            12,/* 166 */
            12,/* 167 */
            12,/* 168 */
            12,/* 169 */
            12,
            /* 170 */
            12,/* 171 */
            12,/* 172 */
            12,/* 173 */
            12,/* 174 */
            12,/* 175 */
            12,/* 176 */
            12,/* 177 */
            12,/* 178 */
            12,/* 179 */
            12,
            /* 180 */
            13,/* 181 */
            13,/* 182 */
            13,/* 183 */
            13,/* 184 */
            13,/* 185 */
            13,/* 186 */
            13,/* 187 */
            13,/* 188 */
            13,/* 189 */
            13,
            /* 190 */
            13,/* 191 */
            13,/* 192 */
            13,/* 193 */
            13,/* 194 */
            13,/* 195 */
            13,/* 196 */
            13,/* 197 */
            13,/* 198 */
            13,/* 199 */
            13,
            /* 200 */
            13,/* 201 */
            13,/* 202 */
            13,/* 203 */
            13,/* 204 */
            13,/* 205 */
            13,/* 206 */
            13,/* 207 */
            13,/* 208 */
            13,/* 209 */
            13,
            /* 210 */
            13,/* 211 */
            13,/* 212 */
            13,/* 213 */
            13,/* 214 */
            13,/* 215 */
            13,/* 216 */
            13,/* 217 */
            13,/* 218 */
            13,/* 219 */
            13,
            /* 220 */
            14,/* 221 */
            14,/* 222 */
            14,/* 223 */
            14,/* 224 */
            14,/* 225 */
            14,/* 226 */
            14,/* 227 */
            14,/* 228 */
            14,/* 229 */
            14,
            /* 230 */
            14,/* 231 */
            14,/* 232 */
            14,/* 233 */
            14,/* 234 */
            14,/* 235 */
            14,/* 236 */
            14,/* 237 */
            14,/* 238 */
            14,/* 239 */
            14,
            /* 240 */
            14,/* 241 */
            14,/* 242 */
            14,/* 243 */
            14,/* 244 */
            14,/* 245 */
            14,/* 246 */
            14,/* 247 */
            14,/* 248 */
            14,/* 249 */
            14,
            /* 250 */
            15,/* 251 */
            15,/* 252 */
            15,/* 253 */
            15,/* 254 */
            15,/* 255 */
            15,/* 256 */
            15,/* 257 */
            15,/* 258 */
            15,/* 259 */
            15,
            /* 260 */
            15,/* 261 */
            15,/* 262 */
            15,/* 263 */
            15,/* 264 */
            15,/* 265 */
            15,/* 266 */
            15,/* 267 */
            15,/* 268 */
            15,/* 269 */
            15,
            /* 270 */
            15,/* 271 */
            15,/* 272 */
            15,/* 273 */
            15,/* 274 */
            15,/* 275 */
            15,/* 276 */
            15,/* 277 */
            15,/* 278 */
            15,/* 279 */
            15,
            /* 280 */
            16,/* 281 */
            16,/* 282 */
            16,/* 283 */
            16,/* 284 */
            16,/* 285 */
            16,/* 286 */
            16,/* 287 */
            16,/* 288 */
            16,/* 289 */
            16,
            /* 290 */
            16,/* 291 */
            16,/* 292 */
            16,/* 293 */
            16,/* 294 */
            16,/* 295 */
            16,/* 296 */
            16,/* 297 */
            16,/* 298 */
            16,/* 299 */
            16,
            /* 300 */
            16,/* 301 */
            16,/* 302 */
            16,/* 303 */
            16,/* 304 */
            16,/* 305 */
            16,/* 306 */
            16,/* 307 */
            16,/* 308 */
            16,/* 309 */
            16,
            /* 310 */
            17,/* 311 */
            17,/* 312 */
            17,/* 313 */
            17,/* 314 */
            17,/* 315 */
            17,/* 316 */
            17,/* 317 */
            17,/* 318 */
            17,/* 319 */
            17,
            /* 320 */
            17,/* 321 */
            17,/* 322 */
            17,/* 323 */
            17,/* 324 */
            17,/* 325 */
            17,/* 326 */
            17,/* 327 */
            17,/* 328 */
            17,/* 329 */
            17,
            /* 330 */
            17,/* 331 */
            17,/* 332 */
            17,/* 333 */
            17,/* 334 */
            17,/* 335 */
            17,/* 336 */
            17,/* 337 */
            17,/* 338 */
            17,/* 339 */
            17,
            /* 340 */
            17,/* 341 */
            17,/* 342 */
            17,/* 343 */
            17,/* 344 */
            17,/* 345 */
            17,/* 346 */
            17,/* 347 */
            17,/* 348 */
            17,/* 349 */
            17,
            /* 350 */
            17,/* 351 */
            17,/* 352 */
            17,/* 353 */
            17,/* 354 */
            17,/* 355 */
            17,/* 356 */
            17,/* 357 */
            17,/* 358 */
            17,/* 359 */
            17,
            /* 360 */
            17,/* 361 */
            17,/* 362 */
            17,/* 363 */
            17,/* 364 */
            17,/* 365 */
            17,/* 366 */
            17,/* 367 */
            17,/* 368 */
            17,/* 369 */
            17,
            /* 370 */
            17,/* 371 */
            17,/* 372 */
            17,/* 373 */
            17,/* 374 */
            17,/* 375 */
            17,/* 376 */
            17,/* 377 */
            17,/* 378 */
            17,/* 379 */
            17,
            /* 380 */
            17,/* 381 */
            17,/* 382 */
            17,/* 383 */
            17,/* 384 */
            17,/* 385 */
            17,/* 386 */
            17,/* 387 */
            17,/* 388 */
            17,/* 389 */
            17,
            /* 390 */
            17,/* 391 */
            17,/* 392 */
            17,/* 393 */
            17,/* 394 */
            17,/* 395 */
            17,/* 396 */
            17,/* 397 */
            17,/* 398 */
            17,/* 399 */
            17,
            /* 400 */
            17,/* 401 */
            17,/* 402 */
            17,/* 403 */
            17,/* 404 */
            17,/* 405 */
            17,/* 406 */
            17,/* 407 */
            17,/* 408 */
            17,/* 409 */
            17,
            /* 410 */
            17,/* 411 */
            17,/* 412 */
            17,/* 413 */
            17,/* 414 */
            17,/* 415 */
            17,/* 416 */
            17,/* 417 */
            17,/* 418 */
            17,/* 419 */
            17,
            /* 420 */
            17,/* 421 */
            17,/* 422 */
            17,/* 423 */
            17,/* 424 */
            17,/* 425 */
            17,/* 426 */
            17,/* 427 */
            17,/* 428 */
            17,/* 429 */
            17,
            /* 430 */
            17,/* 431 */
            17,/* 432 */
            17,/* 433 */
            17,/* 434 */
            17,/* 435 */
            17,/* 436 */
            17,/* 437 */
            17,/* 438 */
            17,/* 439 */
            17,
            /* 440 */
            17,/* 441 */
            17,/* 442 */
            17,/* 443 */
            17,/* 444 */
            17,/* 445 */
            17,/* 446 */
            17,/* 447 */
            17,/* 448 */
            17,/* 449 */
            17,
            /* 450 */
            18,/* 451 */
            18,/* 452 */
            18,/* 453 */
            18,/* 454 */
            18,/* 455 */
            18,/* 456 */
            18,/* 457 */
            18,/* 458 */
            18,/* 459 */
            18,
            /* 460 */
            18,/* 461 */
            18,/* 462 */
            18,/* 463 */
            18,/* 464 */
            18,/* 465 */
            18,/* 466 */
            18,/* 467 */
            18,/* 468 */
            18,/* 469 */
            18,
            /* 470 */
            18,/* 471 */
            18,/* 472 */
            18,/* 473 */
            18,/* 474 */
            18,/* 475 */
            18,/* 476 */
            18,/* 477 */
            18,/* 478 */
            18,/* 479 */
            18,
            /* 480 */
            18,/* 481 */
            18,/* 482 */
            18,/* 483 */
            18,/* 484 */
            18,/* 485 */
            18,/* 486 */
            18,/* 487 */
            18,/* 488 */
            18,/* 489 */
            18,
            /* 490 */
            18,/* 491 */
            18,/* 492 */
            18,/* 493 */
            18,/* 494 */
            18,/* 495 */
            18,/* 496 */
            18,/* 497 */
            18,/* 498 */
            18,/* 499 */
            18,
            /* 500 */
            18,/* 501 */
            18,/* 502 */
            18,/* 503 */
            18,/* 504 */
            18,/* 505 */
            18,/* 506 */
            18,/* 507 */
            18,/* 508 */
            18,/* 509 */
            18,
            /* 510 */
            18,/* 511 */
            18,/* 512 */
            18,/* 513 */
            18,/* 514 */
            18,/* 515 */
            18,/* 516 */
            18,/* 517 */
            18,/* 518 */
            18,/* 519 */
            18,
            /* 520 */
            18,/* 521 */
            18,/* 522 */
            18,/* 523 */
            18,/* 524 */
            18,/* 525 */
            18,/* 526 */
            18,/* 527 */
            18,/* 528 */
            18,/* 529 */
            18,
            /* 530 */
            18,/* 531 */
            18,/* 532 */
            18,/* 533 */
            18,/* 534 */
            18,/* 535 */
            18,/* 536 */
            18,/* 537 */
            18,/* 538 */
            18,/* 539 */
            18,
            /* 540 */
            18,/* 541 */
            18,/* 542 */
            18,/* 543 */
            18,/* 544 */
            18,/* 545 */
            18,/* 546 */
            18,/* 547 */
            18,/* 548 */
            18,/* 549 */
            18,
            /* 550 */
            18,/* 551 */
            18,/* 552 */
            18,/* 553 */
            18,/* 554 */
            18,/* 555 */
            18,/* 556 */
            18,/* 557 */
            18,/* 558 */
            18,/* 559 */
            18,
            /* 560 */
            18,/* 561 */
            18,/* 562 */
            18,/* 563 */
            18,/* 564 */
            18,/* 565 */
            18,/* 566 */
            18,/* 567 */
            18,/* 568 */
            18,/* 569 */
            18,
            /* 570 */
            18,/* 571 */
            18,/* 572 */
            18,/* 573 */
            18,/* 574 */
            18,/* 575 */
            18,/* 576 */
            18,/* 577 */
            18,/* 578 */
            18,/* 579 */
            18,
            /* 580 */
            18,/* 581 */
            18,/* 582 */
            18,/* 583 */
            18,/* 584 */
            18,/* 585 */
            18,/* 586 */
            18,/* 587 */
            18,
            /* 588 */
            19,/* 589 */
            19,/* 590 */
            19,/* 591 */
            19,/* 592 */
            19,/* 593 */
            19,/* 594 */
            19,/* 595 */
            19,/* 596 */
            19,/* 597 */
            19,
            /* 598 */
            19,/* 599 */
            19,/* 600 */
            19,/* 601 */
            19,/* 602 */
            19,/* 603 */
            19,/* 604 */
            19,/* 605 */
            19,/* 606 */
            19,/* 607 */
            19,
            /* 608 */
            19,/* 609 */
            19,/* 610 */
            19,/* 611 */
            19,/* 612 */
            19,/* 613 */
            19,/* 614 */
            19,/* 615 */
            19,/* 616 */
            19,/* 617 */
            19,
            /* 618 */
            19,/* 619 */
            19,/* 620 */
            19,/* 621 */
            19,/* 622 */
            19,/* 623 */
            19,/* 624 */
            19,/* 625 */
            19,/* 626 */
            19,/* 627 */
            19,
            /* 628 */
            19,/* 629 */
            19,/* 630 */
            19,/* 631 */
            19,/* 632 */
            19,/* 633 */
            19,/* 634 */
            19,/* 635 */
            19,/* 636 */
            19,/* 637 */
            19,
            /* 638 */
            19,/* 639 */
            19,/* 640 */
            19,/* 641 */
            19,/* 642 */
            19,/* 643 */
            19,/* 644 */
            19,/* 645 */
            19,/* 646 */
            19,/* 647 */
            19,
            /* 648 */
            19,/* 649 */
            19
        )
        val initArray = intArrayOf(
            0x80,    /* Reg 0x05 */
            0x13,    /* Reg 0x06 */
            0x70,    /* Reg 0x07 */

            0xc0,    /* Reg 0x08 */
            0x40,    /* Reg 0x09 */
            0xdb,    /* Reg 0x0a */
            0x6b,    /* Reg 0x0b */

            /* Reg 0x0c:
             * for manual gain was: set fixed VGA gain for now (16.3 dB): 0x08
             * with active agc was: set fixed VGA gain for now (26.5 dB): 0x0b */
            (0xe0 or 11), /* Reg 0x0c */
            0x53,    /* Reg 0x0d */
            0x75,    /* Reg 0x0e */
            0x68,    /* Reg 0x0f */

            0x6c,    /* Reg 0x10 */
            0xbb,    /* Reg 0x11 */
            0x80,    /* Reg 0x12 */
            0x00,    /* Reg 0x13 */

            0x0f,    /* Reg 0x14 */
            0x00,    /* Reg 0x15 */
            0xc0,    /* Reg 0x16 */
            0x30,    /* Reg 0x17 */

            0x48,    /* Reg 0x18 */
            0xec,    /* Reg 0x19 */
            0x60,    /* Reg 0x1a */
            0x00,    /* Reg 0x1b */

            0x24,    /* Reg 0x1c */
            0xdd,    /* Reg 0x1d */
            0x0e,    /* Reg 0x1e */
            0x40    /* Reg 0x1f */
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

        fun getFrequencyIndex(frequencyMhz: Long): Int {
            if (frequencyMhz < 50) {
                return 0
            } else {
                with((frequencyMhz - 50).toInt()) {
                    return if (this < freqToIndex.size) {
                        freqToIndex[this]
                    } else {
                        20
                    }
                }
            }
        }
    }
}