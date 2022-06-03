package com.virginiaprivacy.sdr.tuner.e4k

sealed class AutomaticGainControl(val value: Byte) {
    object SerialInterfaceControl : AutomaticGainControl(0x0000)
    object PWMIFManualLNAControl : AutomaticGainControl(0x1)


}