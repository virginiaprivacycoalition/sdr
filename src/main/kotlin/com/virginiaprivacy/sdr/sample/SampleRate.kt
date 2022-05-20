package com.virginiaprivacy.sdr.sample

import com.virginiaprivacy.sdr.tuner.RTL2832TunerController

enum class SampleRate(val ratioHighBits: Int, val rate: Int, val label: String) {
    RATE_0_240MHZ(7680, 240000, "0.240 MHz"), RATE_0_288MHZ(6400, 288000, "0.288 MHz"), RATE_0_960MHZ(
        1920,
        960000,
        "0.960 MHz"
    ),
    RATE_1_200MHZ(1536, 1200000, "1.200 MHz"), RATE_1_440MHZ(1280, 1440000, "1.440 MHz"), RATE_1_920MHZ(
        960,
        2016000,
        "2.016 MHz"
    ),
    RATE_2_304MHZ(800, 2208000, "2.208 MHz"), RATE_2_400MHZ(768, 2400000, "2.400 MHz"), RATE_2_880MHZ(
        640,
        2880000,
        "2.880 MHz"
    );

    override fun toString(): String {
        return label
    }

    companion object {
        fun getClosest(sampleRate: Int): SampleRate {
            var var4: Array<SampleRate>
            val var3: Int = values().also { var4 = it }.size
            for (var2 in 0 until var3) {
                val rate = var4[var2]
                if (rate.rate >= sampleRate) {
                    return rate
                }
            }
            return RTL2832TunerController.DEFAULT_SAMPLE_RATE
        }
    }
}