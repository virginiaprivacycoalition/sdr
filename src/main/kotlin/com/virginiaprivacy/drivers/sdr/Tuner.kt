package com.virginiaprivacy.drivers.sdr

import com.virginiaprivacy.drivers.sdr.r2xx.R82xxChip

enum class Tuner(
    val i2cAddress: Byte,
    val checkAddress: Byte,
    val checkVal: Int,
    val chip: R82xxChip
) {

    RTLSDR_TUNER_E4000(0xc8.toByte(), 0x02, 64, R82xxChip.UNKNOWN),
    RTLSDR_TUNER_FC0012(0xc6.toByte(), 0x00, 161, R82xxChip.UNKNOWN),
    RTLSDR_TUNER_FC0013(0xc6.toByte(), 0x00, 163, R82xxChip.UNKNOWN),
    RTLSDR_TUNER_R820T(0x34, 0x00, 105, R82xxChip.CHIP_R820T),
    RTLSDR_TUNER_R828D(0x74, 0x00, 105, R82xxChip.CHIP_R828D),
    RTLSDR_TUNER_FC2580(0xac.toByte(), 0x01, 86, R82xxChip.UNKNOWN);


    companion object {

        fun tunerType(device: RTLDevice): Tuner =
            values().first {
                val i2cReadReg = device.i2cReadRegister(it.i2cAddress, it.checkAddress)
                return@first (it.checkVal == i2cReadReg) || i2cReadReg == 0
            }
    }
}