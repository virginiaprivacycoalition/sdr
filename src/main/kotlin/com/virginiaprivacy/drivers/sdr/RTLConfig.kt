package com.virginiaprivacy.drivers.sdr


open class RTLConfig(
    var maxI2cMsgLen: Int = 8,
    var usePreDetect: Boolean = false,
) {

    val xtal: Int = RTLDevice.DEFAULT_RTL_XTAL_FREQ

}