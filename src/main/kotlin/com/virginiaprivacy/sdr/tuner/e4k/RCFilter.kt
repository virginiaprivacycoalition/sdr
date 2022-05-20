package com.virginiaprivacy.sdr.tuner.e4k

enum class RCFilter(
    val mValue: Int,
    val bandwidth: Int,
    val minimumBandwidth: Int,
    val maximumBandwidth: Int,
    val label: String
) {
    BW_21M4(0, 21400000, 21200000, 28800000, "21.4 MHz"), BW_21M0(
        1,
        21000000,
        19300000,
        21200000,
        "21.0 MHz"
    ),
    BW_17M6(2, 17600000, 16150000, 19300000, "17.6 MHz"), BW_14M7(
        3,
        14700000,
        13550000,
        16150000,
        "14.7 MHz"
    ),
    BW_12M4(4, 12400000, 11500000, 13550000, "12.4 MHz"), BW_10M6(
        5,
        10600000,
        9800000,
        11500000,
        "10.6 MHz"
    ),
    BW_9M0(6, 9000000, 8350000, 9800000, "9.0 MHz"), BW_7M7(7, 7700000, 7050000, 8350000, "7.7 MHz"), BW_6M4(
        8,
        6400000,
        5805000,
        7050000,
        "6.4 MHz"
    ),
    BW_5M3(9, 5300000, 4850000, 5805000, "5.3 MHz"), BW_4M4(10, 4400000, 3900000, 4850000, "4.4 MHz"), BW_3M4(
        11,
        3400000,
        3000000,
        3900000,
        "3.4 MHz"
    ),
    BW_2M6(12, 2600000, 2200000, 3000000, "2.6 MHz"), BW_1M8(13, 1800000, 1500000, 2200000, "1.8 MHz"), BW_1M2(
        14,
        1200000,
        1100000,
        1500000,
        "1.2 MHz"
    ),
    BW_1M0(15, 1000000, 0, 1100000, "1.0 MHz");

    val value: Byte
        get() = mValue.toByte()

    companion object {
        val register: E4KRegister
            get() = E4KRegister.FILT2
        val mask: Byte
            get() = 15

        fun getFilter(bandwidth: Int): RCFilter {
            var var4: Array<RCFilter>
            val var3: Int = values().also { var4 = it }.size
            for (var2 in 0 until var3) {
                val filter = var4[var2]
                if (filter.minimumBandwidth <= bandwidth && bandwidth < filter.maximumBandwidth) {
                    return filter
                }
            }
            return BW_21M4
        }

        fun fromRegisterValue(registerValue: Int): RCFilter {
            val value = registerValue and mask.toInt()
            var var5: Array<RCFilter>
            val var4: Int = values().also { var5 = it }.size
            for (var3 in 0 until var4) {
                val filter = var5[var3]
                if (value == filter.mValue) {
                    return filter
                }
            }
            throw IllegalArgumentException("E4KTunerController - unrecognized rc filter value [$value]")
        }
    }
}