package com.virginiaprivacy.sdr.tuner.e4k

enum class E4KRegister(val mValue: Int) {
    DUMMY(0), MASTER1(0), MASTER2(1), MASTER3(2), MASTER4(3), MASTER5(4), CLK_INP(5), REF_CLK(6), SYNTH1(7), SYNTH2(
        8
    ),
    SYNTH3(9), SYNTH4(10), SYNTH5(11), SYNTH6(12), SYNTH7(13), SYNTH8(14), SYNTH9(15), FILT1(16), FILT2(17), FILT3(
        18
    ),
    GAIN1(20), GAIN2(21), GAIN3(22), GAIN4(23), AGC1(26), AGC2(27), AGC3(28), AGC4(29), AGC5(30), AGC6(31), AGC7(32), AGC8(
        33
    ),
    AGC11(36), AGC12(37), DC1(41), DC2(42), DC3(43), DC4(44), DC5(45), DC6(46), DC7(47), DC8(48), QLUT0(80), QLUT1(
        81
    ),
    QLUT2(82), QLUT3(83), ILUT0(96), ILUT1(97), ILUT2(98), ILUT3(99), DCTIME1(112), DCTIME2(113), DCTIME3(114), DCTIME4(
        115
    ),
    PWM1(116), PWM2(117), PWM3(118), PWM4(119), BIAS(120), CLKOUT_PWDN(122), CHFILT_CALIB(123), I2C_REG_ADDR(125), MAGIC_1(
        126
    ),
    MAGIC_2(127), MAGIC_3(130), MAGIC_4(134), MAGIC_5(135), MAGIC_6(136), MAGIC_7(159), MAGIC_8(160), I2C_REGISTER(
        200
    );

    val value: Byte
        get() = mValue.toByte()
}