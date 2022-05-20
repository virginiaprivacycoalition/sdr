package com.virginiaprivacy.sdr.tuner

enum class Page(val mPage: Int) {
    ZERO(0), ONE(1), TEN(10);

    val page: Byte
        get() = (mPage and 255).toByte()
}