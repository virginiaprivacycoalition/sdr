package com.virginiaprivacy.sdr.tuner.r820t

enum class R820TVGAGain(private val mLabel: String, val mSetting: Int) {
    GAIN_0("0", 0), GAIN_26("26", 1), GAIN_52("52", 2), GAIN_82("82", 3), GAIN_124("124", 4), GAIN_159(
        "159",
        5
    ),
    GAIN_183("183", 6), GAIN_196("196", 7), GAIN_210("210", 8), GAIN_242("242", 9), GAIN_278(
        "278",
        10
    ),
    GAIN_312("312", 11), GAIN_347("347", 12), GAIN_384("384", 13), GAIN_419("419", 14), GAIN_455("455", 15);

    override fun toString(): String {
        return mLabel
    }

    val setting: Byte
        get() = mSetting.toByte()
}