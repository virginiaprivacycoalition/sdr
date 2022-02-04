package com.virginiaprivacy.drivers.sdr.r2xx

import com.virginiaprivacy.drivers.sdr.RTLConfig
import com.virginiaprivacy.drivers.sdr.Tuner

class R82xxConfig constructor(
    tuner: Tuner,
    maxI2cMsgLen: Int = 8,
    usePreDetect: Boolean = false,
) : RTLConfig(maxI2cMsgLen, usePreDetect) {

    val i2cAddr by tuner::i2cAddress
    val rafaelChip by tuner::chip
}