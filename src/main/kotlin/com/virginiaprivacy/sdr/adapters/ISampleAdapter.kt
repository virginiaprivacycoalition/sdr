package com.virginiaprivacy.sdr.adapters

fun interface ISampleAdapter<out T> {

    fun convert(bytes: ByteArray): T
}