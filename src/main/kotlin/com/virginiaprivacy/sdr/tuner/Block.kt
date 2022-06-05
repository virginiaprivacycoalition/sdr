package com.virginiaprivacy.sdr.tuner

sealed class Block(val value: Int) {

    val readIndex: Short
        get() = Integer.rotateLeft(value, 8).toShort()
    val writeIndex: Short
        get() = (readIndex.toInt() or 16).toShort()

    object DEMOD : Block(0)
    object USB : Block(1)
    object SYS : Block(2)
    object TUN : Block(3)
    object ROM : Block(4)  // $FF: renamed from: IR com.g0kla.rtlsdr4java.RTL2832TunerController$Block
    object FIELD_2 : Block(5)
    object I2C : Block(6)
    companion object {
        fun values(): Array<Block> {
            return arrayOf(DEMOD, USB, SYS, TUN, ROM, FIELD_2, I2C)
        }

        fun valueOf(value: String): Block {
            return when (value) {
                "DEMOD" -> DEMOD
                "USB" -> USB
                "SYS" -> SYS
                "TUN" -> TUN
                "ROM" -> ROM
                "FIELD_2" -> FIELD_2
                "I2C" -> I2C
                else -> throw IllegalArgumentException("No object com.virginiaprivacy.sdr.Block.$value")
            }
        }
    }
}