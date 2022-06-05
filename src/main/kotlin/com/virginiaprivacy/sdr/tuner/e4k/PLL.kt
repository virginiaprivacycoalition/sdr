package com.virginiaprivacy.sdr.tuner.e4k

enum class PLL(
    private val mValue: Int,
    val frequency: Long,
    private val mMultiplier: Int,
    val scaledOscillator: Int,
    private val mRequires3PhaseMixing: Boolean,
    val label: String
) {
    PLL_72M4(15, 72400000L, 48, 600000, true, "72.4 MHz"), PLL_81M2(
        14,
        81200000L,
        40,
        720000,
        true,
        "81.2 MHz"
    ),
    PLL_108M3(13, 108300000L, 32, 900000, true, "108.3 MHz"), PLL_162M5(
        12,
        162500000L,
        24,
        1200000,
        true,
        "162.5 MHz"
    ),
    PLL_216M6(11, 216600000L, 16, 1800000, true, "216.6 MHz"), PLL_325M0(
        10,
        325000000L,
        12,
        2400000,
        true,
        "325.0 MHz"
    ),
    PLL_350M0(9, 350000000L, 8, 3600000, true, "350.0 MHz"), PLL_432M0(
        3,
        432000000L,
        8,
        3600000,
        false,
        "432.0 MHz"
    ),
    PLL_667M0(2, 667000000L, 6, 4800000, false, "667.0 MHz"), PLL_1200M0(
        1,
        1200000000L,
        4,
        7200000,
        false,
        "1200.0 MHz"
    );

    val index: Byte
        get() = mValue.toByte()
    val multiplier: Byte
        get() = mMultiplier.toByte()

    fun requires3PhaseMixing(): Boolean {
        return mRequires3PhaseMixing
    }

    override fun toString(): String {
        return label
    }

    companion object {
        fun fromFrequency(frequency: Long): PLL {
            var var5: Array<PLL>
            val var4: Int = values().also { var5 = it }.size
            for (var3 in 0 until var4) {
                val pll = var5[var3]
                if (frequency < pll.frequency) {
                    return pll
                }
            }
            return PLL_72M4
        }

        fun fromSetting(setting: Int): PLL {
            val value = setting and 15
            var var5: Array<PLL>
            val var4: Int = values().also { var5 = it }.size
            for (var3 in 0 until var4) {
                val pll = var5[var3]
                if (value == pll.index.toInt()) {
                    return pll
                }
            }
            return PLL_72M4
        }
    }
}