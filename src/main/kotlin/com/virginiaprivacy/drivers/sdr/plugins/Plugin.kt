@file:OptIn(ExperimentalStdlibApi::class)

package com.virginiaprivacy.drivers.sdr.plugins

import com.virginiaprivacy.drivers.sdr.RTLDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors


fun interface Plugin {


    suspend fun processSignalBuffer(buf: ByteArray)

}

val Plugin.device: RTLDevice
    get() = TODO("Not yet implemented")

val Plugin.scope: CoroutineScope
    get() = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

fun Plugin.run() {
    println("${this::class.qualifiedName} setup complete. . . Now running. test version")
    scope.launch {
        device.rawFlow
            .collect {
                launch {
                    processSignalBuffer(buf = it)
                }
            }
    }
}


