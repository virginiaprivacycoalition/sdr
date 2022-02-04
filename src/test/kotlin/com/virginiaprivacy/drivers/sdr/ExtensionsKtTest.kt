package com.virginiaprivacy.drivers.sdr

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe


class ExtensionsKtTest : FunSpec({

    test("byte and bitwise") {
        val y = (-3 and 0xf).toByte()
        val x = (-3).toByte() and 0xf
        x shouldBe y
    }

    test("bit bitwise shift right") {
        (-3).toByte() shr 4 shouldBeExactly (-2 shr 4)
    }


})