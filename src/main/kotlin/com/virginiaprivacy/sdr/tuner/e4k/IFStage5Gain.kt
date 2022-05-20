package com.virginiaprivacy.sdr.tuner.e4k

enum class IFStage5Gain(val mValue: Int, val label: String) {
    GAIN_PLUS3(0, "3 db"), GAIN_PLUS6(1, "6 db"), GAIN_PLUS9(2, "9 db"), GAIN_PLUS12(3, "12 db"), GAIN_PLUS15A(
        4,
        "15 db"
    ),
    GAIN_PLUS15B(5, "15 db"), GAIN_PLUS15C(6, "15 db"), GAIN_PLUS15D(7, "15 db");

    val value: Byte
        get() = mValue.toByte()

    override fun toString(): String {
        return label
    }

    companion object {
        val register: E4KRegister
            get() = E4KRegister.GAIN4
        val mask: Byte
            get() = 7

        fun fromRegisterValue(registerValue: Int): IFStage5Gain {
            val value = registerValue and mask.toInt()
            var var5: Array<IFStage5Gain>
            val var4: Int = values().also { var5 = it }.size
            for (var3 in 0 until var4) {
                val setting = var5[var3]
                if (value == setting.mValue) {
                    return setting
                }
            }
            throw IllegalArgumentException("E4KTunerController - unrecognized IF Gain Stage 5 value [$value]")
        }
    }
}