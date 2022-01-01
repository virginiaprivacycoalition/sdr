package com.virginiaprivacy.drivers.sdr

interface TunableGain {

    var lnaGain: LNA_GAIN
    var vgaGain: VGA_GAIN
    var mixerGain: MIXER_GAIN
}


enum class LNA_GAIN(val value: Int) {

    AUTOMATIC(0x00),
    _0( 0x10),
    _9( 0x11),
    _21( 0x12),
    _61( 0x13),
    _99( 0x14),
    _112(0x15),
    _143(0x16),
    _165(0x17),
    _191(0x18),
    _222(0x19),
    _248(0x1A),
    _262(0x1B),
    _281(0x1C),
    _286(0x1D),
    _321(0x1E),
    _334(0x1F);

}

enum class VGA_GAIN(val value: Int) {
    _0(0x00),
    _26(0x01),
    _52(0x02),
    _82(0x03),
    _124(0x04),
    _159(0x05),
    _183(0x06),
    _196(0x07),
    _210(0x08),
    _242(0x09),
    _278(0x0A),
    _312(0x0B),
    _347(0x0C),
    _384(0x0D),
    _419(0x0E),
    _455(0x0F);

}

enum class MIXER_GAIN(val value: Int) {
    AUTOMATIC(0x10),
    _0(0x00),
    _5(0x01),
    _15(0x02),
    _25(0x03),
    _44(0x04),
    _53(0x05),
    _63(0x06),
    _88(0x07),
    _105(0x08),
    _115(0x09),
    _123(0x0A),
    _139(0x0B),
    _152(0x0C),
    _158(0x0D),
    _161(0x0E),
    _153(0x0F);
}