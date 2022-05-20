package com.virginiaprivacy.sdr.tuner.r820t

enum class R820TLNAGain(private val mLabel: String, val mSetting: Int) {
    AUTOMATIC("Automatic", 0), GAIN_0("0", 16), GAIN_9("9", 17), GAIN_21("21", 18), GAIN_61("61", 19), GAIN_99(
        "99",
        20
    ),
    GAIN_112("112", 21), GAIN_143("143", 22), GAIN_165("165", 23), GAIN_191("191", 24), GAIN_222(
        "222",
        25
    ),
    GAIN_248("248", 26), GAIN_262("262", 27), GAIN_281("281", 28), GAIN_286("286", 29), GAIN_321(
        "321",
        30
    ),
    GAIN_334("334", 31);

    override fun toString(): String {
        return mLabel
    }

    val setting: Byte
        get() = mSetting.toByte()
}