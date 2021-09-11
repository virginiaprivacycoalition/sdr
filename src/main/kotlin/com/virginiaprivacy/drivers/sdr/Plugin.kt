package com.virginiaprivacy.drivers.sdr

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalStdlibApi
interface Plugin {

    val device: RTLDevice

    val scope: CoroutineScope
        get() = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    fun setup()

    fun processBuffer(buf: ByteArray)

    fun run() {
        println("${this::class.qualifiedName} setup complete. . . Now running")
        scope.launch {
            device.rawFlow
                .collect {
                    processBuffer(buf = it)
                }
        }
    }





}