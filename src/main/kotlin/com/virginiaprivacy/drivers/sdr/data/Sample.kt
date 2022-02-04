package com.virginiaprivacy.drivers.sdr.data

data class SampleBuffer(val buffer: List<Byte>, val centerFrequency: Long, val sampleRate: Int) {

    private val maxOneWayRange = sampleRate / 2

    val data: Iterable<*> = buffer.toList()

    val frequencyRange = (centerFrequency - maxOneWayRange)..(centerFrequency + maxOneWayRange)

    constructor(buffer: ByteArray, centerFrequency: Long, sampleRate: Int) : this(
        buffer.asList(),
        centerFrequency,
        sampleRate
    )
}


