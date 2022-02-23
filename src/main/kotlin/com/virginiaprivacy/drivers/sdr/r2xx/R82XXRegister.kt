package com.virginiaprivacy.drivers.sdr.r2xx

enum class R82XXRegister(override val address: Int, override val mask: Int) : Reg {

    AIR_CABLE1_INPUT(0x05, 0x60),


    LOOP_THROUGH(0x05, 0x80),


    /**
     * LNA AGC control
     * 0: Enable LNA auto gain mode (automatic)
     * 0x10: Disable LNA auto gain mode (manual gain)
     */
    LNA_GAIN_AUTO(0x05, 0x10),

    /**
     * LNA gain power
     * Used in combination with [LNA_GAIN] to control the LNA Gain setting
     */
    LNA_GAIN_POWER(0x05, 0x1f),

    /**
     *
     */
    LNA_GAIN(0x05, 0x0f),

    CABLE2_INPUT(0x06, 0x08),


    FILTER_GAIN(0x06, 0x30),

    PRE_DETECT(0x06, 0x40),



    IMAGE_REVERSE(0x07, 0x80),


    MIXER_GAIN(0x07, 0x1F),

    /**
     * AGC for mixer
     * 0: mixer AGC disabled (manual mode)
     * 0x10: mixer AGC enabled (automatic gain mode_
     */
    MIXER_AUTO_GAIN_ENABLED(0x07, 0x10),

    /**
     *  Mixer gain level:
     *  [MIXER_GAIN]
     */
    MIXER_GAIN_SETTINGS(0x07, 0x0f),

    /**
     * Mixer buffer power
     * 0: off 1: on
     */
    MIXER_BUFFER_POWER(0x08, 0x3f),
    DIVIDER(0x10, 0xE0),

    FILTER_CALIBRATION_CODE(0x0A, 0x1F),

    BANDWIDTH_FILTER_GAIN_HIGHPASS_FILTER_CORNER(0x0B, 0xEF),

    CALIBRATION_TRIGGER(0x0B, 0x10),


    FILTER_CAPACITOR(0x0B, 0x60),

    /**
     * Calibration clock
     * 0x04: Enable
     */
    CALIBRATION_CLOCK(0x0F, 0x04),
    FILTER_EXTENSION_WIDEST(0x0F, 0x80),




    /**
     * Mixer gain power
     * Used in combination with [MIXER_GAIN] to control the mixer Gain setting
     */

    /**
     * Open drain
     * 0: High-Z 1: Low-Z
     */
    OPEN_D(0x17, 0x08),

    RF_POLY_FILTER_CURRENT(0x19, 0x60),
    PLL_AUTOTUNE(0x1A, 0x0C),
    PLL_AUTOTUNE2(0x1A, 0x08),
    AGC_CLOCK(0x1A, 0x30),



    /**
     * Tracking filter switch
     * 00: Tracking filter on
     * 01: Bypass tracking filter
     */
    RFMUX(0x1a, 0xc3),

    /**
     * Internal xtal cap settings
     * 00: no cap, 01: 10pF
     * 10: 20pF, 11: 30pF
     */
    XTAL_CAP(0x10, 0x0b),

    /**
     * IF filter power
     * 0: filter on
     * 1: off
     */
    IF_FILTER_POWER(0x09, 0x3f),

    /**
     * PLL reference frequency divider
     * 0: fref=xtal_freq
     * 1: fref=xtal_freq / 2
     */
    PLL_REFDIV(0x10, 0x10),

    XTAL_CHECK(0x0C, 0x0F),

    LNA_VTH(0x0D, 0x0),
    MIXER_VTH(0x0E, 0x0),

    /**
     * PLL autotune clockrate
     * 00: 128 khz
     * 01: 32 khz
     * 10: 8khz
     */
    PLL_AUTOTUNE_CLOCKRATE(0x1A, 0x0C),

    CP_CURRENT(0x11, 0x38),
    SDM_POWER(0x12, 0x08),

    VERSION(0x12, 0xE0),

    /**
     * ?
     */
    VCO_CURRENT(0x12, 0xE0),
    PLL(0x14, 0x0),
    SDM_LSB(0x15, 0x0),
    SDM_MSB(0x16, 0x0),
    DIVIDER_BUFFER_CURRENT(0x17, 0x30),
    FILTER_BUFFER_CURRENT(0x0A, 0x60),


    TF_BAND(0x01b, 0x00),

    /**
     * Sets the amount of VGA gain. Used in combination with [VGA_GAIN]
     */
    VGA_GAIN(0x0C, 0x9F),

    MIXER_TOP(0X1C, 0xF8),
    MIXER_TOP2(0X1C, 0x04),


    LNA_TOP(0x1D, 0x38),

    LNA_TOP2(0x1D, 0xC7),

    CHANNEL_FILTER_EXTENSION(0x1E, 0x60),
    LNA_DISCHARGE_CURRENT(0x1E, 0x1F),

    LOOP_THROUGH_ATTENUATION(0x1F, 0x80);

    init {
        if (R82XX.regSet.containsKey(address to mask)) {
            throw IllegalArgumentException("Duplicate register value detected: ${this.name} has same address and key as ${R82XX.regSet[address to mask]}")
        }
    }
}