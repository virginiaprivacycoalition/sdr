package com.virginiaprivacy.drivers.sdr.plugins

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.Executors


fun interface Plugin<in T, out R> : (Flow<T>) -> Flow<R> {

}


val Plugin<*, *>.scope: CoroutineScope
    get() = CoroutineScope(Executors.newFixedThreadPool(1).asCoroutineDispatcher())








