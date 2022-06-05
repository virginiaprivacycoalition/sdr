package com.virginiaprivacy.sdr.tuner

sealed class TunerGain(val value: Int) {
    object AutomaticGain : TunerGain(0)
    object GainLevel1 : TunerGain(1)
    object GainLevel2 : TunerGain(2)
    object GainLevel3 : TunerGain(3)
    object GainLevel4 : TunerGain(4)
    object GainLevel5 : TunerGain(5)
    object GainLevel6 : TunerGain(6)
    object GainLevel7 : TunerGain(7)
    object GainLevel8 : TunerGain(8)
    object GainLevel9 : TunerGain(9)
    object GainLevel10 : TunerGain(10)
}
