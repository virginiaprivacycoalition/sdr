package com.virginiaprivacy.sdr.tuner.r820t

enum class R820TGain(
    private val mLabel: String,
    val vGAGain: R820TVGAGain,
    val lNAGain: R820TLNAGain,
    val mixerGain: R820TMixerGain
) {
    AUTOMATIC(
        "Automatic",
        R820TVGAGain.GAIN_312,
        R820TLNAGain.AUTOMATIC,
        R820TMixerGain.AUTOMATIC
    ),
    MANUAL("Manual",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_248,
        R820TMixerGain.GAIN_123
    ), GAIN_0(
        "0",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_0,
        R820TMixerGain.GAIN_0
    ),
    GAIN_9("9",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_9,
        R820TMixerGain.GAIN_0
    ), GAIN_14(
        "14",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_9,
        R820TMixerGain.GAIN_5
    ),
    GAIN_26("26",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_21,
        R820TMixerGain.GAIN_5
    ), GAIN_36(
        "36",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_21,
        R820TMixerGain.GAIN_15
    ),
    GAIN_76("76",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_61,
        R820TMixerGain.GAIN_15
    ), GAIN_86(
        "86",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_61,
        R820TMixerGain.GAIN_25
    ),
    GAIN_124("124",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_99,
        R820TMixerGain.GAIN_25
    ), GAIN_143(
        "143",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_99,
        R820TMixerGain.GAIN_44
    ),
    GAIN_156("156",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_112,
        R820TMixerGain.GAIN_44
    ), GAIN_165(
        "165",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_112,
        R820TMixerGain.GAIN_53
    ),
    GAIN_196("196",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_143,
        R820TMixerGain.GAIN_53
    ), GAIN_208(
        "208",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_143,
        R820TMixerGain.GAIN_63
    ),
    GAIN_228("228",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_165,
        R820TMixerGain.GAIN_63
    ), GAIN_253(
        "253",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_165,
        R820TMixerGain.GAIN_88
    ),
    GAIN_279("279",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_191,
        R820TMixerGain.GAIN_88
    ), GAIN_296(
        "296",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_191,
        R820TMixerGain.GAIN_105
    ),
    GAIN_327("327",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_222,
        R820TMixerGain.GAIN_105
    ), GAIN_337(
        "337",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_222,
        R820TMixerGain.GAIN_115
    ),
    GAIN_363("363",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_248,
        R820TMixerGain.GAIN_115
    ), GAIN_371(
        "371",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_248,
        R820TMixerGain.GAIN_123
    ),
    GAIN_385("385",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_262,
        R820TMixerGain.GAIN_123
    ), GAIN_401(
        "401",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_262,
        R820TMixerGain.GAIN_139
    ),
    GAIN_420("420",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_281,
        R820TMixerGain.GAIN_139
    ), GAIN_433(
        "433",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_281,
        R820TMixerGain.GAIN_152
    ),
    GAIN_438("438",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_286,
        R820TMixerGain.GAIN_152
    ), GAIN_444(
        "444",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_286,
        R820TMixerGain.GAIN_158
    ),
    GAIN_479("479",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_321,
        R820TMixerGain.GAIN_158
    ), GAIN_482(
        "482",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_321,
        R820TMixerGain.GAIN_161
    ),
    GAIN_495("495",
        R820TVGAGain.GAIN_210,
        R820TLNAGain.GAIN_334,
        R820TMixerGain.GAIN_161
    );

    override fun toString(): String {
        return mLabel
    }
}