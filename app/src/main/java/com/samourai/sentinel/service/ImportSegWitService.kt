package com.samourai.sentinel.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.samourai.sentinel.R
import com.samourai.sentinel.api.ApiService
import kotlinx.coroutines.*
import org.json.JSONObject
import org.koin.java.KoinJavaComponent


/**
 * sentinel-android
 *
 */

class ActionReceiverImportSegWitService : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, ImportSegWitService::class.java)
        serviceIntent.action = intent?.action
        context?.startService(serviceIntent)
    }
}

class ImportSegWitService : Service() {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob())
    val apiService: ApiService by KoinJavaComponent.inject(ApiService::class.java)

    private val notificationId = 95
    var progress = 0

    override fun onCreate() {
        super.onCreate()
        startForeground(notificationId, getNotification())

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return if (intent?.action.equals("CLOSE")) {
            this.stopSelf()
            START_NOT_STICKY
        } else {
            startForeground(notificationId, getNotification())
            startImporting(intent)
            START_STICKY
        }


    }

    private fun startImporting(intent: Intent?) {
        var pubKey = intent?.extras?.getString("pubKey")
        var segwit = intent?.extras?.getString("segWit")
        if (pubKey == null || segwit == null) {
            this.stopSelf()
            return
        }
        scope.launch {
            try {
                val deferredImportStatus = async { apiService.checkImportStatus(pubKey) }
                val importStatus = deferredImportStatus.await()
                if (!importStatus) {
                    val job = async {
                        try {
                            apiService.importXpub(pubKey, segwit)
                        } catch (e: Exception) {
                            throw CancellationException(e.message)
                        }
                    }
                    val response = job.await()
                    if (response.isSuccessful) {
                        val json = withContext(Dispatchers.Default) {

                            JSONObject(response.body?.string())

                        }
                        if (json.getString("status") == "ok") {
                            success()
                        } else {
                            failure("Import failed")
                        }
                    } else {
                        failure(response.message)
                    }
                    job.invokeOnCompletion {
                        if (it != null) {
                            it.message?.let { it1 -> failure(it1) };
                        }
                    }
                }
            } catch (e: Exception) {
                e.message?.let { failure(it) }
                e.printStackTrace()
            }
        }
    }

    private fun success() {
        val notification = NotificationCompat.Builder(this, "IMPORT_CHANNEL")
                .setContentTitle("Importing")
                .setContentText("Imported successfully")
                .setSmallIcon(R.drawable.ic_sentinel)
                .addAction(closeAction())
                .build()
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(notificationId, notification)
    }

    private fun failure(message: String) {
        val notification = NotificationCompat.Builder(this, "IMPORT_CHANNEL")
                .setContentTitle("Importing")
                .setContentText("Error : $message")
                .setSmallIcon(R.drawable.ic_sentinel)
                .addAction(closeAction())
                .build()
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(notificationId, notification)
    }


    private fun closeAction(): NotificationCompat.Action? {
        val broadcastIntent = Intent(this, ActionReceiverImportSegWitService::class.java)
        broadcastIntent.action = "CLOSE"
        val actionIntent = PendingIntent.getBroadcast(this,
                0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(R.drawable.ic_sentinel, "Close", actionIntent)
    }

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, "IMPORT_CHANNEL")
                .setContentTitle("Importing SegWit xpub")
                .setSmallIcon(R.drawable.ic_sentinel)
                .setProgress(0, 0, true)
                .setOnlyAlertOnce(true)
                .build();
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}