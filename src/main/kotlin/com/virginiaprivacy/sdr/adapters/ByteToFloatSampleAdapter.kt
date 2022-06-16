package com.virginiaprivacy.sdr.adapters

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

class ByteToFloatSampleAdapter : ISampleAdapter<FloatArray> {

    override fun convert(samples: ReceiveChannel<ByteArray>): Flow<FloatArray> {
        return samples.receiveAsFlow().map { bytes ->
            val convertedSamples = FloatArray(bytes.size)
            bytes.indices.forEach {
                convertedSamples[it] = LOOKUP_VALUES[bytes[it].toInt() and 255]
            }
            convertedSamples
        }
    }

    companion object {
        private val LOOKUP_VALUES = FloatArray(256)

        init {
            for (x in 0..255) {
                LOOKUP_VALUES[x] = (x - 127).toFloat() / 128.0f
            }
        }
    }
}