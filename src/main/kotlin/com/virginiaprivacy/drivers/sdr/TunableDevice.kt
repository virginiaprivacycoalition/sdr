package com.virginiaprivacy.drivers.sdr

import com.virginiaprivacy.drivers.sdr.r2xx.Reg

interface TunableDevice : TunableGain {

    val config: RTLConfig
    var ppmCorrection: Int
    var initDone: Boolean
    val ifFrequency: Long

    var rate: Int
    var directSampling: Boolean
    var bandwidth: Long

    fun rtlXtal(): Int = config.xtal

    fun getTunerFreq() = (((this.ifFrequency) * (1.0 + this.ppmCorrection / 1e6)))

    fun getXtalFreq() = (((this.rtlXtal()) * (1.0 + this.ppmCorrection / 1e6)))

    fun setFrequency(freq: Long)

    fun writeReg(reg: Reg, value: Byte)

    fun init()
}