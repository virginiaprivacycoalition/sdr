package com.virginiaprivacy.sdr.adapters

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

class ByteToDoubleSampleAdapter : ISampleAdapter<DoubleArray> {
    override fun convert(samples: ReceiveChannel<ByteArray>): Flow<DoubleArray> {
        return samples.receiveAsFlow().map { bytes ->
            val output = DoubleArray(bytes.size)
            bytes.forEachIndexed { i, byte ->
                output[i] = LUT[byte.toInt().and(255)]
            }
            output
        }
    }

    companion object {
        private val LUT = DoubleArray(256) {
            (it - 127) / 128.0
        }
    }



}