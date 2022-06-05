package com.virginiaprivacy.sdr.adapters

class ByteToFloatSampleAdapter : ISampleAdapter<FloatArray> {

    override fun convert(bytes: ByteArray): FloatArray {
        val convertedSamples = FloatArray(bytes.size)
        bytes.indices.forEach {
            convertedSamples[it] = LOOKUP_VALUES[bytes[it].toInt() and 255]
        }
        return convertedSamples
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