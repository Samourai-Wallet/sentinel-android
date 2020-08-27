
package com.samourai.sentinel.tor.service.components.onionproxy

import io.matthewnelson.topl_core.OnionProxyManager
import com.samourai.sentinel.tor.TorServiceController
import com.samourai.sentinel.tor.service.BaseService
import com.samourai.sentinel.tor.service.components.actions.ServiceActionProcessor
import io.matthewnelson.topl_core.base.EventBroadcaster
import com.samourai.sentinel.tor.service.TorService
import com.samourai.sentinel.tor.util.ServiceConsts.ServiceActionName
import com.samourai.sentinel.tor.util.ServiceConsts.NotificationImage
import com.samourai.sentinel.tor.util.ServiceUtilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.freehaven.tor.control.TorControlCommands

internal class ServiceEventBroadcaster(private val torService: BaseService): EventBroadcaster() {

    private val scopeMain: CoroutineScope
        get() = torService.getScopeMain()

    /////////////////
    /// Bandwidth ///
    /////////////////
    @Volatile
    private var bytesRead = 0L
    @Volatile
    private var bytesWritten = 0L

    override fun broadcastBandwidth(bytesRead: String, bytesWritten: String) {
        val read =
            try {
                bytesRead.toLong()
            } catch (e: NumberFormatException) {
                this.bytesRead
            }
        val written =
            try {
                bytesWritten.toLong()
            } catch (e: NumberFormatException) {
                this.bytesWritten
            }

        // Only update the notification if proper State is had & we're bootstrapped.
        if (torState == TorState.ON &&
            torNetworkState == TorNetworkState.ENABLED &&
            isBootstrappingComplete()
        ) {
            if (read != this.bytesRead || written != this.bytesWritten) {
                this.bytesRead = read
                this.bytesWritten = written

                updateBandwidth(read, written)

                if (read == 0L && written == 0L)
                    torService.updateNotificationIcon(NotificationImage.ENABLED)
                else
                    torService.updateNotificationIcon(NotificationImage.DATA)
            }
        }

        TorServiceController.appEventBroadcaster?.let {
            scopeMain.launch { it.broadcastBandwidth(bytesRead, bytesWritten) }
        }
    }

    /**
     * Do a check for if a message is being displayed in the contentText of the
     * notification, allowing it to remain there unabated until the coroutine
     * finishes.
     * */
    private fun updateBandwidth(download: Long, upload: Long) {
        if (noticeMsgToContentTextJob?.isActive == true) return
        torService.updateNotificationContentText(
            ServiceUtilities.getFormattedBandwidthString(download, upload)
        )
    }


    /////////////
    /// Debug ///
    /////////////
    override fun broadcastDebug(msg: String) {
        TorServiceController.appEventBroadcaster?.let {
            scopeMain.launch { it.broadcastDebug(msg) }
        }
    }


    //////////////////
    /// Exceptions ///
    //////////////////
    override fun broadcastException(msg: String?, e: Exception) {
        if (!msg.isNullOrEmpty()) {
            if (msg.contains(TorService::class.java.simpleName)) {
                torService.updateNotificationIcon(NotificationImage.ERROR)
                val msgSplit = msg.split("|")
                msgSplit.elementAtOrNull(2)?.let {
                    torService.updateNotificationContentText(it)
                    torService. updateNotificationProgress(false, null)
                }
            }
        }

        TorServiceController.appEventBroadcaster?.let {
            scopeMain.launch { it.broadcastException(msg, e) }
        }
    }


    ///////////////////
    /// LogMessages ///
    ///////////////////
    override fun broadcastLogMessage(logMessage: String?) {
        TorServiceController.appEventBroadcaster?.let {
            scopeMain.launch { it.broadcastLogMessage(logMessage) }
        }
    }


    ///////////////
    /// Notices ///
    ///////////////
    private var noticeMsgToContentTextJob: Job? = null

    @Volatile
    private var bootstrapProgress = ""
    private fun isBootstrappingComplete(): Boolean =
        bootstrapProgress == "Bootstrapped 100%"

    @Volatile
    private var controlPort: String? = null
    @Volatile
    private var httpTunnelPort: String? = null
    @Volatile
    private var socksPort: String? = null

    override fun broadcastNotice(msg: String) {

        when {
            // ServiceActionProcessor
            msg.contains(ServiceActionProcessor::class.java.simpleName) -> {
                handleServiceActionProcessorMsg(msg)
            }
            // BOOTSTRAPPED
            msg.contains("Bootstrapped") -> {
                handleBootstrappedMsg(msg)
            }
            // Control Port
            msg.contains("Successfully connected to Control Port:") -> {
                handleControlPortMsg(msg)
            }
            // Http Tunnel Port
            msg.contains("Opened HTTP tunnel listener on ") -> {
                handleHttpTunnelPortMsg(msg)
            }
            // Socks Port
            msg.contains("Opened Socks listener on ") -> {
                handleSocksPortMsg(msg)
            }
            // NEWNYM
            msg.contains(TorControlCommands.SIGNAL_NEWNYM) -> {
                handleNewNymMsg(msg)
            }
        }

        TorServiceController.appEventBroadcaster?.let {
            scopeMain.launch { it.broadcastNotice(msg) }
        }
    }

