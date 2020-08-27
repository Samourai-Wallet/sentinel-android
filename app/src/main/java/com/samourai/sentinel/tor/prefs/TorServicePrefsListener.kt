package com.samourai.sentinel.tor.prefs

import android.content.SharedPreferences
import com.samourai.sentinel.tor.service.BaseService
import com.samourai.sentinel.tor.service.TorService
import com.samourai.sentinel.tor.util.ServiceConsts.PrefKeyBoolean
import io.matthewnelson.topl_service.prefs.TorServicePrefs

/**
 * Listens to [TorServicePrefs] for changes such that while Tor is running, it can
 * query [TorService.onionProxyManager] to have it updated immediately (if the setting doesn't
 * require a restart), or submit [io.matthewnelson.topl_service.util.ServiceConsts.ServiceActionName]'s
 * to [io.matthewnelson.topl_service.service.components.actions.ServiceActionProcessor] to be
 * queued for execution.
 *
 * @param [torService] To instantiate [TorServicePrefs]
 * */
internal class TorServicePrefsListener(
    private val torService: BaseService
): SharedPreferences.OnSharedPreferenceChangeListener {

    private val torServicePrefs = TorServicePrefs(torService.context)
    private val broadcastLogger = torService.getBroadcastLogger(TorServicePrefsListener::class.java)

    init {
        torServicePrefs.registerListener(this)
        broadcastLogger.debug("Has been registered")
    }

    /**
     * Called from [TorService.onDestroy].
     * */
    fun unregister() {
        torServicePrefs.unregisterListener(this)
        broadcastLogger.debug("Has been unregistered")
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (!key.isNullOrEmpty()) {
            broadcastLogger.debug("$key was modified")
            when (key) {
                PrefKeyBoolean.HAS_DEBUG_LOGS -> {
                    //  .
                    //  Especially necessary if I switch Tor's debug output location from
                    //  SystemOut to log to a file (more secure).
                    //  .
                    //  Will need to create another class available to Library user
                    //  strictly for Tor logs if logging to a file, such that they can
                    //  easily query, read, and load them to views.
                    //  Maybe a `TorDebugLogHelper` class?
                    //  .
                    //  Will need some way of automatically clearing old log files, too.
                    if (!torService.isTorOff())
                        torService.refreshBroadcastLoggersHasDebugLogsVar()
                }
            }
        }
    }

}