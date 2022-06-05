package com.virginiaprivacy.sdr

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow

interface Receiver  {
    val input: ReceiveChannel<FloatArray>
}

abstract class RebroadcastingReceiver<T>: Receiver {
    abstract val output: Flow<T>
}