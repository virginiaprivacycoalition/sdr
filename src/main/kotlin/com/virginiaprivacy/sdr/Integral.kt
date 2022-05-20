package com.virginiaprivacy.sdr

sealed class Integral(val number: Int, private val mRegister: Int) {

    val registerValue: Byte
        get() = mRegister.toByte()

    companion object {
        fun fromValue(value: Int): Integral {
            return if (value in 0..31) {
                values()[value]
            } else {
                throw IllegalArgumentException("PLL integral value [$value] must be in the range 0 - 31")
            }
        }

        fun values(): Array<Integral> {
            return arrayOf(
                I00,
                I01,
                I02,
                I03,
                I04,
                I05,
                I06,
                I07,
                I08,
                I09,
                I10,
                I11,
                I12,
                I13,
                I14,
                I15,
                I16,
                I17,
                I18,
                I19,
                I20,
                I21,
                I22,
                I23,
                I24,
                I25,
                I26,
                I27,
                I28,
                I29,
                I30,
                I31
            )
        }

        fun valueOf(value: String): Integral {
            return when (value) {
                "I00" -> I00
                "I01" -> I01
                "I02" -> I02
                "I03" -> I03
                "I04" -> I04
                "I05" -> I05
                "I06" -> I06
                "I07" -> I07
                "I08" -> I08
                "I09" -> I09
                "I10" -> I10
                "I11" -> I11
                "I12" -> I12
                "I13" -> I13
                "I14" -> I14
                "I15" -> I15
                "I16" -> I16
                "I17" -> I17
                "I18" -> I18
                "I19" -> I19
                "I20" -> I20
                "I21" -> I21
                "I22" -> I22
                "I23" -> I23
                "I24" -> I24
                "I25" -> I25
                "I26" -> I26
                "I27" -> I27
                "I28" -> I28
                "I29" -> I29
                "I30" -> I30
                "I31" -> I31
                else -> throw IllegalArgumentException("No object com.virginiaprivacy.sdr.Integral.$value")
            }
        }
    }

    object I00 : Integral(0, 68)
    object I01 : Integral(1, 132)
    object I02 : Integral(2, 196)
    object I03 : Integral(3, 5)
    object I04 : Integral(4, 69)
    object I05 : Integral(5, 133)
    object I06 : Integral(6, 197)
    object I07 : Integral(7, 6)
    object I08 : Integral(
        8,
        70
    )

    object I09 : Integral(9, 134)
    object I10 : Integral(10, 198)
    object I11 : Integral(11, 7)
    object I12 : Integral(12, 71)
    object I13 : Integral(13, 135)
    object I14 : Integral(14, 199)
    object I15 : Integral(15, 8)
    object I16 : Integral(16, 72)
    object I17 : Integral(
        17,
        136
    )

    object I18 : Integral(18, 200)
    object I19 : Integral(19, 9)
    object I20 : Integral(20, 73)
    object I21 : Integral(21, 137)
    object I22 : Integral(22, 201)
    object I23 : Integral(23, 10)
    object I24 : Integral(24, 74)
    object I25 : Integral(25, 138)
    object I26 : Integral(
        26,
        202
    )

    object I27 : Integral(27, 11)
    object I28 : Integral(28, 75)
    object I29 : Integral(29, 139)
    object I30 : Integral(30, 203)
    object I31 : Integral(31, 12)
}