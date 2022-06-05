package com.virginiaprivacy.sdr.tuner.e4k

enum class MixerFilter(
    val mValue: Int,
    val bandwidth: Int,
    val minimumBandwidth: Int,
    val maximumBandwidth: Int,
    val label: String
) {
    BW_27M0(0, 27000000, 4800000, 28800000, "27.0 MHz"), BW_4M6(128, 4600000, 4400000, 4800000, "4.6 MHz"), BW_4M2(
        144,
        4200000,
        4000000,
        4400000,
        "4.2 MHz"
    ),
    BW_3M8(160, 3800000, 3600000, 4000000, "3.8 MHz"), BW_3M4(
        176,
        3400000,
        3200000,
        3600000,
        "3.4 MHz"
    ),
    BW_3M0(192, 3000000, 2850000, 3200000, "3.0 MHz"), BW_2M7(
        208,
        2700000,
        2500000,
        2850000,
        "2.7 MHz"
    ),
    BW_2M3(224, 2300000, 2100000, 2500000, "2.3 MHz"), BW_1M9(240, 1900000, 0, 2100000, "1.9 MHz");

    val value: Byte
        get() = mValue.toByte()

    companion object {
        val register: E4KRegister
            get() = E4KRegister.FILT2
        val mask: Byte
            get() = -16

        fun getFilter(bandwidth: Int): MixerFilter {
            var var4: Array<MixerFilter>
            val var3: Int = values().also { var4 = it }.size
            for (var2 in 0 until var3) {
                val filter = var4[var2]
                if (filter.minimumBandwidth <= bandwidth && bandwidth < filter.maximumBandwidth) {
                    return filter
                }
            }
            return BW_27M0
        }

        fun fromRegisterValue(registerValue: Int): MixerFilter {
            val value = registerValue and mask.toInt()
            var var5: Array<MixerFilter>
            val var4: Int = values().also { var5 = it }.size
            for (var3 in 0 until var4) {
                val filter = var5[var3]
                if (value == filter.mValue) {
                    return filter
                }
            }
            throw IllegalArgumentException("E4KTunerController - unrecognized mixer filter value [$value]")
        }
    }
}