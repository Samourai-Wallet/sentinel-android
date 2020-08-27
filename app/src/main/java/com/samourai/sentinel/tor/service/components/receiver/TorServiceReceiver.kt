package com.samourai.sentinel.tor.service.components.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.samourai.sentinel.tor.service.BaseService
import com.samourai.sentinel.tor.service.TorService
import com.samourai.sentinel.tor.service.components.actions.ServiceActions
import com.samourai.sentinel.tor.service.components.binding.BaseServiceConnection
import com.samourai.sentinel.tor.util.ServiceConsts.ServiceActionName
import java.math.BigInteger
import java.security.SecureRandom

internal class TorServiceReceiver(private val torService: BaseService): BroadcastReceiver() {

    companion object {
        // Secures the intent filter at each application startup.
        // Also serves as the key to string extras containing the ServiceAction to be executed.
        val SERVICE_INTENT_FILTER: String = BigInteger(130, SecureRandom()).toString(32)

        @Volatile
        var isRegistered = false
            private set
    }

    private val broadcastLogger = torService.getBroadcastLogger(TorServiceReceiver::class.java)

    fun register() {
        torService.context.applicationContext.registerReceiver(
            this, IntentFilter(SERVICE_INTENT_FILTER)
        )
        if (!isRegistered)
            broadcastLogger.debug("Has been registered")
        isRegistered = true
    }

    fun unregister() {
        if (isRegistered) {
            try {
                torService.context.applicationContext.unregisterReceiver(this)
                isRegistered = false
                broadcastLogger.debug("Has been unregistered")
            } catch (e: IllegalArgumentException) {
                broadcastLogger.exception(e)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            // Only accept Intents from this package.
            if (context.applicationInfo.dataDir != torService.context.applicationInfo.dataDir) return

            when (val serviceAction = intent.getStringExtra(SERVICE_INTENT_FILTER)) {
                ServiceActionName.NEW_ID -> {
                    torService.processServiceAction(ServiceActions.NewId())
                }
                ServiceActionName.RESTART_TOR -> {
                    torService.processServiceAction(ServiceActions.RestartTor())
                }
                ServiceActionName.STOP -> {
                    torService.processServiceAction(ServiceActions.Stop())
                }
                else -> {
                    broadcastLogger.warn(
                        "This class does not accept $serviceAction as an argument."
                    )
                }
            }
        }
    }
}