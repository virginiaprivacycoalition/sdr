package com.virginiaprivacy.sdr.tuner.r820t

import com.virginiaprivacy.sdr.exceptions.DeviceException
import com.virginiaprivacy.sdr.exceptions.UsbException
import com.virginiaprivacy.sdr.sample.SampleMode
import com.virginiaprivacy.sdr.sample.SampleRate
import com.virginiaprivacy.sdr.tuner.Block
import com.virginiaprivacy.sdr.tuner.Page
import com.virginiaprivacy.sdr.tuner.RTL2832TunerController
import com.virginiaprivacy.sdr.tuner.TunerType
import com.virginiaprivacy.sdr.FrequencyDivider
import com.virginiaprivacy.sdr.FrequencyRange
import com.virginiaprivacy.sdr.usb.UsbController
import java.nio.ByteBuffer

class R820TTunerController(
    usbController: UsbController,
    override val tunerType: TunerType = TunerType.RAFAELMICRO_R820T
) :
    RTL2832TunerController(usbController) {
    private val mI2CAddress: Byte = 52
    private val mShadowRegister = intArrayOf(
        0,
        0,
        0,
        0,
        0,
        131,
        50,
        117,
        192,
        64,
        214,
        108,
        245,
        99,
        117,
        104,
        108,
        131,
        128,
        0,
        15,
        0,
        192,
        48,
        72,
        204,
        96,
        0,
        84,
        174,
        74,
        192
    )


    @kotlin.Throws(UsbException::class)
    override fun setSampleRateFilters(bandwidth: Int) {
    }

    @get:kotlin.Throws(DeviceException::class)
    @set:kotlin.Throws(DeviceException::class)
    private var _tunedFrequency: Long = 0L
    override var tunedFrequency: Long
        get() = _tunedFrequency
        set(frequency) {
            try {
                enableI2CRepeater(usbController, true)
                val controlI2C = false
                val offsetFrequency = frequency + 3570000L
                setMux(offsetFrequency, controlI2C)
                setPLL(offsetFrequency, controlI2C)
                enableI2CRepeater(usbController, false)
                _tunedFrequency = frequency
            } catch (var6: UsbException) {
                throw DeviceException("R820TTunerController - exception while setting frequency [" + frequency + "] - " + var6.localizedMessage)
            }
        }

    @kotlin.Throws(UsbException::class)
    override fun setSamplingMode(mode: SampleMode) {
        when (mode.ordinal) {
            1 -> {
                setIFFrequency(3570000)
                writeDemodRegister(usbController, Page.ONE, 21.toShort(), 1, 1)
                writeDemodRegister(usbController, Page.ZERO, 6.toShort(), 128, 1)
            }
            else -> {}
        }
    }

    @kotlin.Throws(UsbException::class)
    private fun setMux(frequency: Long, controlI2C: Boolean) {
        val range = FrequencyRange.getRangeForFrequency(frequency)
        writeR820TRegister(R820TRegister.DRAIN, range.mOpenDrain.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.RF_POLY_MUX, range.rFMuxPolyMux, controlI2C)
        writeR820TRegister(R820TRegister.TF_BAND, range.tFC, controlI2C)
        writeR820TRegister(R820TRegister.PLL_XTAL_CAPACITOR_AND_DRIVE, range.xTALHighCap0P, controlI2C)
        writeR820TRegister(R820TRegister.UNKNOWN_REGISTER_8, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.UNKNOWN_REGISTER_9, 0.toByte(), controlI2C)
    }

    @kotlin.Throws(DeviceException::class)
    override fun init(sampleRate: SampleRate) {
        super.init(sampleRate)
        try {
            initBaseband(usbController)
            enableI2CRepeater(usbController, true)
            val i2CRepeaterControl = false
            initTuner(i2CRepeaterControl)
            enableI2CRepeater(usbController, false)
        } catch (var3: UsbException) {
            throw DeviceException("error during init():$var3")
        }
    }

    @kotlin.Throws(UsbException::class)
    override fun initTuner(controlI2C: Boolean) {
        writeDemodRegister(usbController, Page.ONE, 177.toShort(), 26, 1)
        writeDemodRegister(usbController, Page.ZERO, 8.toShort(), 77, 1)
        setIFFrequency(3570000)
        writeDemodRegister(usbController, Page.ONE, 21.toShort(), 1, 1)
        initializeRegisters(controlI2C)
        setTVStandard(controlI2C)
        systemFrequencySelect(0L, controlI2C)
    }

    @kotlin.Throws(UsbException::class)
    private fun setTVStandard(controlI2C: Boolean) {
        writeR820TRegister(R820TRegister.XTAL_CHECK, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.VERSION, 49.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.LNA_TOP, 0.toByte(), controlI2C)
        var calibrationCode = 0
        for (x in 0..1) {
            writeR820TRegister(R820TRegister.FILTER_CAPACITOR, 107.toByte(), controlI2C)
            writeR820TRegister(R820TRegister.CALIBRATION_CLOCK, 4.toByte(), controlI2C)
            writeR820TRegister(R820TRegister.PLL_XTAL_CAPACITOR, 0.toByte(), controlI2C)
            setPLL(56000000L, controlI2C)
            writeR820TRegister(R820TRegister.CALIBRATION_TRIGGER, 16.toByte(), controlI2C)
            writeR820TRegister(R820TRegister.CALIBRATION_TRIGGER, 0.toByte(), controlI2C)
            writeR820TRegister(R820TRegister.CALIBRATION_CLOCK, 0.toByte(), controlI2C)
            calibrationCode = getCalibrationCode(controlI2C)
            if (!calibrationSuccessful(calibrationCode)) {
                println("Calibration NOT successful - code: $calibrationCode")
            }
        }
        if (calibrationCode == 15) {
            calibrationCode = 0
        }
        val filtQ: Byte = 16
        writeR820TRegister(R820TRegister.FILTER_CALIBRATION_CODE, (calibrationCode or filtQ.toInt()).toByte(), controlI2C)
        writeR820TRegister(R820TRegister.BANDWIDTH_FILTER_GAIN_HIGHPASS_FILTER_CORNER, 107.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.IMAGE_REVERSE, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.FILTER_GAIN, 16.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.CHANNEL_FILTER_EXTENSION, 96.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.LOOP_THROUGH, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.LOOP_THROUGH_ATTENUATION, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.FILTER_EXTENSION_WIDEST, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.RF_POLY_FILTER_CURRENT, 96.toByte(), controlI2C)
    }

    private fun calibrationSuccessful(calibrationCode: Int): Boolean {
        return calibrationCode != 0 && calibrationCode != 15
    }

    @kotlin.Throws(UsbException::class)
    private fun getCalibrationCode(controlI2C: Boolean): Int {
        return getStatusRegister(4, controlI2C) and 15
    }

    @kotlin.Throws(UsbException::class)
    private fun systemFrequencySelect(frequency: Long, controlI2C: Boolean) {
        writeR820TRegister(R820TRegister.LNA_TOP2, ((-27).toByte()), controlI2C)
        val mixerTop: Byte
        val cpCur: Byte
        val divBufCur: Byte
        if (frequency != 506000000L && frequency != 666000000L && frequency != 818000000L) {
            mixerTop = 36
            cpCur = 56
            divBufCur = 48
        } else {
            mixerTop = 20
            cpCur = 40
            divBufCur = 32
        }
        writeR820TRegister(R820TRegister.MIXER_TOP, mixerTop, controlI2C)
        writeR820TRegister(R820TRegister.LNA_VTH_L, 83.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.MIXER_VTH_L, 117.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.AIR_CABLE1_INPUT_SELECTOR, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.CABLE2_INPUT_SELECTOR, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.CP_CUR, cpCur, controlI2C)
        writeR820TRegister(R820TRegister.DIVIDER_BUFFER_CURRENT, divBufCur, controlI2C)
        writeR820TRegister(R820TRegister.FILTER_CURRENT, 64.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.LNA_TOP, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.MIXER_TOP2, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.PRE_DETECT, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.AGC_CLOCK, 48.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.LNA_TOP, 24.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.MIXER_TOP2, mixerTop, controlI2C)
        writeR820TRegister(R820TRegister.LNA_DISCHARGE_CURRENT, 20.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.AGC_CLOCK, 32.toByte(), controlI2C)
    }

    @kotlin.Throws(UsbException::class)
    private fun setPLL(frequency: Long, controlI2C: Boolean) {
        writeR820TRegister(R820TRegister.REFERENCE_DIVIDER_2, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.PLL_AUTOTUNE, 0.toByte(), controlI2C)
        writeR820TRegister(R820TRegister.VCO_CURRENT, ((-128).toByte()), controlI2C)
        val divider = FrequencyDivider.fromFrequency(frequency)
        val statusRegister4 = getStatusRegister(4, controlI2C)
        val vcoFineTune = statusRegister4 and 48 shr 4
        val divNum = divider.getDividerNumber(vcoFineTune)
        writeR820TRegister(R820TRegister.DIVIDER, (divNum shl 5).toByte(), controlI2C)
        val integral = divider.getIntegral(frequency)
        writeR820TRegister(R820TRegister.PLL, integral.registerValue, controlI2C)
        val sdm = divider.getSDM(integral, frequency)
        if (sdm != 0) {
            writeR820TRegister(R820TRegister.SIGMA_DELTA_MODULATOR_POWER, 0.toByte(), controlI2C)
            writeR820TRegister(R820TRegister.SIGMA_DELTA_MODULATOR_MSB, (sdm shr 8 and 255).toByte(), controlI2C)
            writeR820TRegister(R820TRegister.SIGMA_DELTA_MODULATOR_LSB, (sdm and 255).toByte(), controlI2C)
        } else {
            writeR820TRegister(R820TRegister.SIGMA_DELTA_MODULATOR_POWER, 8.toByte(), controlI2C)
        }
        if (!isPLLLocked(controlI2C)) {
            writeR820TRegister(R820TRegister.VCO_CURRENT, 96.toByte(), controlI2C)
            if (!isPLLLocked(controlI2C)) {
                throw UsbException("R820T Tuner Controller - couldn't achieve PLL lock on frequency [$frequency]")
            }
        }
        writeR820TRegister(R820TRegister.PLL_AUTOTUNE_VARIANT, 8.toByte(), controlI2C)
    }

    @kotlin.Throws(UsbException::class)
    private fun isPLLLocked(controlI2C: Boolean): Boolean {
        val register = getStatusRegister(2, controlI2C)
        return register and 64 == 64
    }

    @kotlin.Throws(UsbException::class)
    private fun initializeRegisters(controlI2C: Boolean) {
        for (x in 5 until mShadowRegister.size) {
            writeI2CRegister(mI2CAddress, x.toByte(), mShadowRegister[x].toByte(), controlI2C)
        }
    }

    @kotlin.Throws(UsbException::class)
    private fun getStatusRegister(register: Int, controlI2C: Boolean): Int {
        val buffer = ByteBuffer.allocateDirect(5)
        read(usbController, mI2CAddress.toShort(), Block.I2C, buffer)
        return bitReverse(buffer[register].toInt() and 255)
    }

    @kotlin.Throws(UsbException::class)
    fun writeR820TRegister(register: R820TRegister, value: Byte, controlI2C: Boolean) {
        var value = value
        if (register.isMasked) {
            val current = mShadowRegister[register.register]
            value = (current and register.mMask.inv() or (value.toInt() and register.mMask)).toByte()
        }
        writeI2CRegister(mI2CAddress, register.register.toByte(), value, controlI2C)
        mShadowRegister[register.register] = value.toInt()
    }

    @kotlin.Throws(UsbException::class)
    fun readR820TRegister(register: R820TRegister, controlI2C: Boolean): Int {
        return readI2CRegister(usbController, mI2CAddress, register.register.toByte(), controlI2C)
    }

    @kotlin.Throws(UsbException::class)
    fun setGain(gain: R820TGain, controlI2C: Boolean) {
        setLNAGain(gain.lNAGain, controlI2C)
        setMixerGain(gain.mixerGain, controlI2C)
        setVGAGain(gain.vGAGain, controlI2C)
    }

    @kotlin.Throws(UsbException::class)
    fun setLNAGain(gain: R820TLNAGain, controlI2C: Boolean) {
        writeR820TRegister(R820TRegister.LNA_GAIN, gain.mSetting.toByte(), controlI2C)
    }

    @kotlin.Throws(UsbException::class)
    fun setMixerGain(gain: R820TMixerGain, controlI2C: Boolean) {
        writeR820TRegister(R820TRegister.MIXER_GAIN, gain.mSetting.toByte(), controlI2C)
    }

    @kotlin.Throws(UsbException::class)
    fun setVGAGain(gain: R820TVGAGain, controlI2C: Boolean) {
        writeR820TRegister(R820TRegister.VGA_GAIN, gain.mSetting.toByte(), controlI2C)
    }

    companion object {
        const val MIN_FREQUENCY = 31800L
        const val MAX_FREQUENCY = 1782030L
        const val USABLE_BANDWIDTH_PERCENT = 1.0
        const val DC_SPIKE_AVOID_BUFFER = 5000
        const val R820T_IF_FREQUENCY = 3570000
        const val VERSION: Byte = 49
        private val BIT_REV_LOOKUP_TABLE = byteArrayOf(0, 8, 4, 12, 2, 10, 6, 14, 1, 9, 5, 13, 3, 11, 7, 15)
        private fun bitReverse(value: Int): Int {
            return BIT_REV_LOOKUP_TABLE[value and 15].toInt() shl 4 or BIT_REV_LOOKUP_TABLE[value and 240 shr 4].toInt()
        }
    }
}