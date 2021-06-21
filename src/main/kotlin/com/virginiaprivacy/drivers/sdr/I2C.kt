package com.virginiaprivacy.drivers.sdr

import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
interface I2C {

    val dev: RTLDevice

    fun i2cWriteFun(address: Int, buf: ByteArray, length: Int): Int {
        return dev.i2cWrite(address, buf, length)
    }

    fun i2cReadFun(address: Int, length: Int): ByteArray =
        dev.i2cRead(address, length)


}