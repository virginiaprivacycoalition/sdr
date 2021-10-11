package com.virginiaprivacy.drivers.sdr.data

import com.virginiaprivacy.drivers.sdr.plugins.Plugin
import kotlin.experimental.and

data class Sample(val iByte: Byte, val qByte: Byte, val index: Int) {

    val i: Int
    get() = iByte.toInt().and(0xFF)

    val q: Int
    get() = qByte.toInt().and(0xFF)

}


val convertBufToSamplesPlugin = Plugin {
    it.asList().windowed(2, 2).map {

    }
}