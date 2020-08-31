package com.samourai.sentinel.util

import kotlinx.coroutines.*

/**
 * sentinel-android
 *
 * @author Sarath
 */

val apiJob = SupervisorJob()
val apiScope: CoroutineScope = CoroutineScope(context = Dispatchers.IO) + apiJob
val dataBaseScope: CoroutineScope = CoroutineScope(context = Dispatchers.IO)+ SupervisorJob()
