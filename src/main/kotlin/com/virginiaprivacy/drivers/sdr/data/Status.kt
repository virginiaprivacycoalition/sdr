package com.virginiaprivacy.drivers.sdr.data

import com.virginiaprivacy.drivers.sdr.IOStatus
import com.virginiaprivacy.drivers.sdr.precision
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

object Status {

    private val ioStatus = MutableStateFlow(IOStatus.EXIT)

    private val readSpeed = MutableStateFlow<Double>(0.0)

    private val processSpeed = MutableStateFlow<Double>(0.0)

    @Volatile
    var bytesRead: Long = 0
        set(value) {
            field = value
            updateReadSpeed(field)
        }

    @Volatile
    var bytesProcessed: Long = 0
        set(value) {
            field = value
            updateProcessingSpeed(field)
        }

    @Volatile
    var startTime: Long = 0

    @Volatile
    var pluginStartTime: Long = 0

    fun setIOStatus(ioStatus: IOStatus) {
        this.ioStatus.tryEmit(ioStatus)
    }

    fun getIOStatus() = ioStatus.asStateFlow()

    private fun updateReadSpeed(long: Long) { readSpeed.value = ((long.toDouble() / runTime().toDouble()).precision()) }

    private fun updateProcessingSpeed(long: Long) {
        processSpeed.value = ((long.toDouble() / pluginRunTime()).precision())
    }

    fun readSpeed() = readSpeed.asSharedFlow()

    fun processingSpeed() = processSpeed.asSharedFlow()

    private fun runTime() =
        (System.currentTimeMillis() - startTime).div(1000).coerceAtLeast(1)

    private fun pluginRunTime() =
        (System.currentTimeMillis() - pluginStartTime).div(1000).coerceAtLeast(1)


}