package com.virginiaprivacy.drivers.sdr.r2xx

enum class R82xxDeliverySystem {
    SYS_UNDEFINED,
    SYS_DVBT,
    SYS_DVBT2,
    SYS_ISDBT;

    fun getFlags(freq: Long = 0): Flags {
        if (this == SYS_DVBT) {
            if ((freq.toInt() == 506000000) || (freq.toInt() == 666000000) ||
                freq.toInt() == 818000000
            ) {
                return Flags(mixer_top = 0x14, lna_top = 0xe5, cp_cur = 0x28, div_buf_cur = 0x20)
            }
            return Flags(mixer_top = 0x24, lna_top = 0xe5, cp_cur = 0x38, div_buf_cur = 0x30)
        }
        if (this == SYS_DVBT2) {
            return Flags()
        }
        if (this == SYS_ISDBT) {
            return Flags(lna_vth_l = 0x75)
        }
        return Flags()
    }

    data class Flags(
        var mixer_top: Int = 0x24,
        var lna_top: Int = 0xe5,
        var lna_vth_l: Int = 0x53,
        var mixer_vth_l: Int = 0x75,
        var air_cable1_in: Int = 0x00,
        var cable2_in: Int = 0x00,
        var pre_dect: Int = 0x40,
        var lna_discharge: Int = 14,
        var cp_cur: Int = 0x38,
        var div_buf_cur: Int = 0x30,
        var filter_cur: Int = 0x40,
    )

}