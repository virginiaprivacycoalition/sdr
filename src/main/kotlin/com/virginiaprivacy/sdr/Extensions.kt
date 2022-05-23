package com.virginiaprivacy.sdr

import com.virginiaprivacy.sdr.sample.SampleRate
import kotlin.math.pow

fun ByteArray.littleEndian2(bitsPerSample: Int): Int {
    val b1 = this[0]
    val b2 = this[1]
    var value = (b2.toInt() and 0xff shl 8
            or (b1.toInt() and 0xff shl 0))
    if (value > 2.0.pow((bitsPerSample - 1).toDouble()) - 1) value =
        (-1 * 2.0.pow(bitsPerSample.toDouble()) + value).toInt()
    return value
}

fun ByteArray.psd(bitsPerSample: Int): Double {
    val value = this.littleEndian2(bitsPerSample)
    return value / 2.0.pow(bitsPerSample.toDouble() - 1.0)
}

fun Number.toSampleRate(): SampleRate = SampleRate.getClosest(this.toInt())


var debug = false
