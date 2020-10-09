package com.samourai.sentinel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.samourai.sentinel.api.ApiService
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.core.SentinelState.Companion.torState
import com.samourai.sentinel.core.access.AccessFactory
import com.samourai.sentinel.data.db.SentinelCollectionStore
import com.samourai.sentinel.data.db.SentinelRoomDb
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.data.repository.ExchangeRateRepository
import com.samourai.sentinel.data.repository.TransactionsRepository
import com.samourai.sentinel.service.WebSocketHandler
import com.samourai.sentinel.tor.TorEventsReceiver
import com.samourai.sentinel.tor.prefs.SentinelTorSettings
import com.samourai.sentinel.ui.dojo.DojoUtility
import com.samourai.sentinel.ui.home.HomeActivity
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.util.MonetaryUtil
import com.samourai.sentinel.util.apiScope
import com.samourai.sentinel.util.dataBaseScope
import io.matthewnelson.topl_service.TorServiceController
import io.matthewnelson.topl_service.lifecycle.BackgroundManager
import io.matthewnelson.topl_service.notification.ServiceNotification
import kotlinx.coroutines.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy


class SentinelApplication : Application() {


    override fun onCreate() {
        super.onCreate()

        setUpChannels()
        initializeDI()
        setUpTor()


        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            val cacheDir = File(this.applicationContext.cacheDir.toURI())
            if (!cacheDir.exists()) {
                cacheDir.mkdir()
            }
            Timber.plant(CrashReportingTree(cacheDir))
        }
    }

    /**
     * Koin
     */
    private fun initializeDI() {

        val appModule = module {
            single { PrefsUtil(applicationContext) }
            single { DojoUtility() }
            single { AccessFactory.getInstance(null) }
            single { SentinelCollectionStore() }
            single { MonetaryUtil.getInstance() }
            single { CollectionRepository() }
            single { ApiService() }
            single { ExchangeRateRepository() }
            single { TransactionsRepository() }
            single { WebSocketHandler() }
            factory { SentinelRoomDb.getDatabase(applicationContext).txDao() }
            factory { SentinelRoomDb.getDatabase(applicationContext).utxoDao() }
        }

        startKoin {
            androidContext(this@SentinelApplication)
            modules(appModule)
        }
    }


    private fun setUpTor() {
        TorServiceController.Builder(
                application = this,
                torServiceNotificationBuilder = getTorNotificationBuilder(),
                backgroundManagerPolicy = gePolicyManager(),
                buildConfigVersionCode = BuildConfig.VERSION_CODE,
                // Can instantiate directly here then access it from
                torSettings = SentinelTorSettings(),
                geoipAssetPath = "common/geoip",
                geoip6AssetPath = "common/geoip6"
        )
                .addTimeToRestartTorDelay(milliseconds = 100L)
                .addTimeToStopServiceDelay(milliseconds = 100L)
                .setEventBroadcaster(TorEventsReceiver())
                .setBuildConfigDebug(buildConfigDebug = BuildConfig.DEBUG)
                .build()

        TorServiceController.appEventBroadcaster?.let {

            (it as TorEventsReceiver).liveTorState.observeForever { torState ->
                when (torState.state) {
                    "Tor: Off" -> {
                        SentinelState.torState = SentinelState.TorState.OFF
                    }
                    "Tor: Starting" -> {
                        SentinelState.torState = SentinelState.TorState.WAITING
                    }
                    "Tor: Stopping" -> {
                        SentinelState.torState = SentinelState.TorState.WAITING
                    }
                }
            }
            it.torLogs.observeForever { log ->
                if (log.contains("Bootstrapped 100%")) {
                    it.torPortInfo.value?.socksPort?.let { it1 -> createProxy(it1) }
                    torState = SentinelState.TorState.ON
                }
            }
            it.torPortInfo.observeForever { torInfo ->
                torInfo.socksPort?.let { port ->
                    createProxy(port)
                }
            }
        }
        if (SentinelState.isTorRequired()) {
            TorServiceController.startTor()
        }
    }


    private fun createProxy(proxyUrl: String) {
        val host = proxyUrl.split(":")[0].trim()
        val port = proxyUrl.split(":")[1]
        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(
                host, port.trim().toInt()))
        SentinelState.torProxy = proxy;
    }

    private fun setUpChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val descriptionText = getString(R.string.import_channel_description)
            val name = getString(R.string.import_channel_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel("IMPORT_CHANNEL", name, importance)
            mChannel.description = descriptionText
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)

            val channel = NotificationChannel("PAYMENTS_CHANNEL", "Payments", importance)
            channel.description = "Alerts for new payments"
            channel.enableLights(true)
            channel.importance = NotificationManager.IMPORTANCE_HIGH
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun gePolicyManager(): BackgroundManager.Builder.Policy {
        return BackgroundManager.Builder()
                .runServiceInForeground(true)
    }

    private fun getTorNotificationBuilder(): ServiceNotification.Builder {
        return ServiceNotification.Builder(
                channelName = "Tor Service",
                channelDescription = "Tor foreground service notifications ",
                channelID = "TOR_CHANNEL",
                notificationID = 121
        )
                .setActivityToBeOpenedOnTap(
                        clazz = HomeActivity::class.java,
                        intentExtrasKey = null,
                        intentExtras = null,
                        intentRequestCode = null
                )
                .enableTorRestartButton(enable = true)
                .enableTorStopButton(enable = true)
                .showNotification(show = true)
    }


    override fun onTerminate() {
        dataBaseScope.cancel()
        apiScope.cancel()
        super.onTerminate()
    }

    private class CrashReportingTree(val dir: File) : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (t == null) {
                return
            }
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val file = File("${dir}/error_dump.log")
                    if (!file.exists()) {
                        file.createNewFile()
                    }
                    //Limit file size to 2 mb
                    if ((file.length() / 1024) > 2048) {
                        file.writeText("")
                    }
                    file.appendText(
                            "\n-Logged at: ${System.currentTimeMillis()}-\n" +
                                    "" +
                                    " ${t.stackTraceToString()}"
                    )
                } catch (ex: Exception) {
                    throw CancellationException(ex.message)
                }
            }
        }
    }
}
