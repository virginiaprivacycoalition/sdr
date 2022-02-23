package com.virginiaprivacy.drivers.sdr.r2xx

sealed interface Reg {
    val address: Int
    val mask: Int
    val isMasked: Boolean
        get() = this.mask != 0
}
