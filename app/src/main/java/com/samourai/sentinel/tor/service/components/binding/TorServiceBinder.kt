package com.samourai.sentinel.tor.service.components.binding

import android.os.Binder
import com.samourai.sentinel.tor.service.BaseService
import com.samourai.sentinel.tor.lifecycle.BackgroundManager
import com.samourai.sentinel.tor.service.components.actions.ServiceActions
import com.samourai.sentinel.tor.service.components.actions.ServiceActions.ServiceAction
import com.samourai.sentinel.tor.util.ServiceConsts.BackgroundPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


internal class TorServiceBinder(private val torService: BaseService): Binder() {

    /**
     * Accepts all [ServiceActions] except [ServiceActions.Start], which gets issued via
     * [io.matthewnelson.topl_service.service.TorService.onStartCommand].
     * */
    fun submitServiceAction(serviceAction: ServiceAction) {
        if (serviceAction is ServiceActions.Start) return
        torService.processServiceAction(serviceAction)
    }


    //////////////////////////////////////////
    /// BackgroundManager Policy Execution ///
    //////////////////////////////////////////
    private val bgMgrBroadcastLogger = torService.getBroadcastLogger(BackgroundManager::class.java)
    private var backgroundPolicyExecutionJob: Job? = null

    /**
     * Execution of a [BackgroundPolicy] takes place here in order to stay within the lifecycle
     * of [io.matthewnelson.topl_service.service.TorService] so that we prevent any potential
     * leaks from occurring.
     *
     * @param [policy] The [BackgroundPolicy] to be executed
     * @param [executionDelay] the time expressed in your [BackgroundManager.Builder.Policy]
     * */
    fun executeBackgroundPolicyJob(@BackgroundPolicy policy: String, executionDelay: Long) {
        cancelExecuteBackgroundPolicyJob()
        backgroundPolicyExecutionJob = torService.getScopeMain().launch {
            when (policy) {
                BackgroundPolicy.KEEP_ALIVE -> {
                    while (isActive && BaseServiceConnection.serviceBinder != null) {
                        delay(executionDelay)
                        if (isActive && BaseServiceConnection.serviceBinder != null) {
                            bgMgrBroadcastLogger.debug("Executing background management policy")
                            torService.stopForegroundService()
                            torService.startForegroundService()
                            torService.stopForegroundService()
                        }
                    }
                }
                BackgroundPolicy.RESPECT_RESOURCES -> {
                    delay(executionDelay)
                    bgMgrBroadcastLogger.debug("Executing background management policy")
                    torService.processServiceAction(
                        ServiceActions.Stop(updateLastServiceAction = false)
                    )
                }
            }
        }
    }

    /**
     * Cancels the coroutine executing the [BackgroundPolicy] if it is active.
     * */
    fun cancelExecuteBackgroundPolicyJob() {
        if (backgroundPolicyExecutionJob?.isActive == true) {
            backgroundPolicyExecutionJob?.let {
                it.cancel()
                bgMgrBroadcastLogger.debug("Execution has been cancelled")
            }
        }
    }
}