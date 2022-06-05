package com.virginiaprivacy.sdr.usb

import java.nio.charset.Charset

class Descriptor(data: ByteArray) {
    lateinit var mData: ByteArray
    private val mLabels = mutableListOf<String>()

    init {
        mData = data
        labels
    }

    val isValid: Boolean
        get() = mData[0].toInt() == 40 && mData[1].toInt() == 50
    val vendorID: String
        get() {
            val id = Integer.rotateLeft(255 and mData[3].toInt(), 8) or (255 and mData[2].toInt())
            return String.format("%04X", id)
        }
    val vendorLabel: String
        get() = mLabels[0]
    val productID: String
        get() {
            val id = Integer.rotateLeft(255 and mData[5].toInt(), 8) or (255 and mData[4].toInt())
            return String.format("%04X", id)
        }
    val productLabel: String
        get() = mLabels[1]

    fun hasSerial(): Boolean {
        return mData[6].toInt() == -91
    }

    val serial: String
        get() = mLabels[2]

    fun remoteWakeupEnabled(): Boolean {
        val mask: Byte = 1
        return mData[7].toInt() and mask.toInt() == mask.toInt()
    }

    fun irEnabled(): Boolean {
        val mask: Byte = 2
        return mData[7].toInt() and mask.toInt() == mask.toInt()
    }

    private val labels: Unit
        get() {
            mLabels.clear()
            var start = 9
            while (start < 256) {
                start = getLabel(start)
            }
        }

    private fun getLabel(start: Int): Int {
        return if (start <= 254 && mData[start + 1].toInt() == 3) {
            val length = 255 and mData[start].toInt()
            if (start + length > 255) {
                256
            } else {
                val data = mData.copyOfRange(start + 2, start + length)
                val label = String(data, Charset.forName("UTF-16LE"))
                mLabels.add(label)
                start + length
            }
        } else {
            256
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("RTL-2832 EEPROM Descriptor\n")
        sb.append("Vendor: ")
        sb.append(vendorID)
        sb.append(" [")
        sb.append(vendorLabel)
        sb.append("]\n")
        sb.append("Product: ")
        sb.append(productID)
        sb.append(" [")
        sb.append(productLabel)
        sb.append("]\n")
        sb.append("Serial: ")
        if (hasSerial()) {
            sb.append("yes [")
            sb.append(serial)
            sb.append("]\n")
        } else {
            sb.append("no\n")
        }
        sb.append("Remote Wakeup Enabled: ")
        sb.append(if (remoteWakeupEnabled()) "yes" else "no")
        sb.append("\n")
        sb.append("IR Enabled: ")
        sb.append(if (irEnabled()) "yes" else "no")
        sb.append("\n")
        if (mLabels.size > 3) {
            sb.append("Additional Labels: ")
            for (x in 3 until mLabels.size) {
                sb.append(" [")
                sb.append(mLabels[x])
                sb.append("\n")
            }
        }
        return sb.toString()
    }
}