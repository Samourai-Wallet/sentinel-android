package com.samourai.sentinel.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.samourai.sentinel.SentinelApplication
import com.samourai.sentinel.core.SentinelUtil
import com.samourai.sentinel.data.db.DbHandler
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.util.MonetaryUtil
import io.paperdb.Paper
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module


class SentinelApplicationV3 : Application() {


    override fun onCreate() {
        super.onCreate()
        Paper.init(applicationContext)
        initializeDI()
        setNotificationChannels()
    }


    /**
     * Koin
     */
    private fun initializeDI() {

        val appModule = module {
            single { DbHandler() }
            single { MonetaryUtil.getInstance() }
            single { CollectionRepository() }
        }

        startKoin {
            androidContext(this@SentinelApplicationV3)
            modules(appModule)
        }
    }

    private fun setNotificationChannels() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                    SentinelApplication.TOR_CHANNEL_ID,
                    "Tor service ",
                    NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.setSound(null, null)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
