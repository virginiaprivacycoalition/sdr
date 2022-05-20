package com.virginiaprivacy.sdr.tuner.r820t

sealed class R820TRegister(val register: Int, val mMask: Int) {

    val mask: Byte
        get() = mMask.toByte()
    val isMasked: Boolean
        get() = mMask != 0

    object LNA_GAIN : R820TRegister(5, 31)
    object AIR_CABLE1_INPUT_SELECTOR : R820TRegister(5, 96)
    object LOOP_THROUGH : R820TRegister(5, 128)
    object CABLE2_INPUT_SELECTOR : R820TRegister(
        6,
        8
    )

    object FILTER_GAIN : R820TRegister(6, 48)
    object PRE_DETECT : R820TRegister(6, 64)
    object MIXER_GAIN : R820TRegister(7, 31)
    object IMAGE_REVERSE : R820TRegister(7, 128)
    object UNKNOWN_REGISTER_8 : R820TRegister(
        8,
        63
    )

    object UNKNOWN_REGISTER_9 : R820TRegister(9, 63)
    object FILTER_CALIBRATION_CODE : R820TRegister(10, 31)
    object FILTER_CURRENT : R820TRegister(10, 96)
    object CALIBRATION_TRIGGER : R820TRegister(
        11,
        16
    )

    object FILTER_CAPACITOR : R820TRegister(11, 96)
    object BANDWIDTH_FILTER_GAIN_HIGHPASS_FILTER_CORNER : R820TRegister(11, 239)
    object XTAL_CHECK : R820TRegister(
        12,
        15
    )

    object VGA_GAIN : R820TRegister(12, 159)
    object LNA_VTH_L : R820TRegister(13, 0)
    object MIXER_VTH_L : R820TRegister(14, 0)
    object CALIBRATION_CLOCK : R820TRegister(15, 4)
    object FILTER_EXTENSION_WIDEST : R820TRegister(
        15,
        128
    )

    object PLL_XTAL_CAPACITOR : R820TRegister(16, 3)
    object UNKNOWN_REGISTER_10 : R820TRegister(16, 4)
    object PLL_XTAL_CAPACITOR_AND_DRIVE : R820TRegister(
        16,
        11
    )

    object REFERENCE_DIVIDER_2 : R820TRegister(16, 16)
    object CAPACITOR_SELECTOR : R820TRegister(16, 27)
    object DIVIDER : R820TRegister(16, 224)
    object CP_CUR : R820TRegister(
        17,
        56
    )

    object SIGMA_DELTA_MODULATOR_POWER : R820TRegister(18, 8)
    object VCO_CURRENT : R820TRegister(18, 224)
    object VERSION : R820TRegister(19, 63)
    object PLL : R820TRegister(
        20,
        0
    )

    object SIGMA_DELTA_MODULATOR_LSB : R820TRegister(21, 0)
    object SIGMA_DELTA_MODULATOR_MSB : R820TRegister(22, 0)
    object DRAIN : R820TRegister(23, 8)
    object DIVIDER_BUFFER_CURRENT : R820TRegister(
        23,
        48
    )

    object RF_POLY_FILTER_CURRENT : R820TRegister(25, 96)
    object PLL_AUTOTUNE : R820TRegister(26, 12)
    object PLL_AUTOTUNE_VARIANT : R820TRegister(26, 8)
    object AGC_CLOCK : R820TRegister(
        26,
        48
    )

    object RF_POLY_MUX : R820TRegister(26, 195)
    object TF_BAND : R820TRegister(27, 0)
    object MIXER_TOP : R820TRegister(28, 248)
    object MIXER_TOP2 : R820TRegister(28, 4)
    object LNA_TOP : R820TRegister(29, 56)
    object LNA_TOP2 : R820TRegister(
        29,
        199
    )

