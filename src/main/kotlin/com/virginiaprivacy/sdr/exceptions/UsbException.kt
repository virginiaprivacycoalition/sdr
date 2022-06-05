package com.virginiaprivacy.sdr.exceptions

open class UsbException(message: String) : Exception(message) {
    val errorCode: Int? = null
}
