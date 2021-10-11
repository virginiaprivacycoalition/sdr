package com.virginiaprivacy.drivers.sdr

import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
interface TunableDevice {

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

    fun getTunedFrequency(): Long

    fun setGain(manualGain: Boolean, gain: Int? = null)

    fun writeReg(reg: Int, value: Int)

    fun read(reg: Int, len: Int): ByteArray

    fun writeRegMask(reg: Int, value: Int, bitMask: Int)
}