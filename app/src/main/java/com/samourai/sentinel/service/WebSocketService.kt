package com.samourai.sentinel.service

import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.samourai.sentinel.api.ApiService
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class WebSocketService : JobService() {
    private val webSocketHandler: WebSocketHandler by inject(WebSocketHandler::class.java);

    override fun onStartJob(p0: JobParameters?): Boolean {
        Timber.i("onStartJob ")
        webSocketHandler.connect()
        return false
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        Timber.i("onStopJob")
        webSocketHandler.dispose()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("onDestroy")
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Timber.i("onRebind ")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.i("onLowMemory")
    }

    companion object {
        fun start(mContext: Context) {
            val mJobScheduler = mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobInfo = JobInfo.Builder(12, ComponentName(mContext, WebSocketService::class.java))
                    .setPersisted(true)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build()

            mJobScheduler.schedule(jobInfo)
        }
    }
}
