package com.virginiaprivacy.sdr.adapters

class ByteToDoubleSampleAdapter : ISampleAdapter<DoubleArray> {
    override fun convert(bytes: ByteArray): DoubleArray {
        val output = DoubleArray(bytes.size)
        bytes.forEachIndexed { i, byte ->
            output[i] = LUT[byte.toInt().and(255)]
        }
        return output
    }

    companion object {
        private val LUT = DoubleArray(256) {
            (it - 127) / 128.0
        }
    }



}