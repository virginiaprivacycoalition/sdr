package com.virginiaprivacy.sdr.tuner.r820t

enum class R820TMixerGain(private val mLabel: String, val mSetting: Int) {
    AUTOMATIC("Automatic", 16), GAIN_0("0", 0), GAIN_5("5", 1), GAIN_15("15", 2), GAIN_25("25", 3), GAIN_44(
        "44",
        4
    ),
    GAIN_53("53", 5), GAIN_63("63", 6), GAIN_88("88", 7), GAIN_105("105", 8), GAIN_115("115", 9), GAIN_123(
        "123",
        10
    ),
    GAIN_139("139", 11), GAIN_152("152", 12), GAIN_158("158", 13), GAIN_161("161", 14), GAIN_153("153", 15);

    override fun toString(): String {
        return mLabel
    }

    val setting: Byte
        get() = mSetting.toByte()
}