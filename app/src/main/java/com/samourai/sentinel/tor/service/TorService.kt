package com.samourai.sentinel.tor.service

import android.content.*
import android.os.IBinder
import androidx.annotation.WorkerThread
import io.matthewnelson.topl_core.OnionProxyManager
import io.matthewnelson.topl_core.broadcaster.BroadcastLogger
import io.matthewnelson.topl_core.util.FileUtilities
import com.samourai.sentinel.tor.TorServiceController
import com.samourai.sentinel.tor.service.components.actions.ServiceActions
import com.samourai.sentinel.tor.service.components.onionproxy.ServiceEventBroadcaster
import com.samourai.sentinel.tor.service.components.onionproxy.ServiceEventListener
import com.samourai.sentinel.tor.service.components.onionproxy.ServiceTorInstaller
import com.samourai.sentinel.tor.service.components.onionproxy.ServiceTorSettings
import com.samourai.sentinel.tor.service.components.receiver.TorServiceReceiver
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException

internal class TorService: BaseService() {

    override val context: Context
        get() = this


    ///////////////
    /// Binding ///
    ///////////////
    override fun unbindTorService(): Boolean {
        val boolean = super.unbindTorService()
        if (boolean)
            broadcastLogger.debug("Has been unbound")
        return boolean
    }
    override fun onBind(intent: Intent?): IBinder? {
        broadcastLogger.debug("Has been bound")
        return super.onBind(intent)
    }


    /////////////////////////
    /// BroadcastReceiver ///
    /////////////////////////
    private val torServiceReceiver by lazy {
        TorServiceReceiver(this)
    }
    override fun registerReceiver() {
        torServiceReceiver.register()
    }
    override fun unregisterReceiver() {
        torServiceReceiver.unregister()
    }


    //////////////////
    /// Coroutines ///
    //////////////////
    private val supervisorJob = SupervisorJob()
    private val scopeIO = CoroutineScope(Dispatchers.IO + supervisorJob)
    private val scopeMain = CoroutineScope(Dispatchers.Main + supervisorJob)

    override fun getScopeIO(): CoroutineScope {
        return scopeIO
    }
    override fun getScopeMain(): CoroutineScope {
        return scopeMain
    }

    private val broadcastLogger: BroadcastLogger by lazy {
        getBroadcastLogger(TorService::class.java)
    }
    private val onionProxyManager: OnionProxyManager by lazy {
        OnionProxyManager(
            context,
            TorServiceController.getTorConfigFiles(),
            ServiceTorInstaller(this),
            ServiceTorSettings(this, TorServiceController.getTorSettings()),
            ServiceEventListener(),
            ServiceEventBroadcaster(this),
            buildConfigDebug
        )
    }

    @WorkerThread
    @Throws(IOException::class)
    override fun copyAsset(assetPath: String, file: File) {
        try {
            FileUtilities.copy(context.assets.open(assetPath), file.outputStream())
        } catch (e: Exception) {
            throw IOException("Failed copying asset from $assetPath", e)
        }
    }
    override fun getBroadcastLogger(clazz: Class<*>): BroadcastLogger {
        return onionProxyManager.getBroadcastLogger(clazz)
    }
    override fun hasControlConnection(): Boolean {
        return onionProxyManager.hasControlConnection
    }
    override fun isTorOff(): Boolean {
        return onionProxyManager.torStateMachine.isOff
    }
    override fun refreshBroadcastLoggersHasDebugLogsVar() {
        onionProxyManager.refreshBroadcastLoggersHasDebugLogsVar()
    }
    @WorkerThread
    override fun signalControlConnection(torControlCommand: String): Boolean {
        return onionProxyManager.signalControlConnection(torControlCommand)
    }
    @WorkerThread
    override suspend fun signalNewNym() {
        onionProxyManager.signalNewNym()
    }
    @WorkerThread
    override fun startTor() {
        try {
            onionProxyManager.setup()
            generateTorrcFile()

            onionProxyManager.start()

        } catch (e: Exception) {
            broadcastLogger.exception(e)
        }
    }
    @WorkerThread
    override fun stopTor() {
        try {
            onionProxyManager.stop()
        } catch (e: Exception) {
            broadcastLogger.exception(e)
        }
    }
    @WorkerThread
    @Throws(
        SecurityException::class,
        IllegalAccessException::class,
        IllegalArgumentException::class,
        InvocationTargetException::class,
        NullPointerException::class,
        ExceptionInInitializerError::class,
        IOException::class
    )
    private fun generateTorrcFile() {
        onionProxyManager.getNewSettingsBuilder()
            .updateTorSettings()
            .setGeoIpFiles()
            .finishAndWriteToTorrcFile()

    }


    ///////////////////////////////
    /// TorServicePrefsListener ///
    ///////////////////////////////
    override fun onDestroy() {
        super.onDestroy()
        supervisorJob.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        broadcastLogger.debug("Task has been removed")

        // Shutdown Tor and stop the Service.
        processServiceAction(ServiceActions.Stop(updateLastServiceAction = false))
    }
}