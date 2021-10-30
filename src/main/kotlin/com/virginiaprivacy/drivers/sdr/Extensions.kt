package com.virginiaprivacy.drivers.sdr

import com.virginiaprivacy.drivers.sdr.plugins.Plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

fun Int.uint8(): Int {
    return if (this >= 127) {
        this - 127
    } else
        127 - this
}

val Boolean.int
    get() = if (this) 1 else 0

fun Int.mhz(): Int = this * 1000 * 1000

fun Int.khz(): Int = this * 1000

fun Double.precision(decimals: Int = 2): Double = "%.${decimals}f".format(this).toDouble()
fun String.precision(decimals: Int = 2): String = "%.${decimals}s".format(this)


val Plugin.scope: CoroutineScope
    get() = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

@ExperimentalStdlibApi
fun Plugin.run(device: RTLDevice) {
    println("${this::class.qualifiedName} setup complete. . . Now running")
    scope.launch {
        device.rawFlow
            .collect {
                processSignalBuffer(buf = it)
            }
    }
}
