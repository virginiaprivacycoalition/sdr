package com.virginiaprivacy.sdr.tuner.e4k

enum class IFStage3Gain(val mValue: Int, val label: String) {
    GAIN_PLUS0(0, "0 db"), GAIN_PLUS3(8, "3 db"), GAIN_PLUS6(16, "6 db"), GAIN_PLUS9(48, "9 db");

    val value: Byte
        get() = mValue.toByte()

    override fun toString(): String {
        return label
    }

    companion object {
        val register: E4KRegister
            get() = E4KRegister.GAIN3
        val mask: Byte
            get() = 24

        fun fromRegisterValue(registerValue: Int): IFStage3Gain {
            val value = registerValue and mask.toInt()
            var var5: Array<IFStage3Gain>
            val var4: Int = values().also { var5 = it }.size
            for (var3 in 0 until var4) {
                val setting = var5[var3]
                if (value == setting.mValue) {
                    return setting
                }
            }
            throw IllegalArgumentException("E4KTunerController - unrecognized IF Gain Stage 3 value [$value]")
        }
    }
}