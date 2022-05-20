package com.virginiaprivacy.sdr

sealed class FrequencyRange(
    val minFrequency: Long,
    val maxFrequency: Long,
    internal val mOpenDrain: Int,
    private val mRFMux_PolyMux: Int,
    private val mTF_c: Int,
    private val mXtalCap20p: Int,
    private val mXtalCap10p: Int
) {

    operator fun contains(frequency: Long): Boolean {
        return frequency in minFrequency..maxFrequency
    }

    val openDrain: Byte
        get() = mOpenDrain.toByte()
    val rFMuxPolyMux: Byte
        get() = mRFMux_PolyMux.toByte()
    val tFC: Byte
        get() = mTF_c.toByte()
    val xTALCap20P: Byte
        get() = mXtalCap20p.toByte()
    val xTALCap10P: Byte
        get() = mXtalCap10p.toByte()
    val xTALLowCap0P: Byte
        get() = 8
    val xTALHighCap0P: Byte
        get() = 0

    companion object {
        fun getRangeForFrequency(frequency: Long): FrequencyRange {
            var var5: Array<FrequencyRange>
            val var4: Int = values().also { var5 = it }.size
            for (var3 in 0 until var4) {
                val range = var5[var3]
                if (range.contains(frequency)) {
                    return range
                }
            }
            return RANGE_UNK
        }

        fun values(): Array<FrequencyRange> {
            return arrayOf(
                RANGE_024,
                RANGE_050,
                RANGE_055,
                RANGE_060,
                RANGE_065,
                RANGE_070,
                RANGE_075,
                RANGE_080,
                RANGE_090,
                RANGE_100,
                RANGE_110,
                RANGE_120,
                RANGE_140,
                RANGE_180,
                RANGE_220,
                RANGE_250,
                RANGE_280,
                RANGE_310,
                RANGE_450,
                RANGE_588,
                RANGE_650,
                RANGE_UNK
            )
        }

        fun valueOf(value: String): FrequencyRange {
            return when (value) {
                "RANGE_024" -> RANGE_024
                "RANGE_050" -> RANGE_050
                "RANGE_055" -> RANGE_055
                "RANGE_060" -> RANGE_060
                "RANGE_065" -> RANGE_065
                "RANGE_070" -> RANGE_070
                "RANGE_075" -> RANGE_075
                "RANGE_080" -> RANGE_080
                "RANGE_090" -> RANGE_090
                "RANGE_100" -> RANGE_100
                "RANGE_110" -> RANGE_110
                "RANGE_120" -> RANGE_120
                "RANGE_140" -> RANGE_140
                "RANGE_180" -> RANGE_180
                "RANGE_220" -> RANGE_220
                "RANGE_250" -> RANGE_250
                "RANGE_280" -> RANGE_280
                "RANGE_310" -> RANGE_310
                "RANGE_450" -> RANGE_450
                "RANGE_588" -> RANGE_588
                "RANGE_650" -> RANGE_650
                "RANGE_UNK" -> RANGE_UNK
                else -> throw IllegalArgumentException("No object com.virginiaprivacy.sdr.FrequencyRange.$value")
            }
        }
    }

    object RANGE_024 : FrequencyRange(24000000L, 49999999L, 8, 2, 223, 2, 1)
    object RANGE_050 : FrequencyRange(50000000L, 54999999L, 8, 2, 190, 2, 1)
    object RANGE_055 : FrequencyRange(
        55000000L,
        59999999L,
        8,
        2,
        139,
        2,
        1
    )

    object RANGE_060 : FrequencyRange(60000000L, 64999999L, 8, 2, 123, 2, 1)
    object RANGE_065 : FrequencyRange(65000000L, 69999999L, 8, 2, 105, 2, 1)
    object RANGE_070 : FrequencyRange(
        70000000L,
        74999999L,
        8,
        2,
        88,
        2,
        1
    )

    object RANGE_075 : FrequencyRange(75000000L, 79999999L, 0, 2, 68, 2, 1)
    object RANGE_080 : FrequencyRange(80000000L, 89999999L, 0, 2, 68, 2, 1)
    object RANGE_090 : FrequencyRange(
        90000000L,
        99999999L,
        0,
        2,
        52,
        1,
        1
    )

    object RANGE_100 : FrequencyRange(100000000L, 109999999L, 0, 2, 52, 1, 1)
    object RANGE_110 : FrequencyRange(110000000L, 119999999L, 0, 2, 36, 1, 1)
    object RANGE_120 : FrequencyRange(
        120000000L,
        139999999L,
        0,
        2,
        36,
        1,
        1
    )

    object RANGE_140 : FrequencyRange(140000000L, 179999999L, 0, 2, 20, 1, 1)
    object RANGE_180 : FrequencyRange(180000000L, 219999999L, 0, 2, 19, 0, 0)
    object RANGE_220 : FrequencyRange(
        220000000L,
        249999999L,
        0,
        2,
        19,
        0,
        0
    )

    object RANGE_250 : FrequencyRange(250000000L, 279999999L, 0, 2, 17, 0, 0)
    object RANGE_280 : FrequencyRange(280000000L, 309999999L, 0, 2, 0, 0, 0)
    object RANGE_310 : FrequencyRange(
        310000000L,
        449999999L,
        0,
        65,
        0,
        0,
        0
    )

    object RANGE_450 : FrequencyRange(450000000L, 587999999L, 0, 65, 0, 0, 0)
    object RANGE_588 : FrequencyRange(588000000L, 649999999L, 0, 64, 0, 0, 0)
    object RANGE_650 : FrequencyRange(
        650000000L,
        1766000000L,
        0,
        64,
        0,
        0,
        0
    )

    object RANGE_UNK : FrequencyRange(0L, 0L, 0, 0, 0, 0, 0)
}