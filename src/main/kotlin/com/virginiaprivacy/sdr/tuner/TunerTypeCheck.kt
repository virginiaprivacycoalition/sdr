package com.virginiaprivacy.sdr.tuner

sealed class TunerTypeCheck(
    private val mI2CAddress: Int,
    private val mCheckAddress: Int,
    private val mCheckValue: Int,
    val tunerType: TunerType
) {

    val i2CAddress: Byte
        get() = mI2CAddress.toByte()
    val checkAddress: Byte
        get() = mCheckAddress.toByte()
    val checkValue: Byte
        get() = mCheckValue.toByte()

    object E4K : TunerTypeCheck(200, 2, 64, TunerType.ELONICS_E4000)
    object FC0012 : TunerTypeCheck(198, 0, 161, TunerType.FITIPOWER_FC0012)
    object FC0013 : TunerTypeCheck(198, 0, 163, TunerType.FITIPOWER_FC0013)
    object FC2580 : TunerTypeCheck(172, 1, 86, TunerType.FCI_FC2580)
    object R820T : TunerTypeCheck(52, 0, 105, TunerType.RAFAELMICRO_R820T)
    object R828D : TunerTypeCheck(116, 0,        105, TunerType.RAFAELMICRO_R828D)

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