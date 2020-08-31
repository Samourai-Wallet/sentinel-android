package com.samourai.sentinel.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.samourai.sentinel.R
import com.samourai.sentinel.api.ApiService
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.data.repository.TransactionsRepository
import com.samourai.sentinel.helpers.fromJSON
import com.samourai.sentinel.ui.home.HomeActivity
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.util.MonetaryUtil
import kotlinx.coroutines.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

/**
 * sentinel-android
 *
 * @author sarath
 */
class WebSocketHandler : WebSocketListener() {

    enum class Status {
        CONNECTED,
        DISCONNECTED,
    }

    private val context: Context by inject(Context::class.java);
    private val apiService: ApiService by inject(ApiService::class.java);
    private val monetaryUtil: MonetaryUtil by inject(MonetaryUtil::class.java);
    private val transactionsRepository: TransactionsRepository by inject(TransactionsRepository::class.java);
    private val collectionRepo: CollectionRepository by inject(CollectionRepository::class.java);
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);
    private val mainJob = SupervisorJob()
    private val webSocketScope = CoroutineScope(context = Dispatchers.IO) + mainJob
    private var socket: WebSocket? = null
    private var socketStatus = Status.DISCONNECTED

    init {
        webSocketScope.launch(Dispatchers.Main) {
            SentinelState.torStateLiveData().observeForever {
                if (it == SentinelState.TorState.OFF || it == SentinelState.TorState.ON)
                    closeSocket()
            }
        }

    }


    fun connect(): Job? {
        val apiEndPoint = try {
            apiService.getAPIUrl()?.toHttpUrl()
        } catch (er: ApiService.ApiNotConfigured) {
            return null
        }
        if (SentinelState.isTorRequired()) {
            if (!SentinelState.isTorStarted()) {
                return null
            }
        }
        Timber.i("OnConnect Calll")
        if (apiEndPoint != null) {
            var ws = "ws://"
            if (apiEndPoint.isHttps) {
                ws = "wss://"
            }
            /**
             * Create web socket from current api
             */
            val webSocketEndPoint = "${ws}${apiEndPoint.host}/${apiEndPoint.pathSegments.joinToString("/")}/inv".toUri()
            val client = ApiService.buildClient(apiService = null,
                    url = apiService.getAPIUrl(),
                    excludeApiKey = true,
                    excludeAuthenticator = true,
                    authToken = prefsUtil.authorization)


//            /**
//             * TODO()
//             * Need fix for WebSocket Auths
//             */
//            if (SentinelState.isDojoEnabled()) {
//                closeSocket()
//                return null
//            }

            return webSocketScope.launch {
                try {
                    val request = Request.Builder().url(webSocketEndPoint.toString()).build();
                    socket = client.newWebSocket(request, this@WebSocketHandler)
                } catch (e: Exception) {
                    Timber.e(e)
                    throw  CancellationException((e.message))
                }
            }

        }
        return null
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)
        Timber.i("GOT MESSAGE ")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Timber.i("onClosed")
        //Reconnect
        connect()
        socketStatus = Status.DISCONNECTED
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Timber.i("onFailure ${t.message}")
        Timber.e(t)
        socketStatus = Status.DISCONNECTED
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val payload = JSONObject(text)
        if (text.contains("Invalid JSON Web Token")) {
            apiService.authenticateDojo().invokeOnCompletion {
                webSocket.cancel()
                connect()
            }
            return
        }
        if (payload.has("op") && payload.getString("op") == "utx") {
            showTxNotification(payload)
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        socketStatus = Status.CONNECTED
        subscribeBlocks(webSocket)
        subscribeNewTx(webSocket)
    }

    private fun subscribeNewTx(webSocket: WebSocket) {

        collectionRepo.pubKeyCollections.forEach { pubKeyCollection ->
            pubKeyCollection.pubs.forEach {
                val payload = JSONObject().apply {
                    put("op", "addr_sub")
                    put("addr", it.pubKey)
                    addToken()
                }.toString();
                val item = webSocket.send(payload)
                Timber.d("SubscribeTx status:${item}, payload:$payload")
            }
        }

    }

    private fun subscribeBlocks(webSocket: WebSocket) {
        try {
            val payload = JSONObject().apply {
                put("op", "blocks_sub")
                addToken()
            }.toString();
            val item = webSocket.send(payload)
            Timber.d("SubscribeBlocks status:${item}, payload:$payload")
        } catch (er: Exception) {
            Timber.e(er)
        }
    }

    fun refreshSubscription() {
        if (socket != null) {
            subscribeNewTx(socket!!)
        } else {
            if (socketStatus == Status.DISCONNECTED) {
                connect()
            }
        }
    }

    private fun showTxNotification(payload: JSONObject) {
        try {
            val tx = fromJSON<Tx>(payload.getString("x")) ?: return

            var amount = 0L

            tx.inputs.forEach {
                if (it.prev_out != null)
                    it.prev_out.value.let { value ->
                        amount -= value
                    }
            }
            tx.out.forEach {
                amount += it.value
            }

            val notificationManager = NotificationManagerCompat.from(context)
            val mBuilder = NotificationCompat.Builder(context, "PAYMENTS_CHANNEL")
                    .setSmallIcon(R.drawable.ic_sentinel)
                    .setContentTitle("Payment received")
                    .setContentText("Amount ${monetaryUtil.formatToBtc(amount)} BTC")
                    .setTicker("Payment received")
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
            val notifyIntent: Intent = Intent(context, HomeActivity::class.java)
            val intent = PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            mBuilder.setContentIntent(intent)
            notificationManager.notify(tx.locktime, mBuilder.build())


            //Get all associated keys with current tx
            //Collection that associated with any of these keys will refreshed
            val keys = tx.inputs.map { it.prev_out?.xpub?.m }.toMutableList()
            keys.addAll(tx.out.map { it.xpub?.m })
            keys.addAll(tx.inputs.map { it.prev_out?.addr })
            keys.addAll(tx.out.map { it.addr })


            collectionRepo.pubKeyCollections.forEach {
                it.pubs.forEach { pubKeyModel ->
                    if (keys.contains(pubKeyModel.pubKey)) {
                        webSocketScope.launch {
                            try {
                                withContext(Dispatchers.IO) { transactionsRepository.fetchFromServer(it.id) }
                            } catch (e: Exception) {
                                Timber.e(e)
                                throw  CancellationException(e.message)
                            }
                        }
                    }
                }
            }


        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun dispose() {
        if (mainJob.isActive) {
            mainJob.cancel("Dispose")
        }
    }

    private fun closeSocket() {
        webSocketScope.launch(Dispatchers.IO) {
            try {
                if (socket != null) {
                    socket?.close(3000, "CLOSING");
                    connect()
                }
            } catch (e: Exception) {

            }
        }
    }


    private fun JSONObject.addToken() {
        if (SentinelState.isDojoEnabled()) {
            put("at", prefsUtil.authorization)
        }
    }
}

