package com.virginiaprivacy.drivers.sdr.usb

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer

interface UsbIFace {

    /**
     * The product name from the USB device descriptors
     */
    val productName: String

    /**
     * The serial number from the USB device descriptors
     */
    val serialNumber: String

    /**
     * The manufacturer name from the USB device descriptors
     */
    val manufacturerName: String



    fun readBytes(): Flow<ByteArray>

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
        address: Short,
        index: Short,
        bytes: ByteBuffer
    ): ControlTransferResult

    suspend fun bulkTransfer(
        bytes: ByteArray,
        length: Int
    ): Int

    /**
     * This function should initiate instances anything needed to queue and submit a bulk transfer
     * request. For example, on Android this would entail creating a new UsbRequest and keeping the
     * request to use for later during [submitBulkTransfer].
     */
    fun prepareNewBulkTransfer(byteBuffer: ByteBuffer)

    /**
     * This function should initiate and enqueue the bulk USB transfer at index [transferIndex] and
     * supply the given [ByteBuffer] [buffer] to be read into or written from. It is important that
     * this function doesn't manipulate, copy, or store any references to the supplied [ByteBuffer]
     */
    fun submitBulkTransfer()


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

    fun resetDevice()

}

