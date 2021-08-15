package com.virginiaprivacy.drivers.sdr

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalStdlibApi
interface Plugin {

    fun onEachIntBuffer(intBuf: Iterable<Int>)

    val device: RTLDevice

    val scope: CoroutineScope
        get() = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    fun setup()

    fun run() {
        println("${this::class.qualifiedName} setup complete. . . Now running")
            device.unsignedIntFlow
                .onEach { onEachIntBuffer(it) }
                .launchIn(scope)

    }





}