    // NOTICE|BaseEventListener|Bootstrapped 5% (conn): Connecting to a relay
    private fun handleBootstrappedMsg(msg: String) {
        val msgSplit = msg.split(" ")
        msgSplit.elementAtOrNull(2)?.let {
            val bootstrapped = "${msgSplit[0]} ${msgSplit[1]}".split("|")[2]

            if (bootstrapped != bootstrapProgress) {
                torService.updateNotificationContentText(bootstrapped)

                if (bootstrapped == "Bootstrapped 100%") {
                    updateAppEventBroadcasterWithPortInfo()
                    torService.updateNotificationIcon(NotificationImage.ENABLED)
                    torService.updateNotificationProgress(true, 100)
                    torService.updateNotificationProgress(false, null)
                    torService.addNotificationActions()
                } else {
                    val progress: Int? = try {
                        bootstrapped.split(" ")[1].split("%")[0].toInt()
                    } catch (e: Exception) {
                        null
                    }
                    progress?.let {
                        torService.updateNotificationProgress(true, progress)
                    }
                }

                bootstrapProgress = bootstrapped
            }
        }
    }

    // NOTICE|OnionProxyManager|Successfully connected to Control Port: 44201
    private fun handleControlPortMsg(msg: String) {
        val port = msg.split(":")[1].trim()
        controlPort = "127.0.0.1:$port"
    }

    // NOTICE|BaseEventListener|Opened HTTP tunnel listener on 127.0.0.1:37397
    private fun handleHttpTunnelPortMsg(msg: String) {
        val port = msg.split(":")[1].trim()
        httpTunnelPort = "127.0.0.1:$port"
    }

    // NOTICE|BaseEventListener|Opened Socks listener on 127.0.0.1:9051
    private fun handleSocksPortMsg(msg: String) {
        val port = msg.split(":")[1].trim()
        socksPort = "127.0.0.1:$port"
    }

    private fun handleNewNymMsg(msg: String) {
        val msgToShow: String? =
            when {
                msg.contains(OnionProxyManager.NEWNYM_SUCCESS_MESSAGE) -> {
                    OnionProxyManager.NEWNYM_SUCCESS_MESSAGE
                }
                msg.contains(OnionProxyManager.NEWNYM_NO_NETWORK) -> {
                    OnionProxyManager.NEWNYM_NO_NETWORK
                }
                else -> {
                    val msgSplit = msg.split("|")
                    msgSplit.elementAtOrNull(2)
                }
            }

        if (noticeMsgToContentTextJob?.isActive == true)
            noticeMsgToContentTextJob?.cancel()

        msgToShow?.let {
            displayMessageToContentText(it, 3500L)
        }
    }

    private fun handleServiceActionProcessorMsg(msg: String) {
        val msgSplit = msg.split("|")
        val msgToShow: String? = msgSplit.elementAtOrNull(2)?.let {
            when (it) {
                ServiceActionName.RESTART_TOR -> {
                    "Restarting Tor..."
                }
                ServiceActionName.STOP -> {
                    "Stopping Service..."
                }
                else -> {
                    null
                }
            }
        }
        msgToShow?.let {
            torService.updateNotificationContentText(it)
        }
    }

    private fun updateAppEventBroadcasterWithPortInfo() {
        TorServiceController.appEventBroadcaster?.let {
            scopeMain.launch {
                it.broadcastControlPortAddress(controlPort)
                it.broadcastHttpPortAddress(httpTunnelPort)
                it.broadcastSocksPortAddress(socksPort)
            }
        }
    }

    /**
     * Display a message in the notification's ContentText space for the defined
     * [delayMilliSeconds], after which (if Tor is connected), publish to the Notification's
     * ContentText the most recently broadcast bandwidth via [bytesRead] && [bytesWritten].
     * */
    private fun displayMessageToContentText(message: String, delayMilliSeconds: Long) {
        noticeMsgToContentTextJob = scopeMain.launch {
            torService.updateNotificationContentText(message)
            delay(delayMilliSeconds)

            // Publish the last bandwidth broadcast to overwrite the message.
            if (torNetworkState == TorNetworkState.ENABLED) {
                torService.updateNotificationContentText(
                    ServiceUtilities.getFormattedBandwidthString(bytesRead, bytesWritten)
                )
            } else if (isBootstrappingComplete()){
                torService.updateNotificationContentText(
                    ServiceUtilities.getFormattedBandwidthString(0L, 0L)
                )
            }
        }
    }


    ////////////////
    /// TorState ///
    ////////////////
    @Volatile
    private var torState = TorState.OFF
    @Volatile
    private var torNetworkState = TorNetworkState.DISABLED

    override fun broadcastTorState(@TorState state: String, @TorNetworkState networkState: String) {
        if (torState == TorState.ON && state != torState) {
            bootstrapProgress = ""
            controlPort = null
            httpTunnelPort = null
            socksPort = null
            updateAppEventBroadcasterWithPortInfo()
            torService.removeNotificationActions()
        }

        if (state != TorState.ON)
            torService.updateNotificationProgress(true, null)

        torService.updateNotificationContentTitle(state)
        torState = state

        if (networkState == TorNetworkState.DISABLED) {
            // Update torNetworkState _before_ setting the icon to `disabled` so bandwidth won't
            // overwrite the icon with an update
            torNetworkState = networkState
            torService.updateNotificationIcon(NotificationImage.DISABLED)
        } else {
            if (isBootstrappingComplete())
                torService.updateNotificationIcon(NotificationImage.ENABLED)

            // Update torNetworkState _after_ setting the icon to `enabled` so bandwidth changes
            // occur afterwards and this won't overwrite ImageState.DATA
            torNetworkState = networkState
        }

        TorServiceController.appEventBroadcaster?.let {
            scopeMain.launch { it.broadcastTorState(state, networkState) }
        }
    }
}