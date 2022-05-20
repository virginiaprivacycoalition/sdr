package com.virginiaprivacy.sdr.tuner

sealed interface Tuner
sealed interface R820TTunerType
sealed class TunerType(val label: String) : Tuner {
    object AIRSPY_R820T : TunerType("Airspy R820T")
    object ELONICS_E4000 : TunerType("E4000")
    object ETTUS_WBX : TunerType("WBX")
    object ETTUS_VARIOUS : TunerType("Ettus Tuner")
    object FCI_FC2580 : TunerType("FC2580")
    object FITIPOWER_FC0012 : TunerType(
        "FC0012"
    )

    object FITIPOWER_FC0013 : TunerType("FC0013")
    object FUNCUBE_DONGLE_PRO : TunerType("Funcube Dongle Pro")
    object FUNCUBE_DONGLE_PRO_PLUS : TunerType("Funcube Dongle Pro Plus")
    object HACKRF : TunerType(
        "HackRF"
    )

    object RAFAELMICRO_R820T : TunerType("R820T"), R820TTunerType
    object RAFAELMICRO_R828D : TunerType("R828D"), R820TTunerType
    object RTL2832_VARIOUS : TunerType("Generic"), R820TTunerType
    object UNKNOWN : TunerType("Unknown")
    companion object {
        fun values(): Array<TunerType> {
            return arrayOf(
                AIRSPY_R820T,
                ELONICS_E4000,
                ETTUS_WBX,
                ETTUS_VARIOUS,
                FCI_FC2580,
                FITIPOWER_FC0012,
                FITIPOWER_FC0013,
                FUNCUBE_DONGLE_PRO,
                FUNCUBE_DONGLE_PRO_PLUS,
                HACKRF,
                RAFAELMICRO_R820T,
                RAFAELMICRO_R828D,
                RTL2832_VARIOUS,
                UNKNOWN
            )
        }

        fun valueOf(value: String): TunerType {
            return when (value) {
                "AIRSPY_R820T" -> AIRSPY_R820T
                "ELONICS_E4000" -> ELONICS_E4000
                "ETTUS_WBX" -> ETTUS_WBX
                "ETTUS_VARIOUS" -> ETTUS_VARIOUS
                "FCI_FC2580" -> FCI_FC2580
                "FITIPOWER_FC0012" -> FITIPOWER_FC0012
                "FITIPOWER_FC0013" -> FITIPOWER_FC0013
                "FUNCUBE_DONGLE_PRO" -> FUNCUBE_DONGLE_PRO
                "FUNCUBE_DONGLE_PRO_PLUS" -> FUNCUBE_DONGLE_PRO_PLUS
                "HACKRF" -> HACKRF
                "RAFAELMICRO_R820T" -> RAFAELMICRO_R820T
                "RAFAELMICRO_R828D" -> RAFAELMICRO_R828D
                "RTL2832_VARIOUS" -> RTL2832_VARIOUS
                "UNKNOWN" -> UNKNOWN
                else -> throw IllegalArgumentException("No object com.virginiaprivacy.sdr.TunerType.$value")
            }
        }
    }

}

