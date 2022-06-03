package com.virginiaprivacy.sdr.tuner

sealed interface Tuner
sealed interface R820TTunerType
sealed class TunerType(val label: String, val tunerClass: Class<*>) : Tuner {
    object AIRSPY_R820T : TunerType("Airspy R820T", R820TTunerController::class.java)
    object ELONICS_E4000 : TunerType("E4000", E4KTunerController::class.java)
    object ETTUS_WBX : TunerType("WBX", RTL2832TunerController::class.java)
    object ETTUS_VARIOUS : TunerType("Ettus Tuner", RTL2832TunerController::class.java)
    object FCI_FC2580 : TunerType("FC2580", RTL2832TunerController::class.java)
    object FITIPOWER_FC0012 : TunerType(
        "FC0012"
        , RTL2832TunerController::class.java)

    object FITIPOWER_FC0013 : TunerType("FC0013", RTL2832TunerController::class.java)
    object FUNCUBE_DONGLE_PRO : TunerType("Funcube Dongle Pro", RTL2832TunerController::class.java)
    object FUNCUBE_DONGLE_PRO_PLUS : TunerType("Funcube Dongle Pro Plus", RTL2832TunerController::class.java)
    object HACKRF : TunerType(
        "HackRF"
        , RTL2832TunerController::class.java)

    object RAFAELMICRO_R820T : TunerType("R820T", R820TTunerController::class.java), R820TTunerType
    object RAFAELMICRO_R828D : TunerType("R828D", R820TTunerController::class.java), R820TTunerType
    object RTL2832_VARIOUS : TunerType("Generic", RTL2832TunerController::class.java), R820TTunerType
    object UNKNOWN : TunerType("Unknown", RTL2832TunerController::class.java)
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

