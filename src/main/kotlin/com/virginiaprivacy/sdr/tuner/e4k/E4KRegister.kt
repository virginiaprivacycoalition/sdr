package com.virginiaprivacy.sdr.tuner.e4k

sealed class E4KRegister(val mValue: Int) {

    val value: Byte
        get() = mValue.toByte()

    object DUMMY : E4KRegister(0)
    object MASTER1 : E4KRegister(0)
    object MASTER2 : E4KRegister(1)
    object MASTER3 : E4KRegister(2)
    object MASTER4 : E4KRegister(3)
    object MASTER5 : E4KRegister(4)
    object CLK_INP : E4KRegister(5)
    object REF_CLK : E4KRegister(6)
    object SYNTH1 : E4KRegister(7)
    object SYNTH2 : E4KRegister(8)

    object SYNTH3 : E4KRegister(9)
    object SYNTH4 : E4KRegister(10)
    object SYNTH5 : E4KRegister(11)
    object SYNTH6 : E4KRegister(12)
    object SYNTH7 : E4KRegister(13)
    object SYNTH8 : E4KRegister(14)
    object SYNTH9 : E4KRegister(15)
    object FILT1 : E4KRegister(16)
    object FILT2 : E4KRegister(17)
    object FILT3 : E4KRegister(
        18
    )

    object GAIN1 : E4KRegister(20)
    object GAIN2 : E4KRegister(21)
    object GAIN3 : E4KRegister(22)
    object GAIN4 : E4KRegister(23)

    /**
     * Auto Gain Control Mode
     * @see AGCMode for different AGC mode options
     */
    object AGC1 : E4KRegister(26)
    object AGC2 : E4KRegister(27)

    /**
     * RSSI Indicator - Read to get tuner RSSI value
     * "The RSSI indicator can be accessed when operating in autonomous or supervisor gain control mode but not
     *  when using PWM or serial interface control."
     */
    object AGC3 : E4KRegister(28)
    object AGC4 : E4KRegister(29)
    object AGC5 : E4KRegister(30)
    object AGC6 : E4KRegister(31)

    /**
     * Mixer gain control mode
     * 1 = auto
     */
    object AGC7 : E4KRegister(32)
    object AGC8 : E4KRegister(
        33
    )

    object AGC11 : E4KRegister(36)
    object AGC12 : E4KRegister(37)
    object DC1 : E4KRegister(41)
    object DC2 : E4KRegister(42)
    object DC3 : E4KRegister(43)
    object DC4 : E4KRegister(44)
    object DC5 : E4KRegister(45)
    object DC6 : E4KRegister(46)
    object DC7 : E4KRegister(47)
    object DC8 : E4KRegister(48)
    object QLUT0 : E4KRegister(80)
    object QLUT1 : E4KRegister(
        81
    )

    object QLUT2 : E4KRegister(82)
    object QLUT3 : E4KRegister(83)
    object ILUT0 : E4KRegister(96)
    object ILUT1 : E4KRegister(97)
    object ILUT2 : E4KRegister(98)
    object ILUT3 : E4KRegister(99)
    object DCTIME1 : E4KRegister(112)
    object DCTIME2 : E4KRegister(113)
    object DCTIME3 : E4KRegister(114)
    object DCTIME4 : E4KRegister(
        115
    )

    object PWM1 : E4KRegister(116)
    object PWM2 : E4KRegister(117)
    object PWM3 : E4KRegister(118)
    object PWM4 : E4KRegister(119)
    object BIAS : E4KRegister(120)
    object CLKOUT_PWDN : E4KRegister(122)
    object CHFILT_CALIB : E4KRegister(123)
    object I2C_REG_ADDR : E4KRegister(125)
    object MAGIC_1 : E4KRegister(
        126
    )

    object MAGIC_2 : E4KRegister(127)
    object MAGIC_3 : E4KRegister(130)
    object MAGIC_4 : E4KRegister(134)
    object MAGIC_5 : E4KRegister(135)
    object MAGIC_6 : E4KRegister(136)
    object MAGIC_7 : E4KRegister(159)
    object MAGIC_8 : E4KRegister(160)
    object I2C_REGISTER : E4KRegister(
        200
    )

