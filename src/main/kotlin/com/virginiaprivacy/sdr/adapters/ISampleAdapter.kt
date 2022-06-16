package com.virginiaprivacy.sdr.adapters

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow

fun interface ISampleAdapter<out T> {

    fun convert(samples: ReceiveChannel<ByteArray>): Flow<T>
}