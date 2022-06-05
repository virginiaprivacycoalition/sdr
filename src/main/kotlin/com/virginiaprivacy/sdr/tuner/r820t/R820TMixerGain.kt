package com.virginiaprivacy.sdr.tuner.r820t

sealed class R820TMixerGain(private val mLabel: String, val mSetting: Int) {

    override fun toString(): String {
        return mLabel
    }

    val setting: Byte
        get() = mSetting.toByte()

    object AUTOMATIC : R820TMixerGain("Automatic", 16)
    object GAIN_0 : R820TMixerGain("0", 0)
    object GAIN_5 : R820TMixerGain("5", 1)
    object GAIN_15 : R820TMixerGain("15", 2)
    object GAIN_25 : R820TMixerGain("25", 3)
    object GAIN_44 : R820TMixerGain(
        "44",
        4
    )

    object GAIN_53 : R820TMixerGain("53", 5)
    object GAIN_63 : R820TMixerGain("63", 6)
    object GAIN_88 : R820TMixerGain("88", 7)
    object GAIN_105 : R820TMixerGain("105", 8)
    object GAIN_115 : R820TMixerGain("115", 9)
    object GAIN_123 : R820TMixerGain(
        "123",
        10
    )

    object GAIN_139 : R820TMixerGain("139", 11)
    object GAIN_152 : R820TMixerGain("152", 12)
    object GAIN_158 : R820TMixerGain("158", 13)
    object GAIN_161 : R820TMixerGain("161", 14)
    object GAIN_153 : R820TMixerGain("153", 15)
    companion object {
        fun values(): Array<R820TMixerGain> {
            return arrayOf(
                AUTOMATIC,
                GAIN_0,
                GAIN_5,
                GAIN_15,
                GAIN_25,
                GAIN_44,
                GAIN_53,
                GAIN_63,
                GAIN_88,
                GAIN_105,
                GAIN_115,
                GAIN_123,
                GAIN_139,
                GAIN_152,
                GAIN_158,
                GAIN_161,
                GAIN_153
            )
        }

        fun valueOf(value: String): R820TMixerGain {
            return when (value) {
                "AUTOMATIC" -> AUTOMATIC
                "GAIN_0" -> GAIN_0
                "GAIN_5" -> GAIN_5
                "GAIN_15" -> GAIN_15
                "GAIN_25" -> GAIN_25
                "GAIN_44" -> GAIN_44
                "GAIN_53" -> GAIN_53
                "GAIN_63" -> GAIN_63
                "GAIN_88" -> GAIN_88
                "GAIN_105" -> GAIN_105
                "GAIN_115" -> GAIN_115
                "GAIN_123" -> GAIN_123
                "GAIN_139" -> GAIN_139
                "GAIN_152" -> GAIN_152
                "GAIN_158" -> GAIN_158
                "GAIN_161" -> GAIN_161
                "GAIN_153" -> GAIN_153
                else -> throw IllegalArgumentException("No object com.virginiaprivacy.sdr.tuner.r820t.R820TMixerGain.$value")
            }
        }
    }
}