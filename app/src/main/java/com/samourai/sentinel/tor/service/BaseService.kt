package com.samourai.sentinel.tor.service

import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import androidx.annotation.WorkerThread
import com.samourai.sentinel.BuildConfig
import io.matthewnelson.topl_core.broadcaster.BroadcastLogger
import com.samourai.sentinel.tor.notification.ServiceNotification
import com.samourai.sentinel.tor.prefs.TorServicePrefsListener
import com.samourai.sentinel.tor.service.components.actions.ServiceActionProcessor
import com.samourai.sentinel.tor.service.components.actions.ServiceActions
import com.samourai.sentinel.tor.service.components.actions.ServiceActions.ServiceAction
import com.samourai.sentinel.tor.service.components.binding.BaseServiceConnection
import com.samourai.sentinel.tor.service.components.binding.TorServiceBinder
import com.samourai.sentinel.tor.service.components.binding.TorServiceConnection
import com.samourai.sentinel.tor.util.ServiceConsts.ServiceActionName
import com.samourai.sentinel.tor.util.ServiceConsts.NotificationImage
import io.matthewnelson.topl_core.base.TorConfigFiles
import io.matthewnelson.topl_core.base.TorSettings
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.IOException

/**
 * Contains all methods that are called from classes external to, and instantiated by
 * [TorService]. It acts as the glue and helps with integration testing of the individual
 * components that make [TorService] work.
 * */
internal abstract class BaseService: Service() {

    companion object {
        private var application: Application? = null
        var buildConfigVersionCode: Int = -1
            private set
        var buildConfigDebug: Boolean = BuildConfig.DEBUG
            private set
        var geoipAssetPath: String = ""
            private set
        var geoip6AssetPath: String = ""
            private set
        lateinit var torConfigFiles: TorConfigFiles
        lateinit var torSettings: TorSettings

        fun initialize(
            application: Application,
            buildConfigVersionCode: Int,
            buildConfigDebug: Boolean,
            geoipAssetPath: String,
            geoip6AssetPath: String,
            torConfigFiles: TorConfigFiles,
            torSettings: TorSettings
        ) {
            Companion.application = application
            Companion.buildConfigVersionCode = buildConfigVersionCode
            Companion.buildConfigDebug = buildConfigDebug
            Companion.geoipAssetPath = geoipAssetPath
            Companion.geoip6AssetPath = geoip6AssetPath
            Companion.torConfigFiles = torConfigFiles
            Companion.torSettings = torSettings
        }

        @Throws(RuntimeException::class)
        fun getAppContext(): Context =
            application?.applicationContext ?: throw RuntimeException(
                "Builder.build has not been called yet"
            )

        // For things that can't be saved to TorServicePrefs, such as BuildConfig.VERSION_CODE
        fun getLocalPrefs(context: Context): SharedPreferences =
            context.getSharedPreferences("TorServiceLocalPrefs", Context.MODE_PRIVATE)


        ///////////////////////////////////
        /// Last Accepted ServiceAction ///
        ///////////////////////////////////
        @Volatile
        @ServiceActionName
        private var lastAcceptedServiceAction: String = ServiceActionName.STOP


        fun updateLastAcceptedServiceAction(@ServiceActionName serviceAction: String) {
            lastAcceptedServiceAction = serviceAction
        }
        fun wasLastAcceptedServiceActionStop(): Boolean =
            lastAcceptedServiceAction == ServiceActionName.STOP


        //////////////////////
        /// ServiceStartup ///
        //////////////////////

        /**
         * Starts the Service. Setting [includeIntentActionStart] to `false`, will not include
         * [ServiceActionName.START] in the Intent as an action so that [onStartCommand] knows
         * to set the [ServiceActions.Start.updateLastAction] to false. This allows for
         * distinguishing what is coming from the application (either by user input, or how
         * the application has the library implemented), and what is coming from this library.
         * It makes keeping the state of the service in sync with the application's desires.
         *
         * @param [context]
         * @param [serviceClass] The Service's class wanting to be started
         * @param [serviceConn] The [BaseServiceConnection] to bind to
         * @param [includeIntentActionStart] Boolean for including [ServiceActionName.START] as
         *   the Intent's Action.
         * */
        fun startService(
                context: Context,
                serviceClass: Class<*>,
                serviceConn: BaseServiceConnection,
                includeIntentActionStart: Boolean = true
        ) {
            val intent = Intent(context.applicationContext, serviceClass)
            if (includeIntentActionStart)
                intent.action = ServiceActionName.START
            context.applicationContext.startService(intent)
            context.applicationContext.bindService(intent, serviceConn, Context.BIND_AUTO_CREATE)
        }

        /**
         * Unbinds [TorService] from the Application and clears the reference to
         * [BaseServiceConnection.serviceBinder].
         *
         * @param [context] [Context]
         * @param [serviceConn] The [BaseServiceConnection] to unbind
         * @throws [IllegalArgumentException] If no binding exists for the provided [serviceConn]
         * */
        @Throws(IllegalArgumentException::class)
        fun unbindService(context: Context, serviceConn: BaseServiceConnection) {
            serviceConn.clearServiceBinderReference()
            context.applicationContext.unbindService(serviceConn)
        }
    }

