package com.virginiaprivacy.drivers.sdr

operator fun IntArray.get(byte: Byte): Int {
    return this[(byte and 0XFF).toInt()]
}

operator fun ByteArray.get(byte: Byte): Byte {
    return this[(byte and 0XFF).toInt()]
}

infix fun <T : Number> Byte.and(int: T) = this.toInt().and(int.toInt()).toByte()

infix fun Byte.shl(pos: Int) = this.toInt().shl(pos)

infix fun Byte.shr(pos: Int) = this.toInt().shr(pos)


fun Int.uint8(): Int {
    return if (this >= 127) {
        this - 127
    } else
        127 - this
}

val Boolean.int
    get() = if (this) 1 else 0

fun Int.mhz(): Int = this * 1000 * 1000

fun Int.khz(): Int = this * 1000

fun Double.precision(decimals: Int = 2): Double = "%.${decimals}f".format(this).toDouble()
fun String.precision(decimals: Int = 2): String = "%.${decimals}s".format(this)