    object CHANNEL_FILTER_EXTENSION : R820TRegister(30, 96)
    object LNA_DISCHARGE_CURRENT : R820TRegister(30, 31)
    object LOOP_THROUGH_ATTENUATION : R820TRegister(31, 128)
    companion object {
        fun values(): Array<R820TRegister> {
            return arrayOf(
                LNA_GAIN,
                AIR_CABLE1_INPUT_SELECTOR,
                LOOP_THROUGH,
                CABLE2_INPUT_SELECTOR,
                FILTER_GAIN,
                PRE_DETECT,
                MIXER_GAIN,
                IMAGE_REVERSE,
                UNKNOWN_REGISTER_8,
                UNKNOWN_REGISTER_9,
                FILTER_CALIBRATION_CODE,
                FILTER_CURRENT,
                CALIBRATION_TRIGGER,
                FILTER_CAPACITOR,
                BANDWIDTH_FILTER_GAIN_HIGHPASS_FILTER_CORNER,
                XTAL_CHECK,
                VGA_GAIN,
                LNA_VTH_L,
                MIXER_VTH_L,
                CALIBRATION_CLOCK,
                FILTER_EXTENSION_WIDEST,
                PLL_XTAL_CAPACITOR,
                UNKNOWN_REGISTER_10,
                PLL_XTAL_CAPACITOR_AND_DRIVE,
                REFERENCE_DIVIDER_2,
                CAPACITOR_SELECTOR,
                DIVIDER,
                CP_CUR,
                SIGMA_DELTA_MODULATOR_POWER,
                VCO_CURRENT,
                VERSION,
                PLL,
                SIGMA_DELTA_MODULATOR_LSB,
                SIGMA_DELTA_MODULATOR_MSB,
                DRAIN,
                DIVIDER_BUFFER_CURRENT,
                RF_POLY_FILTER_CURRENT,
                PLL_AUTOTUNE,
                PLL_AUTOTUNE_VARIANT,
                AGC_CLOCK,
                RF_POLY_MUX,
                TF_BAND,
                MIXER_TOP,
                MIXER_TOP2,
                LNA_TOP,
                LNA_TOP2,
                CHANNEL_FILTER_EXTENSION,
                LNA_DISCHARGE_CURRENT,
                LOOP_THROUGH_ATTENUATION
            )
        }

        fun valueOf(value: String): R820TRegister {
            return when (value) {
                "LNA_GAIN" -> LNA_GAIN
                "AIR_CABLE1_INPUT_SELECTOR" -> AIR_CABLE1_INPUT_SELECTOR
                "LOOP_THROUGH" -> LOOP_THROUGH
                "CABLE2_INPUT_SELECTOR" -> CABLE2_INPUT_SELECTOR
                "FILTER_GAIN" -> FILTER_GAIN
                "PRE_DETECT" -> PRE_DETECT
                "MIXER_GAIN" -> MIXER_GAIN
                "IMAGE_REVERSE" -> IMAGE_REVERSE
                "UNKNOWN_REGISTER_8" -> UNKNOWN_REGISTER_8
                "UNKNOWN_REGISTER_9" -> UNKNOWN_REGISTER_9
                "FILTER_CALIBRATION_CODE" -> FILTER_CALIBRATION_CODE
                "FILTER_CURRENT" -> FILTER_CURRENT
                "CALIBRATION_TRIGGER" -> CALIBRATION_TRIGGER
                "FILTER_CAPACITOR" -> FILTER_CAPACITOR
                "BANDWIDTH_FILTER_GAIN_HIGHPASS_FILTER_CORNER" -> BANDWIDTH_FILTER_GAIN_HIGHPASS_FILTER_CORNER
                "XTAL_CHECK" -> XTAL_CHECK
                "VGA_GAIN" -> VGA_GAIN
                "LNA_VTH_L" -> LNA_VTH_L
                "MIXER_VTH_L" -> MIXER_VTH_L
                "CALIBRATION_CLOCK" -> CALIBRATION_CLOCK
                "FILTER_EXTENSION_WIDEST" -> FILTER_EXTENSION_WIDEST
                "PLL_XTAL_CAPACITOR" -> PLL_XTAL_CAPACITOR
                "UNKNOWN_REGISTER_10" -> UNKNOWN_REGISTER_10
                "PLL_XTAL_CAPACITOR_AND_DRIVE" -> PLL_XTAL_CAPACITOR_AND_DRIVE
                "REFERENCE_DIVIDER_2" -> REFERENCE_DIVIDER_2
                "CAPACITOR_SELECTOR" -> CAPACITOR_SELECTOR
                "DIVIDER" -> DIVIDER
                "CP_CUR" -> CP_CUR
                "SIGMA_DELTA_MODULATOR_POWER" -> SIGMA_DELTA_MODULATOR_POWER
                "VCO_CURRENT" -> VCO_CURRENT
                "VERSION" -> VERSION
                "PLL" -> PLL
                "SIGMA_DELTA_MODULATOR_LSB" -> SIGMA_DELTA_MODULATOR_LSB
                "SIGMA_DELTA_MODULATOR_MSB" -> SIGMA_DELTA_MODULATOR_MSB
                "DRAIN" -> DRAIN
                "DIVIDER_BUFFER_CURRENT" -> DIVIDER_BUFFER_CURRENT
                "RF_POLY_FILTER_CURRENT" -> RF_POLY_FILTER_CURRENT
                "PLL_AUTOTUNE" -> PLL_AUTOTUNE
                "PLL_AUTOTUNE_VARIANT" -> PLL_AUTOTUNE_VARIANT
                "AGC_CLOCK" -> AGC_CLOCK
                "RF_POLY_MUX" -> RF_POLY_MUX
                "TF_BAND" -> TF_BAND
                "MIXER_TOP" -> MIXER_TOP
                "MIXER_TOP2" -> MIXER_TOP2
                "LNA_TOP" -> LNA_TOP
                "LNA_TOP2" -> LNA_TOP2
                "CHANNEL_FILTER_EXTENSION" -> CHANNEL_FILTER_EXTENSION
                "LNA_DISCHARGE_CURRENT" -> LNA_DISCHARGE_CURRENT
                "LOOP_THROUGH_ATTENUATION" -> LOOP_THROUGH_ATTENUATION
                else -> throw IllegalArgumentException("No object com.virginiaprivacy.sdr.R820TRegister.$value")
            }
        }
    }
}