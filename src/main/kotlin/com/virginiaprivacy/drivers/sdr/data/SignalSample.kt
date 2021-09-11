package com.virginiaprivacy.drivers.sdr.data

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

@JvmInline
value class SignalSample(val bytes: ByteArray) {

    fun magnitude() =
        bytes.toList().windowed(2, 2, false, transform)

    fun magLength() = bytes.size / 2


    companion object {
        val transform: (List<Byte>) -> Int = { l ->
            squareInts[(l[0].toInt() and 0xff)] +
                    squareInts[l[1].toInt() and 0xff]

        }
        val squareInts: Array<Int> = (0..256).map {
            val i = it.abs8()
            i * i }.toTypedArray()
    }
}

fun Int.abs8(): Int = if (this >= 127) { this - 127 } else { 127 - this }
