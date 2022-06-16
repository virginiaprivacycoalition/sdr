package com.virginiaprivacy.sdr.adapters

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.experimental.and

class MagnitudeSampleAdapter : ISampleAdapter<DoubleArray> {
    override fun convert(samples: ReceiveChannel<ByteArray>): Flow<DoubleArray> =
        samples.receiveAsFlow().map { bytes ->
            val output = DoubleArray(bytes.size / 2)
            for (i in bytes.indices.step(2)) {
                output[i / 2] = (LUT[bytes[i].toInt().and(0xFF)] + LUT[bytes[i + 1].toInt().and(0xFF)]).toDouble()
            }
            output
        }

    companion object {
        val LUT = Array<Int>(256) {
            val j = if (it >= 127) it - 127 else 127 - it
            j * j
        }
    }
}