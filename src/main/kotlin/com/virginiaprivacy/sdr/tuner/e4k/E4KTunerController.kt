package com.virginiaprivacy.sdr.tuner.e4k

import com.virginiaprivacy.sdr.exceptions.DeviceException
import com.virginiaprivacy.sdr.exceptions.UsbException
import com.virginiaprivacy.sdr.sample.SampleRate
import com.virginiaprivacy.sdr.tuner.Address
import com.virginiaprivacy.sdr.tuner.Block
import com.virginiaprivacy.sdr.tuner.RTL2832TunerController
import com.virginiaprivacy.sdr.tuner.TunerType
import com.virginiaprivacy.sdr.usb.Descriptor
import com.virginiaprivacy.sdr.usb.UsbController
import kotlin.experimental.inv

class E4KTunerController(override val usbController: UsbController, override val tunerType: TunerType = TunerType.ELONICS_E4000) :
    RTL2832TunerController(usbController) {


    @kotlin.Throws(DeviceException::class)
    override fun init(sampleRate: SampleRate) {
        try {
            usbController.open()
        } catch (e: UsbException) {
            throw DeviceException("Error: Couldn't open RTL2832 usb device [${usbController.getErrorMessage(e.errorCode ?: 0)}]")
        }
        usbController.claimInterface(0)
        val eeprom: ByteArray = try {
            readEEPROM(0.toShort(), 256)
        } catch (var7: Exception) {
            throw DeviceException("error while reading the EEPROM device descriptor " + var7.message)
        }
        try {
            mDescriptor = Descriptor(eeprom)
        } catch (var8: Exception) {
            println(
                "error while constructing device descriptor using descriptor byte array " + (eeprom.contentToString() + ": ") + var8
            )
        }
        try {
            writeRegister(usbController, Block.USB, Address.USB_SYSCTL.address, 9, 1)
            initBaseband(usbController)
            enableI2CRepeater(usbController, true)
            val i2CRepeaterControl = false
            initTuner(i2CRepeaterControl)
            enableI2CRepeater(usbController, false)
            try {
                setSampleRate(sampleRate)
            } catch (var5: Exception) {
                throw DeviceException("RTL2832 Tuner Controller - couldn't set default sample rate: $var5")
            }
        } catch (var6: UsbException) {
            var6.printStackTrace()
            throw DeviceException("E4K Tuner Controller - error during init():$var6")
        }
    }

    @kotlin.Throws(DeviceException::class)
    override fun setSampleRateFilters(bandwidth: Int) {
        val i2CRepeaterEnabled = this.isI2CRepeaterEnabled
        if (!i2CRepeaterEnabled) {
            enableI2CRepeater(usbController, true)
        }
        val controlI2CRepeater = false
        val mixer = MixerFilter.getFilter(bandwidth)
        setMixerFilter(mixer, controlI2CRepeater)
        val channel = ChannelFilter.getFilter(bandwidth)
        setChannelFilter(channel, controlI2CRepeater)
        val rc = RCFilter.getFilter(bandwidth)
        setRCFilter(rc, controlI2CRepeater)
        if (!i2CRepeaterEnabled) {
            enableI2CRepeater(usbController, false)
        }
    }

    @get:kotlin.Throws(DeviceException::class)
    @set:kotlin.Throws(DeviceException::class)
    override var tunedFrequency: Long
        get() = try {
            val i2CRepeaterEnabled = this.isI2CRepeaterEnabled
            if (!i2CRepeaterEnabled) {
                enableI2CRepeater(usbController, true)
            }
            val controlI2CRepeater = false
            val z = readE4KRegister(E4KRegister.SYNTH3, controlI2CRepeater).toByte()
            val xHigh = readE4KRegister(E4KRegister.SYNTH4, controlI2CRepeater)
            val xLow = readE4KRegister(E4KRegister.SYNTH5, controlI2CRepeater)
            val x = Integer.rotateLeft(xHigh, 8) or xLow
            val pllSetting = readE4KRegister(E4KRegister.SYNTH7, controlI2CRepeater)
            val pll = PLL.fromSetting(pllSetting)
            if (!i2CRepeaterEnabled) {
                enableI2CRepeater(usbController, false)
            }
            calculateActualFrequency(pll, z, x)
        } catch (var9: UsbException) {
            throw DeviceException("E4K tuner controller - couldn't get tuned frequency:$var9")
        }
        set(frequency) {
            val pll = PLL.fromFrequency(frequency)
            val z = (frequency / pll.scaledOscillator.toLong()).toInt().toByte()
            val remainder = (frequency - ((z.toInt() and 255) * pll.scaledOscillator).toLong()).toInt()
            var x = (remainder.toDouble() / pll.scaledOscillator.toDouble() * 65536.0).toInt()
            var actualFrequency = calculateActualFrequency(pll, z, x)
            if (actualFrequency < 52000000L) {
                ++x
                actualFrequency = calculateActualFrequency(pll, z, x)
            }
            try {
                enableI2CRepeater(usbController, true)
                val controlI2CRepeater = false
                writeE4KRegister(E4KRegister.SYNTH7, pll.index, controlI2CRepeater)
                writeE4KRegister(E4KRegister.SYNTH3, z, controlI2CRepeater)
                writeE4KRegister(E4KRegister.SYNTH4, (x and 255).toByte(), controlI2CRepeater)
                writeE4KRegister(E4KRegister.SYNTH5, (Integer.rotateRight(x, 8) and 255).toByte(), controlI2CRepeater)
                setBand(actualFrequency, controlI2CRepeater)
                setRFFilter(actualFrequency, controlI2CRepeater)
                val lock = readE4KRegister(E4KRegister.SYNTH1, controlI2CRepeater)
                if (lock and 1 != 1) {
                    throw DeviceException("E4K tuner controller - couldn't achieve PLL lock for frequency [$actualFrequency] lock value [$lock]")
                } else {
                    enableI2CRepeater(usbController, false)
                }
            } catch (var11: UsbException) {
                throw DeviceException("E4K tuner controller - error tuning frequency [$frequency]: $var11")
            }
        }

    private fun calculateActualFrequency(pll: PLL, z: Byte, x: Int): Long {
        val whole = (pll.scaledOscillator * (z.toInt() and 255)).toLong()
        val fractional = (pll.scaledOscillator.toDouble() * (x.toDouble() / 65536.0)).toInt()
        return whole + fractional.toLong()
    }

    @kotlin.Throws(UsbException::class)
    override fun initTuner(controlI2CRepeater: Boolean) {
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, true)
        }
        val i2CRepeaterControl = false
        readE4KRegister(E4KRegister.DUMMY, i2CRepeaterControl)
        writeE4KRegister(E4KRegister.MASTER1, 7.toByte(), i2CRepeaterControl)
        writeE4KRegister(E4KRegister.CLK_INP, 0.toByte(), i2CRepeaterControl)
        writeE4KRegister(E4KRegister.REF_CLK, 0.toByte(), i2CRepeaterControl)
        writeE4KRegister(E4KRegister.CLKOUT_PWDN, ((-106).toByte()), i2CRepeaterControl)
        magicInit(i2CRepeaterControl)
        writeE4KRegister(E4KRegister.AGC4, 16.toByte(), i2CRepeaterControl)
        writeE4KRegister(E4KRegister.AGC5, 4.toByte(), i2CRepeaterControl)
        writeE4KRegister(E4KRegister.AGC6, 26.toByte(), i2CRepeaterControl)
        writeMaskedE4KRegister(E4KRegister.AGC1, 15.toByte(), AGCMode.SERIAL.value, false)
        writeMaskedE4KRegister(E4KRegister.AGC7, 1.toByte(), 0.toByte(), false)
        setLNAGain(E4KLNAGain.AUTOMATIC, i2CRepeaterControl)
        setMixerGain(E4KMixerGain.AUTOMATIC, i2CRepeaterControl)
        setEnhanceGain(E4KEnhanceGain.AUTOMATIC, i2CRepeaterControl)
        setIFStage1Gain(IFStage1Gain.GAIN_PLUS6, i2CRepeaterControl)
        setIFStage2Gain(IFStage2Gain.GAIN_PLUS0, i2CRepeaterControl)
        setIFStage3Gain(IFStage3Gain.GAIN_PLUS0, i2CRepeaterControl)
        setIFStage4Gain(IFStage4Gain.GAIN_PLUS0, i2CRepeaterControl)
        setIFStage5Gain(IFStage5Gain.GAIN_PLUS9, i2CRepeaterControl)
        setIFStage6Gain(IFStage6Gain.GAIN_PLUS9, i2CRepeaterControl)
        setMixerFilter(MixerFilter.BW_1M9, i2CRepeaterControl)
        setRCFilter(RCFilter.BW_1M0, i2CRepeaterControl)
        setChannelFilter(ChannelFilter.BW_2M15, i2CRepeaterControl)
        setChannelFilterEnabled(true, i2CRepeaterControl)
        writeMaskedE4KRegister(E4KRegister.DC5, 3.toByte(), 0.toByte(), i2CRepeaterControl)
        writeMaskedE4KRegister(E4KRegister.DCTIME1, 3.toByte(), 0.toByte(), i2CRepeaterControl)
        writeMaskedE4KRegister(E4KRegister.DCTIME2, 3.toByte(), 0.toByte(), i2CRepeaterControl)
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, false)
        }
    }

    @kotlin.Throws(UsbException::class)
    private fun generateDCOffsetTables(controlI2CRepeater: Boolean) {
        val i2CRepeaterControl = false
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, true)
        }
        getIFStage1Gain(i2CRepeaterControl)
        getIFStage2Gain(i2CRepeaterControl)
        getIFStage3Gain(i2CRepeaterControl)
        getIFStage4Gain(i2CRepeaterControl)
        getIFStage5Gain(i2CRepeaterControl)
        getIFStage6Gain(i2CRepeaterControl)
        val mixerGain = getMixerGain(i2CRepeaterControl)
        setMixerGain(mixerGain, i2CRepeaterControl)
        val lnaGain = getLNAGain(i2CRepeaterControl)
        setLNAGain(lnaGain, i2CRepeaterControl)
        setIFStage2Gain(IFStage2Gain.GAIN_PLUS9, i2CRepeaterControl)
        setIFStage3Gain(IFStage3Gain.GAIN_PLUS9, i2CRepeaterControl)
        setIFStage4Gain(IFStage4Gain.GAIN_PLUS2B, i2CRepeaterControl)
        setIFStage5Gain(IFStage5Gain.GAIN_PLUS15D, i2CRepeaterControl)
        setIFStage6Gain(IFStage6Gain.GAIN_PLUS15D, i2CRepeaterControl)
        var var14: Array<DCGainCombination>
        val var13: Int = DCGainCombination.values().also { var14 = it }.size
        for (var12 in 0 until var13) {
            val combo = var14[var12]
            setMixerGain(combo.mixerGain, i2CRepeaterControl)
            setIFStage1Gain(combo.iFStage1Gain, i2CRepeaterControl)
            setDCRangeDetectorEnabled(true, i2CRepeaterControl)
            writeE4KRegister(E4KRegister.DC1, 1.toByte(), i2CRepeaterControl)
            val offsetI = (readE4KRegister(E4KRegister.DC2, i2CRepeaterControl) and 63).toByte()
            val offsetQ = (readE4KRegister(E4KRegister.DC3, i2CRepeaterControl) and 63).toByte()
            val range = readE4KRegister(E4KRegister.DC4, i2CRepeaterControl).toByte()
            val rangeI = (range.toInt() and 3).toByte()
            val rangeQ = (range.toInt() shr 4 and 3).toByte()
            writeE4KRegister(combo.iRegister, (offsetI.toInt() or (rangeI.toInt() shl 6)).toByte(), i2CRepeaterControl)
            writeE4KRegister(combo.qRegister, (offsetQ.toInt() or (rangeQ.toInt() shl 6)).toByte(), i2CRepeaterControl)
        }
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, false)
        }
    }

    @kotlin.Throws(UsbException::class)
    fun setDCRangeDetectorEnabled(enabled: Boolean, controlI2CRepeater: Boolean) {
        if (enabled) {
            writeMaskedE4KRegister(E4KRegister.DC5, 4.toByte(), 4.toByte(), controlI2CRepeater)
        } else {
            writeMaskedE4KRegister(E4KRegister.DC5, 3.toByte(), 0.toByte(), controlI2CRepeater)
        }
    }

    @kotlin.Throws(UsbException::class)
    fun setLNAGain(gain: E4KLNAGain?, controlI2CRepeater: Boolean) {
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, true)
        }
        if (gain == E4KLNAGain.AUTOMATIC) {
            writeMaskedE4KRegister(E4KRegister.AGC1, 15.toByte(), AGCMode.IF_SERIAL_LNA_AUTON.value, false)
        } else {
            writeMaskedE4KRegister(E4KRegister.AGC1, 15.toByte(), AGCMode.SERIAL.value, false)
            writeMaskedE4KRegister(E4KLNAGain.register, E4KLNAGain.mask, gain!!.mValue.toByte(), false)
        }
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, false)
        }
    }

    @kotlin.Throws(UsbException::class)
    fun getLNAGain(controlI2CRepeater: Boolean): E4KLNAGain {
        return E4KLNAGain.fromRegisterValue(readE4KRegister(E4KLNAGain.register, controlI2CRepeater))
    }

    @kotlin.Throws(UsbException::class)
    fun setEnhanceGain(gain: E4KEnhanceGain?, controlI2CRepeater: Boolean) {
        writeMaskedE4KRegister(E4KEnhanceGain.register, E4KEnhanceGain.mask, gain!!.mValue.toByte(), controlI2CRepeater)
    }

    @kotlin.Throws(UsbException::class)
    fun getEnhanceGain(controlI2CRepeater: Boolean): E4KEnhanceGain {
        return E4KEnhanceGain.fromRegisterValue(readE4KRegister(E4KEnhanceGain.register, controlI2CRepeater))
    }

    @kotlin.Throws(UsbException::class)
    fun setMixerGain(gain: E4KMixerGain?, controlI2CRepeater: Boolean) {
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, true)
        }
        val localI2CRepeaterControl = false
        if (gain == E4KMixerGain.AUTOMATIC) {
            writeMaskedE4KRegister(E4KRegister.AGC7, 1.toByte(), 1.toByte(), localI2CRepeaterControl)
        } else {
            writeMaskedE4KRegister(E4KRegister.AGC7, 1.toByte(), 0.toByte(), localI2CRepeaterControl)
            writeMaskedE4KRegister(
                E4KMixerGain.register,
                E4KMixerGain.mask,
                gain!!.mValue.toByte(),
                localI2CRepeaterControl
            )
        }
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, false)
        }
    }

    @kotlin.Throws(UsbException::class)
    fun getMixerGain(controlI2CRepeater: Boolean): E4KMixerGain {
        val autoOrManual = readMaskedE4KRegister(E4KRegister.AGC7, 1.toByte(), controlI2CRepeater)
        return if (autoOrManual.toInt() == 1) {
            E4KMixerGain.AUTOMATIC
        } else {
            val register = readE4KRegister(E4KMixerGain.register, controlI2CRepeater)
            E4KMixerGain.fromRegisterValue(register)
        }
    }

    @kotlin.Throws(UsbException::class)
    fun setIFStage1Gain(gain: IFStage1Gain, controlI2CRepeater: Boolean) {
        writeMaskedE4KRegister(IFStage1Gain.register, IFStage1Gain.mask, gain.mValue.toByte(), controlI2CRepeater)
    }

    @kotlin.Throws(UsbException::class)
    fun getIFStage1Gain(controlI2CRepeater: Boolean): IFStage1Gain {
        return IFStage1Gain.fromRegisterValue(readE4KRegister(IFStage1Gain.register, controlI2CRepeater))
    }

    @kotlin.Throws(UsbException::class)
    fun setIFStage2Gain(gain: IFStage2Gain, controlI2CRepeater: Boolean) {
        writeMaskedE4KRegister(IFStage2Gain.register, IFStage2Gain.mask, gain.mValue.toByte(), controlI2CRepeater)
    }

    @kotlin.Throws(UsbException::class)
    fun getIFStage2Gain(controlI2CRepeater: Boolean): IFStage2Gain {
        return IFStage2Gain.fromRegisterValue(readE4KRegister(IFStage2Gain.register, controlI2CRepeater))
    }

    @kotlin.Throws(UsbException::class)
    fun setIFStage3Gain(gain: IFStage3Gain, controlI2CRepeater: Boolean) {
        writeMaskedE4KRegister(IFStage3Gain.register, IFStage3Gain.mask, gain.mValue.toByte(), controlI2CRepeater)
    }

    @kotlin.Throws(UsbException::class)
    fun getIFStage3Gain(controlI2CRepeater: Boolean): IFStage3Gain {
        return IFStage3Gain.fromRegisterValue(readE4KRegister(IFStage3Gain.register, controlI2CRepeater))
    }

    @kotlin.Throws(UsbException::class)
    fun setIFStage4Gain(gain: IFStage4Gain, controlI2CRepeater: Boolean) {
        writeMaskedE4KRegister(IFStage4Gain.register, IFStage4Gain.mask, gain.mValue.toByte(), controlI2CRepeater)
    }

    @kotlin.Throws(UsbException::class)
    fun getIFStage4Gain(controlI2CRepeater: Boolean): IFStage4Gain {
        return IFStage4Gain.fromRegisterValue(readE4KRegister(IFStage4Gain.register, controlI2CRepeater))
    }

    @kotlin.Throws(UsbException::class)
    fun setIFStage5Gain(gain: IFStage5Gain, controlI2CRepeater: Boolean) {
        writeMaskedE4KRegister(IFStage5Gain.register, IFStage5Gain.mask, gain.mValue.toByte(), controlI2CRepeater)
    }

    @kotlin.Throws(UsbException::class)
    fun getIFStage5Gain(controlI2CRepeater: Boolean): IFStage5Gain {
        return IFStage5Gain.fromRegisterValue(readE4KRegister(IFStage5Gain.register, controlI2CRepeater))
    }

    @kotlin.Throws(UsbException::class)
    fun setIFStage6Gain(gain: IFStage6Gain, controlI2CRepeater: Boolean) {
        writeMaskedE4KRegister(IFStage6Gain.register, IFStage6Gain.mask, gain.mValue.toByte(), controlI2CRepeater)
    }

    @kotlin.Throws(UsbException::class)
    fun getIFStage6Gain(controlI2CRepeater: Boolean): IFStage6Gain {
        return IFStage6Gain.fromRegisterValue(readE4KRegister(IFStage6Gain.register, controlI2CRepeater))
    }

    @kotlin.Throws(UsbException::class)
    fun setMixerFilter(filter: MixerFilter, controlI2CRepeater: Boolean) {
        writeMaskedE4KRegister(MixerFilter.register, MixerFilter.mask, filter.mValue.toByte(), controlI2CRepeater)
    }

    @kotlin.Throws(UsbException::class)
    fun getMixerFilter(controlI2CRepeater: Boolean): MixerFilter {
        val value = readE4KRegister(MixerFilter.register, controlI2CRepeater)
        return MixerFilter.fromRegisterValue(value)
    }

    @kotlin.Throws(UsbException::class)
    fun setRCFilter(filter: RCFilter, controlI2CRepeater: Boolean) {
        writeMaskedE4KRegister(RCFilter.register, RCFilter.mask, filter.mValue.toByte(), controlI2CRepeater)
    }

    @kotlin.Throws(UsbException::class)
    fun getRCFilter(controlI2CRepeater: Boolean): RCFilter {
        val value = readE4KRegister(RCFilter.register, controlI2CRepeater)
        return RCFilter.fromRegisterValue(value)
    }

    @kotlin.Throws(UsbException::class)
    fun setChannelFilter(filter: ChannelFilter, controlI2CRepeater: Boolean) {
        writeMaskedE4KRegister(ChannelFilter.register, ChannelFilter.mask, filter.mValue.toByte(), controlI2CRepeater)
    }

    @kotlin.Throws(UsbException::class)
    fun getChannelFilter(controlI2CRepeater: Boolean): ChannelFilter {
        val value = readE4KRegister(ChannelFilter.register, controlI2CRepeater)
        return ChannelFilter.fromRegisterValue(value)
    }

    @kotlin.Throws(UsbException::class)
    fun setChannelFilterEnabled(enabled: Boolean, controlI2CRepeater: Boolean) {
        if (enabled) {
            writeMaskedE4KRegister(E4KRegister.FILT3, 32.toByte(), 0.toByte(), controlI2CRepeater)
        } else {
            writeMaskedE4KRegister(E4KRegister.FILT3, 32.toByte(), 32.toByte(), controlI2CRepeater)
        }
    }

    @kotlin.Throws(UsbException::class)
    fun setBand(frequency: Long, controlI2CRepeater: Boolean) {
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, true)
        }
        val band = Band.fromFrequency(frequency)
        when ((band.ordinal)) {
            1, 2, 3 -> writeE4KRegister(
                E4KRegister.BIAS, 3.toByte(), false
            )
            4 -> writeE4KRegister(E4KRegister.BIAS, 0.toByte(), false)
            else -> writeE4KRegister(E4KRegister.BIAS, 0.toByte(), false)
        }
        writeMaskedE4KRegister(E4KRegister.SYNTH1, Band.mask, 0.toByte(), false)
        writeMaskedE4KRegister(E4KRegister.SYNTH1, Band.mask, band.mValue.toByte(), false)
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, false)
        }
    }

    @kotlin.Throws(UsbException::class)
    private fun setRFFilter(frequency: Long, controlI2CRepeater: Boolean) {
        val filter = RFFilter.fromFrequency(frequency)
        writeMaskedE4KRegister(E4KRegister.FILT1, RFFilter.mask, filter.mValue.toByte(), controlI2CRepeater)
    }

    @kotlin.Throws(UsbException::class)
    fun setGain(gain: E4KGain, controlI2CRepeater: Boolean) {
        if (gain != E4KGain.MANUAL) {
            if (controlI2CRepeater) {
                enableI2CRepeater(usbController, true)
            }
            val i2CRepeaterControl = false
            setLNAGain(gain.lNAGain, i2CRepeaterControl)
            setMixerGain(gain.mixerGain, i2CRepeaterControl)
            setEnhanceGain(gain.enhanceGain, i2CRepeaterControl)
            if (controlI2CRepeater) {
                enableI2CRepeater(usbController, false)
            }
        }
    }

    @kotlin.Throws(UsbException::class)
    private fun magicInit(controlI2CRepeater: Boolean) {
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, true)
        }
        writeE4KRegister(E4KRegister.MAGIC_1, 1.toByte(), false)
        writeE4KRegister(E4KRegister.MAGIC_2, ((-2).toByte()), false)
        writeE4KRegister(E4KRegister.MAGIC_3, 0.toByte(), false)
        writeE4KRegister(E4KRegister.MAGIC_4, 80.toByte(), false)
        writeE4KRegister(E4KRegister.MAGIC_5, 32.toByte(), false)
        writeE4KRegister(E4KRegister.MAGIC_6, 1.toByte(), false)
        writeE4KRegister(E4KRegister.MAGIC_7, 127.toByte(), false)
        writeE4KRegister(E4KRegister.MAGIC_8, 7.toByte(), false)
        if (controlI2CRepeater) {
            enableI2CRepeater(usbController, false)
        }
    }

    @kotlin.Throws(UsbException::class)
    private fun readMaskedE4KRegister(register: E4KRegister, mask: Byte, controlI2CRepeater: Boolean): Byte {
        val temp = readE4KRegister(register, controlI2CRepeater)
        return (temp and mask.toInt()).toByte()
    }

    @kotlin.Throws(UsbException::class)
    private fun writeMaskedE4KRegister(register: E4KRegister, mask: Byte, value: Byte, controlI2CRepeater: Boolean) {
        val temp = readE4KRegister(register, controlI2CRepeater)
        if ((temp and mask.toInt()).toByte() != value) {
            writeE4KRegister(
                register,
                (temp and mask.inv().toInt() or (value.toInt() and mask.toInt())).toByte(),
                controlI2CRepeater
            )
            readE4KRegister(register, controlI2CRepeater)
        }
    }

    @kotlin.Throws(UsbException::class)
    private fun readE4KRegister(register: E4KRegister, controlI2CRepeater: Boolean): Int {
        return readI2CRegister(
            usbController,
            E4KRegister.I2C_REGISTER.mValue.toByte(),
            register.mValue.toByte(),
            controlI2CRepeater
        )
    }

    @kotlin.Throws(UsbException::class)
    private fun writeE4KRegister(register: E4KRegister, value: Byte, controlI2CRepeater: Boolean) {
        writeI2CRegister(
            E4KRegister.I2C_REGISTER.mValue.toByte(),
            register.mValue.toByte(),
            value,
            controlI2CRepeater
        )
    }

    enum class AGCMode(val value: Byte) {
        SERIAL(0.toByte()), IF_PWM_LNA_SERIAL(1.toByte()), IF_PWM_LNA_AUTONL(2.toByte()), IF_PWM_LNA_SUPERV(3.toByte()), IF_SERIAL_LNA_PWM(
            4.toByte()
        ),
        IF_PWM_LNA_PWM(5.toByte()), IF_DIG_LNA_SERIAL(6.toByte()), IF_DIG_LNA_AUTON(7.toByte()), IF_DIG_LNA_SUPERV(8.toByte()), IF_SERIAL_LNA_AUTON(
            9.toByte()
        ),
        IF_SERIAL_LNA_SUPERV(10.toByte());

    }

    enum class Band(val mValue: Int) {
        VHF2(0), VHF3(2), UHF(4),  // $FF: renamed from: L com.g0kla.rtlsdr4java.E4KTunerController$Band
        field_0(6);

        val value: Byte
            get() = mValue.toByte()
        val label: String
            get() = this.toString()

        companion object {
            val mask: Byte
                get() = 6

            fun fromFrequency(frequency: Long): Band {
                return if (frequency < 140000000L) {
                    VHF2
                } else if (frequency < 350000000L) {
                    VHF3
                } else {
                    if (frequency < 1135000000L) UHF else field_0
                }
            }
        }
    }

    enum class ChannelFilter(
        val mValue: Int,
        val bandwidth: Int,
        val minimumBandwidth: Int,
        val maximumBandwidth: Int,
        val label: String
    ) {
        BW_5M50(0, 5500000, 5400000, 28800000, "5.50 MHz"), BW_5M30(
            1,
            5300000,
            5150000,
            5400000,
            "5.30 MHz"
        ),
        BW_5M00(2, 5000000, 4900000, 5150000, "5.00 MHz"), BW_4M80(3, 4800000, 4700000, 4900000, "4.80 MHz"), BW_4M60(
            4,
            4600000,
            4500000,
            4700000,
            "4.60 MHz"
        ),
        BW_4M40(5, 4400000, 4350000, 4500000, "4.40 MHz"), BW_4M30(6, 4300000, 4200000, 4350000, "4.30 MHz"), BW_4M10(
            7,
            4100000,
            4000000,
            4200000,
            "4.10 MHz"
        ),
        BW_3M90(8, 3900000, 3850000, 4000000, "3.90 MHz"), BW_3M80(
            9,
            3800000,
            3750000,
            3850000,
            "3.80 MHz"
        ),
        BW_3M70(10, 3700000, 3650000, 3750000, "3.70 MHz"), BW_3M60(11, 3600000, 3500000, 3650000, "3.60 MHz"), BW_3M40(
            12,
            3400000,
            3350000,
            3500000,
            "3.40 MHz"
        ),
        BW_3M30(13, 3300000, 3250000, 3350000, "3.30 MHz"), BW_3M20(14, 3200000, 3150000, 3250000, "3.20 MHz"), BW_3M10(
            15,
            3100000,
            3050000,
            3150000,
            "3.10 MHz"
        ),
        BW_3M00(16, 3000000, 2975000, 3050000, "3.00 MHz"), BW_2M95(17, 2950000, 2925000, 2975000, "2.95 MHz"), BW_2M90(
            18,
            2900000,
            2850000,
            2925000,
            "2.90 MHz"
        ),
        BW_2M80(19, 2800000, 2775000, 2850000, "2.80 MHz"), BW_2M75(20, 2750000, 2750000, 2775000, "2.75 MHz"), BW_2M70(
            21,
            2700000,
            2650000,
            2750000,
            "2.70 MHz"
        ),
        BW_2M60(22, 2600000, 2575000, 2650000, "2.60 MHz"), BW_2M55(23, 2550000, 2525000, 2575000, "2.55 MHz"), BW_2M50(
            24,
            2500000,
            2475000,
            2525000,
            "2.50 MHz"
        ),
        BW_2M45(25, 2450000, 2425000, 2475000, "2.45 MHz"), BW_2M40(26, 2400000, 2350000, 2425000, "2.40 MHz"), BW_2M30(
            27,
            2300000,
            2290000,
            2350000,
            "2.30 MHz"
        ),
        BW_2M28(28, 2280000, 2260000, 2290000, "2.28 MHz"), BW_2M24(29, 2240000, 2220000, 2260000, "2.24 MHz"), BW_2M20(
            30,
            2200000,
            2175000,
            2220000,
            "2.20 MHz"
        ),
        BW_2M15(31, 2150000, 0, 2175000, "2.15 MHz");

        val value: Byte
            get() = mValue.toByte()

        companion object {
            val register: E4KRegister
                get() = E4KRegister.FILT3
            val mask: Byte
                get() = 31

            fun getFilter(bandwidth: Int): ChannelFilter {
                var var4: Array<ChannelFilter>
                val var3: Int = values().also { var4 = it }.size
                for (var2 in 0 until var3) {
                    val filter = var4[var2]
                    if (filter.minimumBandwidth <= bandwidth && bandwidth < filter.maximumBandwidth) {
                        return filter
                    }
                }
                return BW_5M50
            }

            fun fromRegisterValue(registerValue: Int): ChannelFilter {
                val value = registerValue and mask.toInt()
                var var5: Array<ChannelFilter>
                val var4: Int = values().also { var5 = it }.size
                for (var3 in 0 until var4) {
                    val filter = var5[var3]
                    if (value == filter.mValue) {
                        return filter
                    }
                }
                throw IllegalArgumentException("E4KTunerController - unrecognized channel filter value [$value]")
            }
        }
    }

    enum class DCGainCombination(
        val qRegister: E4KRegister,
        val iRegister: E4KRegister,
        val mixerGain: E4KMixerGain,
        val iFStage1Gain: IFStage1Gain
    ) {
        LOOKUP_TABLE_0(E4KRegister.QLUT0, E4KRegister.ILUT0, E4KMixerGain.GAIN_4, IFStage1Gain.GAIN_MINUS3), LOOKUP_TABLE_1(
            E4KRegister.QLUT1, E4KRegister.ILUT1, E4KMixerGain.GAIN_4, IFStage1Gain.GAIN_PLUS6
        ),
        LOOKUP_TABLE_2(E4KRegister.QLUT2, E4KRegister.ILUT2, E4KMixerGain.GAIN_12, IFStage1Gain.GAIN_MINUS3), LOOKUP_TABLE_3(
            E4KRegister.QLUT3, E4KRegister.ILUT3, E4KMixerGain.GAIN_12, IFStage1Gain.GAIN_PLUS6
        );

    }

    enum class E4KEnhanceGain(val mValue: Int, val label: String) {
        AUTOMATIC(0, "Auto"), GAIN_1(1, "10 db"), GAIN_3(3, "30 db"), GAIN_5(5, "50 db"), GAIN_7(7, "70 db");

        val value: Byte
            get() = mValue.toByte()

        override fun toString(): String {
            return label
        }

        companion object {
            val register: E4KRegister
                get() = E4KRegister.AGC11
            val mask: Byte
                get() = 7

            fun fromRegisterValue(registerValue: Int): E4KEnhanceGain {
                val value = registerValue and mask.toInt()
                var var5: Array<E4KEnhanceGain>
                val var4: Int = values().also { var5 = it }.size
                for (var3 in 0 until var4) {
                    val setting = var5[var3]
                    if (value == setting.mValue) {
                        return setting
                    }
                }
                throw IllegalArgumentException("E4KTunerController - unrecognized Enhance Gain value [$value]")
            }
        }
    }

    enum class E4KGain(
        val label: String,
        val mixerGain: E4KMixerGain?,
        val lNAGain: E4KLNAGain?,
        val enhanceGain: E4KEnhanceGain?
    ) {
        AUTOMATIC("Auto", E4KMixerGain.AUTOMATIC, E4KLNAGain.AUTOMATIC, E4KEnhanceGain.AUTOMATIC), MANUAL(
            "Manual",
            null as E4KMixerGain?,
            null as E4KLNAGain?,
            null as E4KEnhanceGain?
        ),
        MINUS_10("-1.0 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_MINUS_50, E4KEnhanceGain.AUTOMATIC), PLUS_15(
            "1.5 db",
            E4KMixerGain.GAIN_4,
            E4KLNAGain.GAIN_MINUS_25,
            E4KEnhanceGain.AUTOMATIC
        ),
        PLUS_40("4.0 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_0, E4KEnhanceGain.AUTOMATIC), PLUS_65(
            "6.5 db",
            E4KMixerGain.GAIN_4,
            E4KLNAGain.GAIN_PLUS_25,
            E4KEnhanceGain.AUTOMATIC
        ),
        PLUS_90("9.0 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_50, E4KEnhanceGain.AUTOMATIC), PLUS_115(
            "11.5 db",
            E4KMixerGain.GAIN_4,
            E4KLNAGain.GAIN_PLUS_75,
            E4KEnhanceGain.AUTOMATIC
        ),
        PLUS_140(
            "14.0 db",
            E4KMixerGain.GAIN_4,
            E4KLNAGain.GAIN_PLUS_100,
            E4KEnhanceGain.AUTOMATIC
        ),
        PLUS_165(
            "16.5 db",
            E4KMixerGain.GAIN_4,
            E4KLNAGain.GAIN_PLUS_125,
            E4KEnhanceGain.AUTOMATIC
        ),
        PLUS_190(
            "19.0 db",
            E4KMixerGain.GAIN_4,
            E4KLNAGain.GAIN_PLUS_150,
            E4KEnhanceGain.AUTOMATIC
        ),
        PLUS_215(
            "21.5 db",
            E4KMixerGain.GAIN_4,
            E4KLNAGain.GAIN_PLUS_175,
            E4KEnhanceGain.AUTOMATIC
        ),
        PLUS_240(
            "24.0 db",
            E4KMixerGain.GAIN_4,
            E4KLNAGain.GAIN_PLUS_200,
            E4KEnhanceGain.AUTOMATIC
        ),
        PLUS_290(
            "29.0 db",
            E4KMixerGain.GAIN_4,
            E4KLNAGain.GAIN_PLUS_250,
            E4KEnhanceGain.AUTOMATIC
        ),
        PLUS_340(
            "34.0 db",
            E4KMixerGain.GAIN_4,
            E4KLNAGain.GAIN_PLUS_300,
            E4KEnhanceGain.AUTOMATIC
        ),
        PLUS_420("42.0 db", E4KMixerGain.GAIN_12, E4KLNAGain.GAIN_PLUS_300, E4KEnhanceGain.AUTOMATIC);

        override fun toString(): String {
            return label
        }
    }

    enum class E4KLNAGain(val mValue: Int, val label: String) {
        AUTOMATIC(-1, "Auto"), GAIN_MINUS_50(0, "-5.0 db"), GAIN_MINUS_25(1, "-2.5 db"), GAIN_PLUS_0(
            4,
            "0.0 db"
        ),
        GAIN_PLUS_25(5, "2.5 db"), GAIN_PLUS_50(6, "5.0 db"), GAIN_PLUS_75(7, "7.5 db"), GAIN_PLUS_100(
            8,
            "10.0 db"
        ),
        GAIN_PLUS_125(9, "12.5 db"), GAIN_PLUS_150(10, "15.0 db"), GAIN_PLUS_175(11, "17.5 db"), GAIN_PLUS_200(
            12,
            "20.0 db"
        ),
        GAIN_PLUS_250(13, "25.0 db"), GAIN_PLUS_300(14, "30.0 db");

        val value: Byte
            get() = mValue.toByte()

        override fun toString(): String {
            return label
        }

        companion object {
            val register: E4KRegister
                get() = E4KRegister.GAIN1
            val mask: Byte
                get() = 15

            fun fromRegisterValue(registerValue: Int): E4KLNAGain {
                val value = registerValue and mask.toInt()
                var var5: Array<E4KLNAGain>
                val var4: Int = values().also { var5 = it }.size
                for (var3 in 0 until var4) {
                    val setting = var5[var3]
                    if (value == setting.mValue) {
                        return setting
                    }
                }
                throw IllegalArgumentException("E4KTunerController - unrecognized LNA Gain value [$value]")
            }
        }
    }

    enum class E4KMixerGain(val mValue: Int, val label: String) {
        AUTOMATIC(-1, "Auto"), GAIN_4(0, "4 db"), GAIN_12(1, "12 db");

        val value: Byte
            get() = mValue.toByte()

        override fun toString(): String {
            return label
        }

        companion object {
            val register: E4KRegister
                get() = E4KRegister.GAIN2
            val mask: Byte
                get() = 1

            fun fromRegisterValue(registerValue: Int): E4KMixerGain {
                val value = registerValue and mask.toInt()
                var var5: Array<E4KMixerGain>
                val var4: Int = values().also { var5 = it }.size
                for (var3 in 0 until var4) {
                    val setting = var5[var3]
                    if (value == setting.mValue) {
                        return setting
                    }
                }
                throw IllegalArgumentException("E4KTunerController - unrecognized Mixer Gain value [$value]")
            }
        }
    }

    enum class IFFilter(val label: String) {
        MIX("Mix"), CHAN("Channel"),  // $FF: renamed from: RC com.g0kla.rtlsdr4java.E4KTunerController$IFFilter
        field_1("RC");

    }

    enum class IFStage1Gain(val mValue: Int, val label: String) {
        GAIN_MINUS3(0, "-3 db"), GAIN_PLUS6(1, "6 db");

        val value: Byte
            get() = mValue.toByte()

        override fun toString(): String {
            return label
        }

        companion object {
            val register: E4KRegister
                get() = E4KRegister.GAIN3
            val mask: Byte
                get() = 1

            fun fromRegisterValue(registerValue: Int): IFStage1Gain {
                val value = registerValue and mask.toInt()
                var var5: Array<IFStage1Gain>
                val var4: Int = values().also { var5 = it }.size
                for (var3 in 0 until var4) {
                    val setting = var5[var3]
                    if (value == setting.mValue) {
                        return setting
                    }
                }
                throw IllegalArgumentException("E4KTunerController - unrecognized IF Gain Stage 1 value [$value]")
            }
        }
    }

    companion object {
        const val MIN_FREQUENCY = 52000000L
        const val MAX_FREQUENCY = 2200000000L
        const val USABLE_BANDWIDTH_PERCENT = 0.95
        const val DC_SPIKE_AVOID_BUFFER = 15000
        const val E4K_PLL_Y = 65536L
        const val MASTER1_RESET: Byte = 1
        const val MASTER1_NORM_STBY: Byte = 2
        const val MASTER1_POR_DET: Byte = 4
        const val SYNTH1_PLL_LOCK: Byte = 1
        const val SYNTH1_BAND_SHIF: Byte = 1
        const val SYNTH7_3PHASE_EN: Byte = 8
        const val SYNTH8_VCOCAL_UPD: Byte = 4
        const val FILT3_MASK: Byte = 32
        const val FILT3_ENABLE: Byte = 0
        const val FILT3_DISABLE: Byte = 32
        const val MIXER_FILTER_MASK: Byte = -16
        const val IF_CHANNEL_FILTER_MASK: Byte = 31
        const val IF_RC_FILTER_MASK: Byte = 15
        const val AGC1_LIN_MODE: Byte = 16
        const val AGC1_LNA_UPDATE: Byte = 32
        const val AGC1_LNA_G_LOW: Byte = 64
        const val AGC1_LNA_G_HIGH: Byte = -128
        const val AGC1_MOD_MASK: Byte = 15
        const val GAIN1_MOD_MASK: Byte = 15
        const val IF_GAIN_MODE_SWITCHING_MASK: Byte = 1
        const val AGC6_LNA_CAL_REQ: Byte = 16
        const val AGC7_MIXER_GAIN_MASK: Byte = 1
        const val AGC7_MIX_GAIN_MANUAL: Byte = 0
        const val AGC7_MIX_GAIN_AUTO: Byte = 1
        const val AGC7_GAIN_STEP_5DB: Byte = 32
        const val AGC8_SENS_LIN_AUTO: Byte = 1
        const val AGC11_LNA_GAIN_ENH: Byte = 1
        const val ENH_GAIN_MOD_MASK: Byte = 7
        const val MIXER_GAIN_MASK: Byte = 1
        const val DC1_CAL_REQ: Byte = 1
        const val DC5_I_LUT_EN: Byte = 1
        const val DC5_Q_LUT_EN: Byte = 2
        const val DC5_RANGE_DETECTOR_ENABLED_MASK: Byte = 4
        const val DC5_RANGE_DETECTOR_ENABLED: Byte = 4
        const val DC5_RANGE_EN: Byte = 8
        const val DC5_RANGE_DETECTOR_DISABLED_MASK: Byte = 3
        const val DC5_RANGE_DETECTOR_DISABLED: Byte = 0
        const val DC5_TIMEVAR_EN: Byte = 16
        const val CLKOUT_DISABLE: Byte = -106
        const val CHFCALIB_CMD: Byte = 1
    }
}