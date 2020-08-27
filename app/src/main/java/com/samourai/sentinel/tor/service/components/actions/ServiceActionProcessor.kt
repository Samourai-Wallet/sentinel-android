package com.samourai.sentinel.tor.service.components.actions

import com.samourai.sentinel.tor.service.BaseService
import com.samourai.sentinel.tor.service.components.actions.ServiceActions.ServiceAction
import com.samourai.sentinel.tor.util.ServiceConsts
import kotlinx.coroutines.*

internal class ServiceActionProcessor(private val torService: BaseService): ServiceConsts() {

    companion object {
        var restartTorDelayTime = 500L
            private set
        var stopServiceDelayTime = 100L
            private set

        fun initialize(restartMilliseconds: Long, stopServiceMilliseconds: Long) {
            restartTorDelayTime = restartMilliseconds
            stopServiceDelayTime = stopServiceMilliseconds
        }

        //////////////////////////
        /// Last ServiceAction ///
        //////////////////////////
        @Volatile
        @ServiceActionName
        private var lastServiceAction: String = ServiceActionName.STOP

        fun wasLastAcceptedServiceActionStop(): Boolean =
            lastServiceAction == ServiceActionName.STOP
    }

    private val broadcastLogger = torService.getBroadcastLogger(ServiceActionProcessor::class.java)

    fun processServiceAction(serviceAction: ServiceAction) {
        when (serviceAction) {
            is ServiceActions.Stop -> {
                torService.unbindTorService()
                torService.unregisterReceiver()
                clearActionQueue()
                broadcastLogger.notice(serviceAction.name)
            }
            is ServiceActions.Start -> {
                clearActionQueue()
                torService.stopForegroundService()
                torService.registerReceiver()
            }
        }

        if (serviceAction.updateLastAction)
            lastServiceAction = serviceAction.name

        if (addActionToQueue(serviceAction))
            launchProcessQueueJob()
    }

    private fun broadcastDebugMsgWithObjectDetails(prefix: String, something: Any) {
        broadcastLogger.debug(
            "$prefix${something.javaClass.simpleName}@${something.hashCode()}"
        )
    }


    ////////////////////
    /// Action Queue ///
    ////////////////////
    private val actionQueueLock = Object()
    private val actionQueue = mutableListOf<ServiceAction>()

    private fun addActionToQueue(serviceAction: ServiceAction): Boolean =
        synchronized(actionQueueLock) {
            return if (actionQueue.add(serviceAction)) {
                broadcastDebugMsgWithObjectDetails(
                    "Added to queue: ServiceAction.", serviceAction
                )
                true
            } else {
                false
            }
        }

    private fun removeActionFromQueue(serviceAction: ServiceAction) =
        synchronized(actionQueueLock) {
            if (actionQueue.remove(serviceAction))
                broadcastDebugMsgWithObjectDetails(
                    "Removed from queue: ServiceAction.", serviceAction
                )
        }

    private fun clearActionQueue() =
        synchronized(actionQueueLock) {
            if (!actionQueue.isNullOrEmpty()) {
                actionQueue.clear()
                broadcastLogger.debug("Queue cleared")
            }
        }


    ////////////////////////
    /// Queue Processing ///
    ////////////////////////
    private var processQueueJob: Job? = null

    private fun launchProcessQueueJob() {
        if (processQueueJob?.isActive == true) return
        processQueueJob = torService.getScopeIO().launch {
            broadcastDebugMsgWithObjectDetails("Processing Queue: ", this)

            while (actionQueue.isNotEmpty() && isActive) {
                val serviceAction = actionQueue.elementAtOrNull(0)
                if (serviceAction == null) {
                    return@launch
                } else {
                    broadcastLogger.notice(serviceAction.name)
                    serviceAction.commands.forEachIndexed { index, command ->

                        // Check if the current actionObject being executed has been
                        // removed from the queue before executing it's next command.
                        if (actionQueue.elementAtOrNull(0) != serviceAction) {
                            broadcastDebugMsgWithObjectDetails(
                                "Interrupting execution of: ServiceAction.", serviceAction
                            )
                            return@forEachIndexed
                        }

                        when (command) {
                            ServiceActionCommand.DELAY -> {
                                val delayLength = serviceAction.consumeDelayLength()
                                if (delayLength > 0L)
                                    delay(delayLength)
                            }
                            ServiceActionCommand.NEW_ID -> {
                                torService.signalNewNym()
                            }
                            ServiceActionCommand.START_TOR -> {
                                if (!torService.hasControlConnection()) {
                                    torService.startTor()
                                    delay(300L)
                                }
                            }
                            ServiceActionCommand.STOP_SERVICE -> {
                                broadcastDebugMsgWithObjectDetails("Stopping: ", torService)
                                torService.stopService()
                            }
                            ServiceActionCommand.STOP_TOR -> {
                                if (torService.hasControlConnection()) {
                                    torService.stopTor()
                                    delay(300L)
                                }
                            }
                        }

                        if (index == serviceAction.commands.lastIndex)
                            removeActionFromQueue(serviceAction)
                    }
                }
            }
        }
    }
}