    // All classes that interact with APIs which require Context to do something
    // call this in production (torService.context). This allows for easily
    // swapping it out with what we want to use when testing.
    abstract val context: Context


    ///////////////
    /// Binding ///
    ///////////////
    private val torServiceBinder: TorServiceBinder by lazy {
        TorServiceBinder(this)
    }

    open fun unbindTorService(): Boolean {
        return try {
            unbindService(context, TorServiceConnection.torServiceConnection)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    override fun onBind(intent: Intent?): IBinder? {
        return torServiceBinder
    }


    /////////////////////////
    /// BroadcastReceiver ///
    /////////////////////////
    abstract fun registerReceiver()
    abstract fun unregisterReceiver()


    //////////////////
    /// Coroutines ///
    //////////////////
    abstract fun getScopeIO(): CoroutineScope
    abstract fun getScopeMain(): CoroutineScope


    //////////////////////////////
    /// ServiceActionProcessor ///
    //////////////////////////////
    private val serviceActionProcessor by lazy {
        ServiceActionProcessor(this)
    }

    fun processServiceAction(serviceAction: ServiceAction) {
        serviceActionProcessor.processServiceAction(serviceAction)
    }
    open fun stopService() {
        stopSelf()
    }


    ///////////////////////////
    /// ServiceNotification ///
    ///////////////////////////
    private val serviceNotification: ServiceNotification
        get() = ServiceNotification.serviceNotification

    fun addNotificationActions() {
        serviceNotification.addActions(this)
    }
    fun removeNotificationActions() {
        serviceNotification.removeActions(this)
    }
    fun startForegroundService(): ServiceNotification {
        return serviceNotification.startForeground(this)
    }
    fun stopForegroundService(): ServiceNotification {
        return serviceNotification.stopForeground(this)
    }
    fun updateNotificationContentText(string: String) {
        serviceNotification.updateContentText(string)
    }
    fun updateNotificationContentTitle(title: String) {
        serviceNotification.updateContentTitle(title)
    }
    fun updateNotificationIcon(@NotificationImage notificationImage: Int) {
        serviceNotification.updateIcon(this, notificationImage)
    }
    fun updateNotificationProgress(show: Boolean, progress: Int?) {
        serviceNotification.updateProgress(show, progress)
    }


    /////////////////
    /// TOPL-Core ///
    /////////////////
    @WorkerThread
    @Throws(IOException::class)
    abstract fun copyAsset(assetPath: String, file: File)
    abstract fun getBroadcastLogger(clazz: Class<*>): BroadcastLogger
    abstract fun hasControlConnection(): Boolean
    abstract fun isTorOff(): Boolean
    abstract fun refreshBroadcastLoggersHasDebugLogsVar()
    @WorkerThread
    abstract fun signalControlConnection(torControlCommand: String): Boolean
    @WorkerThread
    abstract suspend fun signalNewNym()
    @WorkerThread
    abstract fun startTor()
    @WorkerThread
    abstract fun stopTor()


    ///////////////////////////////
    /// TorServicePrefsListener ///
    ///////////////////////////////
    private var torServicePrefsListener: TorServicePrefsListener? = null

    // TODO: register and unregister based on background/foreground state using
    //  Background manager
    private fun registerPrefsListener() {
        torServicePrefsListener?.unregister()
        torServicePrefsListener = TorServicePrefsListener(this)
    }
    private fun unregisterPrefsListener() {
        torServicePrefsListener?.unregister()
        torServicePrefsListener = null
    }




    override fun onCreate() {
        serviceNotification.buildNotification(this)
        registerPrefsListener()
    }

    override fun onDestroy() {
        unregisterPrefsListener()
        serviceNotification.remove()
    }

    /**
     * No matter what Intent comes in, it starts Tor. If the Intent comes with no Action,
     * it will not update [ServiceActionProcessor.lastServiceAction].
     *
     * @see [Companion.startService]
     * */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ServiceActionName.START)
            processServiceAction(ServiceActions.Start())
        else
            processServiceAction(ServiceActions.Start(updateLastServiceAction = false))

        return START_NOT_STICKY
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        // Cancel the BackgroundManager's coroutine if it's active so it doesn't execute
        torServiceBinder.cancelExecuteBackgroundPolicyJob()

        // Move to the foreground so we can properly shutdown w/o interrupting the
        // application's normal lifecycle (Context.startServiceForeground does... thus,
        // the complexity)
        startForegroundService()
    }
}