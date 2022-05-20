package com.virginiaprivacy.sdr

import com.virginiaprivacy.sdr.tuner.TunerType
import com.virginiaprivacy.sdr.usb.Descriptor

enum class TunerClass(
    val tunerType: TunerType,
    val vendorID: String,
    val deviceID: String,
    val vendorDescription: String,
    val deviceDescription: String
) {
    AIRSPY(TunerType.AIRSPY_R820T, "1D50", "60A1", "Airspy", "Airspy"), GENERIC_2832(
        TunerType.RTL2832_VARIOUS,
        "0BDA",
        "2832",
        "RTL2832",
        "SDR"
    ),
    GENERIC_2838(
        TunerType.RTL2832_VARIOUS,
        "0BDA",
        "2838",
        "RTL2832",
        "SDR"
    ),
    COMPRO_VIDEOMATE_U620F(
        TunerType.ELONICS_E4000,
        "185B",
        "0620",
        "Compro",
        "Videomate U620F"
    ),
    COMPRO_VIDEOMATE_U650F(
        TunerType.ELONICS_E4000,
        "185B",
        "0650",
        "Compro",
        "Videomate U620F"
    ),
    COMPRO_VIDEOMATE_U680F(
        TunerType.ELONICS_E4000,
        "185B",
        "0680",
        "Compro",
        "Videomate U620F"
    ),
    DEXATEK_LOGILINK_VG002A(
        TunerType.FCI_FC2580,
        "1D19",
        "1101",
        "Dexatek",
        "Logilink VG0002A"
    ),
    DEXATEK_DIGIVOX_MINI_II_REV3(
        TunerType.FCI_FC2580,
        "1D19",
        "1102",
        "Dexatek",
        "MSI Digivox Mini II v3.0"
    ),
    DEXATEK_5217_DVBT(
        TunerType.FCI_FC2580,
        "1D19",
        "1103",
        "Dexatek",
        "5217 DVB-T"
    ),
    ETTUS_USRP_B100(TunerType.ETTUS_VARIOUS, "2500", "0002", "Ettus Research", "USRP B100"), FUNCUBE_DONGLE_PRO(
        TunerType.FUNCUBE_DONGLE_PRO,
        "04D8",
        "FB56",
        "Hamlincrest",
        "Funcube Dongle Pro"
    ),
    FUNCUBE_DONGLE_PRO_PLUS(
        TunerType.FUNCUBE_DONGLE_PRO_PLUS,
        "04D8",
        "FB31",
        "Hamlincrest",
        "Funcube Dongle Pro Plus"
    ),
    GIGABYTE_GTU7300(
        TunerType.FITIPOWER_FC0012,
        "1B80",
        "D393",
        "Gigabyte",
        "GT-U7300"
    ),
    GTEK_T803(TunerType.FITIPOWER_FC0012, "1F4D", "B803", "GTek", "T803"), HACKRF_ONE(
        TunerType.HACKRF,
        "1D50",
        "6089",
        "Great Scott Gadgets",
        "HackRF One"
    ),
    RAD1O(
        TunerType.HACKRF,
        "1D50",
        "CC15",
        "Munich hackerspace",
        "Rad1o"
    ),
    LIFEVIEW_LV5T_DELUXE(
        TunerType.FITIPOWER_FC0012,
        "1F4D",
        "C803",
        "Liveview",
        "LV5T Deluxe"
    ),
    MYGICA_TD312(
        TunerType.FITIPOWER_FC0012,
        "1F4D",
        "D286",
        "MyGica",
        "TD312"
    ),
    PEAK_102569AGPK(
        TunerType.FITIPOWER_FC0012,
        "1B80",
        "D395",
        "Peak",
        "102569AGPK"
    ),
    PROLECTRIX_DV107669(
        TunerType.FITIPOWER_FC0012,
        "1F4D",
        "D803",
        "Prolectrix",
        "DV107669"
    ),
    SVEON_STV20(TunerType.FITIPOWER_FC0012, "1B80", "D39D", "Sveon", "STV20 DVB-T USB & FM"), TERRATEC_CINERGY_T_REV1(
        TunerType.FITIPOWER_FC0012,
        "0CCD",
        "00A9",
        "Terratec",
        "Cinergy T R1"
    ),
    TERRATEC_CINERGY_T_REV3(
        TunerType.ELONICS_E4000,
        "0CCD",
        "00D3",
        "Terratec",
        "Cinergy T R3"
    ),
    TERRATEC_NOXON_REV1_B3(
        TunerType.FITIPOWER_FC0013,
        "0CCD",
        "00B3",
        "Terratec",
        "NOXON R1 (B3)"
    ),
    TERRATEC_NOXON_REV1_B4(
        TunerType.FITIPOWER_FC0013,
        "0CCD",
        "00B4",
        "Terratec",
        "NOXON R1 (B4)"
    ),
    TERRATEC_NOXON_REV1_B7(
        TunerType.FITIPOWER_FC0013,
        "0CCD",
        "00B7",
        "Terratec",
        "NOXON R1 (B7)"
    ),
    TERRATEC_NOXON_REV1_C6(
        TunerType.FITIPOWER_FC0013,
        "0CCD",
        "00C6",
        "Terratec",
        "NOXON R1 (C6)"
    ),
    TERRATEC_NOXON_REV2(TunerType.ELONICS_E4000, "0CCD", "00E0", "Terratec", "NOXON R2"), TERRATEC_T_STICK_PLUS(
        TunerType.ELONICS_E4000,
        "0CCD",
        "00D7",
        "Terratec",
        "T Stick Plus"
    ),
    TWINTECH_UT40(
        TunerType.FITIPOWER_FC0013,
        "1B80",
        "D3A4",
        "Twintech",
        "UT-40"
    ),
    ZAAPA_ZTMINDVBZP(TunerType.FITIPOWER_FC0012, "1B80", "D398", "Zaapa", "ZT-MINDVBZP"), UNKNOWN(
        TunerType.UNKNOWN,
        "0",
        "0",
        "Unknown Manufacturer",
        "Unknown Device"
    );

    override fun toString(): String {
        return "USB Tuner:$tunerType Vendor:$vendorDescription Device:$deviceDescription Address:$vendorID:$deviceID"
    }

    val vendorDeviceLabel: String
        get() = "$vendorDescription $deviceDescription"

    companion object {
        fun valueOf(descriptor: Descriptor): TunerClass {
            return valueOf(descriptor.vendorID.toShort(), descriptor.productID.toShort())
        }

        fun valueOf(vendor: Short, product: Short): TunerClass {
            var retVal = UNKNOWN
            val vendorID = vendor.toInt() and '\uffff'.code
            val productID = product.toInt() and '\uffff'.code
            when (vendorID) {
                1240 -> if (productID == 64305) {
                    retVal = FUNCUBE_DONGLE_PRO_PLUS
                } else if (productID == 64342) {
                    retVal = FUNCUBE_DONGLE_PRO
                }
                3034 -> if (productID == 10290) {
                    retVal = GENERIC_2832
                } else if (productID == 10296) {
                    retVal = GENERIC_2838
                }
                3277 -> if (productID == 169) {
                    retVal = TERRATEC_CINERGY_T_REV1
                } else if (productID == 179) {
                    retVal = TERRATEC_NOXON_REV1_B3
                } else if (productID == 180) {
                    retVal = TERRATEC_NOXON_REV1_B4
                } else if (productID == 181) {
                    retVal = TERRATEC_NOXON_REV1_B7
                } else if (productID == 198) {
                    retVal = TERRATEC_NOXON_REV1_C6
                } else if (productID == 211) {
                    retVal = TERRATEC_CINERGY_T_REV3
                } else if (productID == 215) {
                    retVal = TERRATEC_T_STICK_PLUS
                } else if (productID == 224) {
                    retVal = TERRATEC_NOXON_REV2
                }
                6235 -> if (productID == 1568) {
                    retVal = COMPRO_VIDEOMATE_U620F
                } else if (productID == 1616) {
                    retVal = COMPRO_VIDEOMATE_U650F
                } else if (productID == 1664) {
                    retVal = COMPRO_VIDEOMATE_U680F
                }
                7040 -> if (productID == 54163) {
                    retVal = GIGABYTE_GTU7300
                } else if (productID == 54165) {
                    retVal = PEAK_102569AGPK
                } else if (productID == 54168) {
                    retVal = ZAAPA_ZTMINDVBZP
                } else if (productID == 54173) {
                    retVal = SVEON_STV20
                } else if (productID == 54180) {
                    retVal = TWINTECH_UT40
                }
                7449 -> if (productID == 4353) {
                    retVal = DEXATEK_LOGILINK_VG002A
                } else if (productID == 4354) {
                    retVal = DEXATEK_DIGIVOX_MINI_II_REV3
                } else if (productID == 4355) {
                    retVal = DEXATEK_5217_DVBT
                }
                7504 -> if (productID == 24713) {
                    retVal = HACKRF_ONE
                } else if (productID == 24737) {
                    retVal = AIRSPY
                } else if (productID == 52245) {
                    retVal = HACKRF_ONE
                }
                8013 -> if (productID == 47107) {
                    retVal = GTEK_T803
                } else if (productID == 51203) {
                    retVal = LIFEVIEW_LV5T_DELUXE
                } else if (productID == 53894) {
                    retVal = MYGICA_TD312
                } else if (productID == 55299) {
                    retVal = PROLECTRIX_DV107669
                }
                9472 -> if (productID == 2) {
                    retVal = ETTUS_USRP_B100
                }
            }
            return retVal
        }
    }
}