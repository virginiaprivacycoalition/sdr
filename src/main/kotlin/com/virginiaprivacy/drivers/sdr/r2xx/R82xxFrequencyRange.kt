package com.virginiaprivacy.drivers.sdr.r2xx

data class R82xxFrequencyRange(
    val freq: Int,
    val openD: Int,
    val rfMuxPloy: Int,
    val tfC: Int,
    val xtalCap20p: Int,
    val xtalCap10p: Int,
    val xtalCap0p: Int,
)