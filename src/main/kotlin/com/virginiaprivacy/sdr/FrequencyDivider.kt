package com.virginiaprivacy.sdr

enum class FrequencyDivider(
    private val mDividerNumber: Int,
    val mixerDivider: Int,
    private val minimumFrequency: Long,
    val maximumFrequency: Long,
    private val mRegisterSetting: Int,
    private val mIntegralValue: Int
) {
    DIVIDER_0(0, 2, 864000000L, 1785600000L, 0, 28800000), DIVIDER_1(
        1,
        4,
        432000000L,
        892800000L,
        32,
        14400000
    ),
    DIVIDER_2(2, 8, 216000000L, 460800000L, 64, 7200000), DIVIDER_3(
        3,
        16,
        108000000L,
        223200000L,
        96,
        3600000
    ),
    DIVIDER_4(4, 32, 54000000L, 111600000L, 128, 1800000), DIVIDER_5(
        5,
        64,
        27000000L,
        55800000L,
        160,
        900000
    ),
    DIVIDER_6(6, 128, 13500000L, 27900000L, 192, 450000), DIVIDER_7(7, 256, 6750000L, 13950000L, 224, 225000);

    fun getDividerNumber(vcoFineTune: Int): Int {
        return if (vcoFineTune == 2) {
            mDividerNumber
        } else if (vcoFineTune < 2) {
            mDividerNumber - 1
        } else {
            if (vcoFineTune > 2) mDividerNumber + 1 else mDividerNumber
        }
    }

    val dividerRegisterSetting: Byte
        get() = mRegisterSetting.toByte()

    operator fun contains(frequency: Long): Boolean {
        return frequency in minimumFrequency..maximumFrequency
    }

    fun getIntegral(frequency: Long): Integral {
        return if (contains(frequency)) {
            val delta = (frequency - minimumFrequency).toInt()
            val integral = (delta.toDouble() / mIntegralValue.toDouble()).toInt()
            Integral.fromValue(integral)
        } else {
            throw IllegalArgumentException("PLL frequency [$frequency] is not valid for this frequency divider $this")
        }
    }

    fun getSDM(integral: Integral, frequency: Long): Int {
        return if (contains(frequency)) {
            val delta = (frequency - minimumFrequency - (integral.number * mIntegralValue).toLong()).toInt()
            val fractional = delta.toDouble() / mIntegralValue.toDouble()
            (fractional * 65536.0).toInt() and '\uffff'.code
        } else {
            0
        }
    }

    companion object {
        private const val mVCOPowerReference = 2
        fun fromFrequency(frequency: Long): FrequencyDivider {
            var var5: Array<FrequencyDivider>
            val var4: Int = values().also { var5 = it }.size
            for (var3 in 0 until var4) {
                val divider = var5[var3]
                if (divider.contains(frequency)) {
                    return divider
                }
            }
            return DIVIDER_5
        }
    }
}