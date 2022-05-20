package com.virginiaprivacy.sdr.tuner.e4k

enum class RFFilter(val mValue: Int, val minimumFrequency: Long, val maximumFrequency: Long) {
    NO_FILTER(0, 0L, 0L), M360(0, 350000000L, 370000000L), M380(1, 370000000L, 392500000L), M405(
        2,
        392500000L,
        417500000L
    ),
    M425(3, 417500000L, 437500000L), M450(4, 437500000L, 462500000L), M475(5, 462500000L, 490000000L), M505(
        6,
        490000000L,
        522500000L
    ),
    M540(7, 522500000L, 557500000L), M575(8, 557500000L, 595000000L), M615(9, 595000000L, 642500000L), M670(
        10,
        642500000L,
        695000000L
    ),
    M720(11, 695000000L, 740000000L), M760(12, 740000000L, 800000000L), M840(13, 800000000L, 865000000L), M890(
        14,
        865000000L,
        930000000L
    ),
    M970(15, 930000000L, 1135000000L), M1300(0, 1135000000L, 1310000000L), M1320(
        1,
        1310000000L,
        1340000000L
    ),
    M1360(2, 1340000000L, 1385000000L), M1410(3, 1385000000L, 1427500000L), M1445(
        4,
        1427500000L,
        1452500000L
    ),
    M1460(5, 1452500000L, 1475000000L), M1490(6, 1475000000L, 1510000000L), M1530(
        7,
        1510000000L,
        1545000000L
    ),
    M1560(8, 1545000000L, 1575000000L), M1590(9, 1575000000L, 1615000000L), M1640(
        10,
        1615000000L,
        1650000000L
    ),
    M1660(11, 1650000000L, 1670000000L), M1680(12, 1670000000L, 1690000000L), M1700(
        13,
        1690000000L,
        1710000000L
    ),
    M1720(14, 1710000000L, 1735000000L), M1750(15, 1735000000L, 2147000000L);

    val value: Byte
        get() = mValue.toByte()

    companion object {
        val register: E4KRegister
            get() = E4KRegister.FILT1
        val mask: Byte
            get() = 15

        fun fromFrequency(frequency: Long): RFFilter {
            return if (frequency < 350000000L) {
                NO_FILTER
            } else {
                var var5: Array<RFFilter>
                val var4: Int = values().also { var5 = it }.size
                for (var3 in 0 until var4) {
                    val filter = var5[var3]
                    if (filter.minimumFrequency <= frequency && frequency < filter.maximumFrequency) {
                        return filter
                    }
                }
                throw IllegalArgumentException("E4KTunerController - cannot find RF filter for frequency [$frequency]")
            }
        }
    }
}