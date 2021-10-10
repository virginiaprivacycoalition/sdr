package com.virginiaprivacy.drivers.sdr.plugins


fun interface Plugin {

    fun processSignalBuffer(buf: ByteArray)

}


