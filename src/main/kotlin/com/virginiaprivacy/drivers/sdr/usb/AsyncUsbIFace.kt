package com.virginiaprivacy.drivers.sdr.usb

interface AsyncUsbIFace : UsbIFace {
    suspend fun bulkTransferAsync(index: Int): CharArray
}