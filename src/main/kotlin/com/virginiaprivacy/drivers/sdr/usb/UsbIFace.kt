package com.virginiaprivacy.drivers.sdr.usb

import java.nio.ByteBuffer

interface UsbIFace {

    val productName: String

    val serialNumber: String

    val manufacturerName: String

    /**
     * This function should implement the control transfer of config to/from the USB Device's
     * default endpoint (usually 0)
     * @param direction The direction of the transfer. Determined by the vendor-specific
     * request type byte and bitwise or operation on 128 for IN, 0 for OUT
     * @param requestID 0 for now TODO(figure out what this does)
     *
     */
    fun controlTransfer(
        direction: Int,
        requestID: Int,
        address: Int,
        index: Int,
        bytes: ByteArray,
        length: Int,
        timeout: Int
    ) : Int

    /**
     * This function should initiate instances anything needed to queue and submit a bulk transfer
     * request. For example, on Android this would entail creating a new UsbRequest and keeping the
     * request to use for later during [submitBulkTransfer].
     */
    fun prepareNewBulkTransfer(transferIndex: Int, byteBuffer: ByteBuffer)

    /**
     * This function should initiate and enqueue the bulk USB transfer at index [transferIndex] and
     * supply the given [ByteBuffer] [buffer] to be read into or written from. It is important that
     * this function doesn't manipulate, copy, or store any references to the supplied [ByteBuffer]
     */
    fun submitBulkTransfer(buffer: ByteBuffer)

    /**
     * This function should wait until a transfer is completed and the data has been written to the
     * [ByteBuffer]
     * @return the index as an Int of the transfer index that was submitted with [submitBulkTransfer]
     */
    fun waitForTransferResult(): Int

    /**
     * This function should take any steps necessary to claim exclusive access to the read/write
     * interface of the USB Device, including disabling the kernal drivers.
     */
    fun claimInterface()

    /**
     * This should release exclusive control of the Usb read/write interfaces that was obtained in
     * [claimInterface]. This is called in the close process.
     */
    fun releaseUsbDevice()

    fun shutdown()

}