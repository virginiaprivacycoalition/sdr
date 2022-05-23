package com.virginiaprivacy.sdr.sample

import com.virginiaprivacy.sdr.tuner.RTL2832TunerController

sealed class SampleRate(val ratioHighBits: Int, val rate: Int, val label: String) {

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

        fun values(): Array<SampleRate> {
            return arrayOf(
                RATE_0_240MHZ,
                RATE_0_288MHZ,
                RATE_0_960MHZ,
                RATE_1_200MHZ,
                RATE_1_440MHZ,
                RATE_1_920MHZ,
                RATE_2_304MHZ,
                RATE_2_400MHZ,
                RATE_2_880MHZ
            )
        }

        fun valueOf(value: String): SampleRate {
            return when (value) {
                "RATE_0_240MHZ" -> RATE_0_240MHZ
                "RATE_0_288MHZ" -> RATE_0_288MHZ
                "RATE_0_960MHZ" -> RATE_0_960MHZ
                "RATE_1_200MHZ" -> RATE_1_200MHZ
                "RATE_1_440MHZ" -> RATE_1_440MHZ
                "RATE_1_920MHZ" -> RATE_1_920MHZ
                "RATE_2_304MHZ" -> RATE_2_304MHZ
                "RATE_2_400MHZ" -> RATE_2_400MHZ
                "RATE_2_880MHZ" -> RATE_2_880MHZ
                else -> throw IllegalArgumentException("No object com.virginiaprivacy.sdr.sample.SampleRate.$value")
            }
        }
    }

    object RATE_0_240MHZ : SampleRate(7680, 240000, "0.240 MHz")
    object RATE_0_288MHZ : SampleRate(6400, 288000, "0.288 MHz")
    object RATE_0_960MHZ : SampleRate(
        1920,
        960000,
        "0.960 MHz"
    )

    object RATE_1_200MHZ : SampleRate(1536, 1200000, "1.200 MHz")
    object RATE_1_440MHZ : SampleRate(1280, 1440000, "1.440 MHz")
    object RATE_1_920MHZ : SampleRate(
        960,
        2016000,
        "2.016 MHz"
    )

    object RATE_2_304MHZ : SampleRate(800, 2208000, "2.208 MHz")
    object RATE_2_400MHZ : SampleRate(768, 2400000, "2.400 MHz")
    object RATE_2_880MHZ : SampleRate(
        640,
        2880000,
        "2.880 MHz"
    )

}