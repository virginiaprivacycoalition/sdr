package com.virginiaprivacy.drivers.sdr

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalStdlibApi
interface Plugin {

    fun getForEachBuffer(bytes: ByteArray)

    val device: RTLDevice

    val scope: CoroutineScope
        get() = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    fun getByteFlow() = device

    fun setup()

    fun run() {
        println("${this::class.qualifiedName} setup complete. . . Now running")
            device.flow.onEach {
                    getForEachBuffer(it)
            }
                .launchIn(scope)

    }





}