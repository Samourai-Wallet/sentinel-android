package com.samourai.sentinel.api

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.samourai.sentinel.BuildConfig
import com.samourai.sentinel.SamouraiSentinel
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.tor.TorManager
import com.samourai.sentinel.util.WebUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.java.KoinJavaComponent

/**
 * sentinel-android
 *
 * @author Sarath
 */

class ApiService {

    private val appContext: Context by KoinJavaComponent.inject(Context::class.java);


    val SAMOURAI_API = "https://api.samouraiwallet.com/"
    val SAMOURAI_API_CHECK = "https://api.samourai.com/v1/status"
    val SAMOURAI_API2 = "https://api.samouraiwallet.com/v2/"
    val SAMOURAI_API2_TESTNET = "https://api.samouraiwallet.com/test/v2/"


    val SAMOURAI_API2_TOR_DIST = "http://d2oagweysnavqgcfsfawqwql2rwxend7xxpriq676lzsmtfwbt75qbqd.onion/v2/"
    val SAMOURAI_API2_TESTNET_TOR_DIST = "http://d2oagweysnavqgcfsfawqwql2rwxend7xxpriq676lzsmtfwbt75qbqd.onion/test/v2/"

    var SAMOURAI_API2_TOR = SAMOURAI_API2_TOR_DIST
    var SAMOURAI_API2_TESTNET_TOR = SAMOURAI_API2_TESTNET_TOR_DIST
    private var APP_TOKEN: String? = null

    private var ACCESS_TOKEN: String? = null

    private val ACCESS_TOKEN_REFRESH = 300L

    private var client: OkHttpClient = OkHttpClient()

    init {
        buildClient()
    }


    fun buildClient() {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }
        client = builder.build()
    }


    suspend fun getTx(pubKey: String): Response {
        buildClient()
        val request = Request.Builder()
                .url("${getAPIUrl()}multiaddr?active=${pubKey}")
                .build()
        return client.newCall(request).execute();
    }


    private fun getAPIUrl(): String? {
        return if (TorManager.getInstance(appContext).isRequired) {
            if (SamouraiSentinel.getInstance().isTestNet) SAMOURAI_API2_TESTNET_TOR else SAMOURAI_API2_TOR
        } else {
            if (SamouraiSentinel.getInstance().isTestNet) SAMOURAI_API2_TESTNET else SAMOURAI_API2
        }
    }
}
