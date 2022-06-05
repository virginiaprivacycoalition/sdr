package com.virginiaprivacy.sdr.tuner.r820t

sealed class R820TLNAGain(private val mLabel: String, val mSetting: Int) {

    override fun toString(): String {
        return mLabel
    }

    val setting: Byte
        get() = mSetting.toByte()

    object AUTOMATIC : R820TLNAGain("Automatic", 0)
    object GAIN_0 : R820TLNAGain("0", 16)
    object GAIN_9 : R820TLNAGain("9", 17)
    object GAIN_21 : R820TLNAGain("21", 18)
    object GAIN_61 : R820TLNAGain("61", 19)
    object GAIN_99 : R820TLNAGain(
        "99",
        20
    )

    object GAIN_112 : R820TLNAGain("112", 21)
    object GAIN_143 : R820TLNAGain("143", 22)
    object GAIN_165 : R820TLNAGain("165", 23)
    object GAIN_191 : R820TLNAGain("191", 24)
    object GAIN_222 : R820TLNAGain(
        "222",
        25
    )

    object GAIN_248 : R820TLNAGain("248", 26)
    object GAIN_262 : R820TLNAGain("262", 27)
    object GAIN_281 : R820TLNAGain("281", 28)
    object GAIN_286 : R820TLNAGain("286", 29)
    object GAIN_321 : R820TLNAGain(
        "321",
        30
    )

    object GAIN_334 : R820TLNAGain("334", 31)
    companion object {
        fun values(): Array<R820TLNAGain> {
            return arrayOf(
                AUTOMATIC,
                GAIN_0,
                GAIN_9,
                GAIN_21,
                GAIN_61,
                GAIN_99,
                GAIN_112,
                GAIN_143,
                GAIN_165,
                GAIN_191,
                GAIN_222,
                GAIN_248,
                GAIN_262,
                GAIN_281,
                GAIN_286,
                GAIN_321,
                GAIN_334
            )
        }

        fun valueOf(value: String): R820TLNAGain {
            return when (value) {
                "AUTOMATIC" -> AUTOMATIC
                "GAIN_0" -> GAIN_0
                "GAIN_9" -> GAIN_9
                "GAIN_21" -> GAIN_21
                "GAIN_61" -> GAIN_61
                "GAIN_99" -> GAIN_99
                "GAIN_112" -> GAIN_112
                "GAIN_143" -> GAIN_143
                "GAIN_165" -> GAIN_165
                "GAIN_191" -> GAIN_191
                "GAIN_222" -> GAIN_222
                "GAIN_248" -> GAIN_248
                "GAIN_262" -> GAIN_262
                "GAIN_281" -> GAIN_281
                "GAIN_286" -> GAIN_286
                "GAIN_321" -> GAIN_321
                "GAIN_334" -> GAIN_334
                else -> throw IllegalArgumentException("No object com.virginiaprivacy.sdr.tuner.r820t.R820TLNAGain.$value")
            }
        }
    }
}