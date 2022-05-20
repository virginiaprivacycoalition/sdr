package com.virginiaprivacy.sdr.tuner

sealed class TunerTypeCheck(
    private val mI2CAddress: Int,
    private val mCheckAddress: Int,
    private val mCheckValue: Int
) {

    val i2CAddress: Byte
        get() = mI2CAddress.toByte()
    val checkAddress: Byte
        get() = mCheckAddress.toByte()
    val checkValue: Byte
        get() = mCheckValue.toByte()

    object E4K : TunerTypeCheck(200, 2, 64)
    object FC0012 : TunerTypeCheck(198, 0, 161)
    object FC0013 : TunerTypeCheck(198, 0, 163)
    object FC2580 : TunerTypeCheck(172, 1, 86)
    object R820T : TunerTypeCheck(52, 0, 105)
    object R828D : TunerTypeCheck(
        116,
        0,
        105
    )

    companion object {
        fun values(): Array<TunerTypeCheck> {
            return arrayOf(E4K, FC0012, FC0013, FC2580, R820T, R828D)
        }

        fun valueOf(value: String): TunerTypeCheck {
            return when (value) {
                "E4K" -> E4K
                "FC0012" -> FC0012
                "FC0013" -> FC0013
                "FC2580" -> FC2580
                "R820T" -> R820T
                "R828D" -> R828D
                else -> throw IllegalArgumentException("No object com.virginiaprivacy.sdr.TunerTypeCheck.$value")
            }
        }
    }
}