    companion object {
        fun values(): Array<E4KRegister> {
            return arrayOf(
                DUMMY,
                MASTER1,
                MASTER2,
                MASTER3,
                MASTER4,
                MASTER5,
                CLK_INP,
                REF_CLK,
                SYNTH1,
                SYNTH2,
                SYNTH3,
                SYNTH4,
                SYNTH5,
                SYNTH6,
                SYNTH7,
                SYNTH8,
                SYNTH9,
                FILT1,
                FILT2,
                FILT3,
                GAIN1,
                GAIN2,
                GAIN3,
                GAIN4,
                AGC1,
                AGC2,
                AGC3,
                AGC4,
                AGC5,
                AGC6,
                AGC7,
                AGC8,
                AGC11,
                AGC12,
                DC1,
                DC2,
                DC3,
                DC4,
                DC5,
                DC6,
                DC7,
                DC8,
                QLUT0,
                QLUT1,
                QLUT2,
                QLUT3,
                ILUT0,
                ILUT1,
                ILUT2,
                ILUT3,
                DCTIME1,
                DCTIME2,
                DCTIME3,
                DCTIME4,
                PWM1,
                PWM2,
                PWM3,
                PWM4,
                BIAS,
                CLKOUT_PWDN,
                CHFILT_CALIB,
                I2C_REG_ADDR,
                MAGIC_1,
                MAGIC_2,
                MAGIC_3,
                MAGIC_4,
                MAGIC_5,
                MAGIC_6,
                MAGIC_7,
                MAGIC_8,
                I2C_REGISTER
            )
        }

        fun valueOf(value: String): E4KRegister {
            return when (value) {
                "DUMMY" -> DUMMY
                "MASTER1" -> MASTER1
                "MASTER2" -> MASTER2
                "MASTER3" -> MASTER3
                "MASTER4" -> MASTER4
                "MASTER5" -> MASTER5
                "CLK_INP" -> CLK_INP
                "REF_CLK" -> REF_CLK
                "SYNTH1" -> SYNTH1
                "SYNTH2" -> SYNTH2
                "SYNTH3" -> SYNTH3
                "SYNTH4" -> SYNTH4
                "SYNTH5" -> SYNTH5
                "SYNTH6" -> SYNTH6
                "SYNTH7" -> SYNTH7
                "SYNTH8" -> SYNTH8
                "SYNTH9" -> SYNTH9
                "FILT1" -> FILT1
                "FILT2" -> FILT2
                "FILT3" -> FILT3
                "GAIN1" -> GAIN1
                "GAIN2" -> GAIN2
                "GAIN3" -> GAIN3
                "GAIN4" -> GAIN4
                "AGC1" -> AGC1
                "AGC2" -> AGC2
                "AGC3" -> AGC3
                "AGC4" -> AGC4
                "AGC5" -> AGC5
                "AGC6" -> AGC6
                "AGC7" -> AGC7
                "AGC8" -> AGC8
                "AGC11" -> AGC11
                "AGC12" -> AGC12
                "DC1" -> DC1
                "DC2" -> DC2
                "DC3" -> DC3
                "DC4" -> DC4
                "DC5" -> DC5
                "DC6" -> DC6
                "DC7" -> DC7
                "DC8" -> DC8
                "QLUT0" -> QLUT0
                "QLUT1" -> QLUT1
                "QLUT2" -> QLUT2
                "QLUT3" -> QLUT3
                "ILUT0" -> ILUT0
                "ILUT1" -> ILUT1
                "ILUT2" -> ILUT2
                "ILUT3" -> ILUT3
                "DCTIME1" -> DCTIME1
                "DCTIME2" -> DCTIME2
                "DCTIME3" -> DCTIME3
                "DCTIME4" -> DCTIME4
                "PWM1" -> PWM1
                "PWM2" -> PWM2
                "PWM3" -> PWM3
                "PWM4" -> PWM4
                "BIAS" -> BIAS
                "CLKOUT_PWDN" -> CLKOUT_PWDN
                "CHFILT_CALIB" -> CHFILT_CALIB
                "I2C_REG_ADDR" -> I2C_REG_ADDR
                "MAGIC_1" -> MAGIC_1
                "MAGIC_2" -> MAGIC_2
                "MAGIC_3" -> MAGIC_3
                "MAGIC_4" -> MAGIC_4
                "MAGIC_5" -> MAGIC_5
                "MAGIC_6" -> MAGIC_6
                "MAGIC_7" -> MAGIC_7
                "MAGIC_8" -> MAGIC_8
                "I2C_REGISTER" -> I2C_REGISTER
                else -> throw IllegalArgumentException("No object com.virginiaprivacy.sdr.tuner.e4k.E4KRegister.$value")
            }
        }
    }
}