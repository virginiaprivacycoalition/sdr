package com.virginiaprivacy.drivers.sdr

import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
interface I2C {

    val dev: RTLDevice

    /**
     * Writes data to the I2C address.
     * @param address - the starting i2c address.
     * @param buf - the bytes to write.
     * @param length - the length of the bytes to be written.
     * @returns the number of bytes written to the i2c register.
     */
    fun i2cWriteFun(address: Int, buf: ByteArray, length: Int): Int = dev.i2cWrite(address, buf, length)

    fun i2cReadFun(address: Int, length: Int): ByteArray =
        dev.i2cRead(address, length)


}