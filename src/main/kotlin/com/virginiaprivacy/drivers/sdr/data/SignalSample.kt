package com.virginiaprivacy.drivers.sdr.data

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

@JvmInline
value class SignalSample(val bytes: ByteArray) {

    fun magnitude() =
        bytes.asSequence()
            .chunked(2)
            .map {
                val i = abs(it[0] * 2)
                val q = abs(it[1] * 2)
                magnitudeLut[(i
                        * 256 + q)]
            }
            .toList()
            .toIntArray()

    fun magLength() = bytes.size / 2


    companion object {
        val magnitudeLut = IntArray(255 * 255 * 2).apply {
            (0.until(256)).forEach { i ->
                (0.until(256)).forEach { q ->
                    val multiplier = 2
                    fun mag(iq: Int) = (iq * multiplier) - 255

                    val magI = mag(i)
                    val magQ = mag(q)
                    val mag = ((sqrt(((magI * magI) + (magQ * magQ)).toDouble()) * 258.433254) - 365.4798).roundToInt()
                    this[i * 256 + q] = if (mag > 65535) 65535 else mag
                }
            }
        }
    }
}