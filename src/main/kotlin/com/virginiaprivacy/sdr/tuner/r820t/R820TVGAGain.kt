package com.virginiaprivacy.sdr.tuner.r820t

sealed class R820TVGAGain(private val mLabel: String, val mSetting: Int) {

    override fun toString(): String {
        return mLabel
    }

    val setting: Byte
        get() = mSetting.toByte()

    object GAIN_0 : R820TVGAGain("0", 0)
    object GAIN_26 : R820TVGAGain("26", 1)
    object GAIN_52 : R820TVGAGain("52", 2)
    object GAIN_82 : R820TVGAGain("82", 3)
    object GAIN_124 : R820TVGAGain("124", 4)
    object GAIN_159 : R820TVGAGain(
        "159",
        5
    )

    object GAIN_183 : R820TVGAGain("183", 6)
    object GAIN_196 : R820TVGAGain("196", 7)
    object GAIN_210 : R820TVGAGain("210", 8)
    object GAIN_242 : R820TVGAGain("242", 9)
    object GAIN_278 : R820TVGAGain(
        "278",
        10
    )

    object GAIN_312 : R820TVGAGain("312", 11)
    object GAIN_347 : R820TVGAGain("347", 12)
    object GAIN_384 : R820TVGAGain("384", 13)
    object GAIN_419 : R820TVGAGain("419", 14)
    object GAIN_455 : R820TVGAGain("455", 15)
    companion object {
        fun values(): Array<R820TVGAGain> {
            return arrayOf(
                GAIN_0,
                GAIN_26,
                GAIN_52,
                GAIN_82,
                GAIN_124,
                GAIN_159,
                GAIN_183,
                GAIN_196,
                GAIN_210,
                GAIN_242,
                GAIN_278,
                GAIN_312,
                GAIN_347,
                GAIN_384,
                GAIN_419,
                GAIN_455
            )
        }

        fun valueOf(value: String): R820TVGAGain {
            return when (value) {
                "GAIN_0" -> GAIN_0
                "GAIN_26" -> GAIN_26
                "GAIN_52" -> GAIN_52
                "GAIN_82" -> GAIN_82
                "GAIN_124" -> GAIN_124
                "GAIN_159" -> GAIN_159
                "GAIN_183" -> GAIN_183
                "GAIN_196" -> GAIN_196
                "GAIN_210" -> GAIN_210
                "GAIN_242" -> GAIN_242
                "GAIN_278" -> GAIN_278
                "GAIN_312" -> GAIN_312
                "GAIN_347" -> GAIN_347
                "GAIN_384" -> GAIN_384
                "GAIN_419" -> GAIN_419
                "GAIN_455" -> GAIN_455
                else -> throw IllegalArgumentException("No object com.virginiaprivacy.sdr.tuner.r820t.R820TVGAGain.$value")
            }
        }
    }
}