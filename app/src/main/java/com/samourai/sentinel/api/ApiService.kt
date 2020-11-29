@file:Suppress("BlockingMethodInNonBlockingContext")

package com.samourai.sentinel.api

import com.samourai.sentinel.BuildConfig
import com.samourai.sentinel.api.okHttp.await
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.ui.dojo.DojoUtility
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.util.apiScope
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * sentinel-android
 *
 * @author Sarath
 */

open class ApiService {

    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);
    private val dojoUtility: DojoUtility by inject(DojoUtility::class.java);

    private var ACCESS_TOKEN: String? = null

    private val ACCESS_TOKEN_REFRESH = 300L

    lateinit var client: OkHttpClient

    init {
        try {
            buildClient()
        } catch (e: Exception) {
        } catch (e: ApiNotConfigured) {
            Timber.e(e)
        }
    }


    private fun buildClient(excludeApiKey: Boolean = false, excludeAuthenticator: Boolean = false) {
        client = buildClient(excludeApiKey, getAPIUrl(), this, prefsUtil.authorization, excludeAuthenticator)
    }


    fun authenticateDojo(): Job {
        return apiScope.launch {
            if (dojoUtility.getApiKey() != null) {
                try {
                    val response = authenticateDojo(dojoUtility.getApiKey()!!)
                    if (response.isSuccessful) {
                        val string = response.body?.string()
                        string?.let { dojoUtility.setAuthToken(it) }
                    } else {
                        throw  Throwable(response.message)
                    }
                } catch (e: Exception) {
                    throw  Throwable(e.message)
                }
            }
        }
    }

    suspend fun checkImportStatus(pubKey: String) = withContext(Dispatchers.IO) {
        buildClient(excludeAuthenticator = true)
        val request = Request.Builder()
                .url("${getAPIUrl()}/xpub/${pubKey}/import/status")
                .build()
        val response = client.newCall(request).await()
        val status = response.body?.string()
        if (!status.isNullOrEmpty()) {
            val json = JSONObject(status)
            if (json["status"] == "ok") {
                return@withContext true
            } else {
                try {
                    return@withContext json.getJSONObject("data").getBoolean("import_in_progress")
                } catch (e: Exception) {
                    return@withContext false
                }
            }
        } else {
            return@withContext false
        }
    }

    suspend fun importXpub(pubKey: String, segwit: String): Response {
        buildClient(excludeAuthenticator = true)
        /**
         * Import process a long running process , this depends on the xpub
         *
         */
        client.newBuilder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .callTimeout(120, TimeUnit.SECONDS)
        val request = Request.Builder()
                .post(byteArrayOf().toRequestBody())
                .url("${getAPIUrl()}/xpub?xpub=$pubKey&&segwit=$segwit&type=restore")
                .build()
        return client.newCall(request).await()
    }


    suspend fun authenticateDojo(apiKey: String): Response {
        buildClient()
        val request = Request.Builder()
                .post(byteArrayOf().toRequestBody())
                .url("${getAPIUrl()}/auth/login?apikey=$apiKey")
                .build()
        return client.newCall(request).await()
    }


    suspend fun getTx(txId: String): Response {
        buildClient()
        val request = Request.Builder()
                .url("${getAPIUrl()}/tx/${txId}?fees=1")
                .build()
        return client.newCall(request).await()
    }


    suspend fun getFees(): Response {
        buildClient()
        val request = Request.Builder()
                .url("${getAPIUrl()}/fees")
                .build()
        return client.newCall(request).await()
    }


    suspend fun getWallet(pubKey: String): Response {
        buildClient()
        val request = Request.Builder()
                .url("${getAPIUrl()}/wallet?active=${pubKey}")
                .build()
        return client.newCall(request).await()
    }


    suspend fun request(request: Request): Response {
        buildClient()
        return client.newCall(request).await()
    }


    public fun getAPIUrl(): String? {
        return if (SentinelState.isTorRequired()) {
            if (prefsUtil.apiEndPointTor == null) {
                throw  ApiNotConfigured()
            }
            prefsUtil.apiEndPointTor
        } else {
            if (SentinelState.isTorStarted()) {
                return prefsUtil.apiEndPointTor
            }
            if (prefsUtil.apiEndPoint == null) {
                throw  ApiNotConfigured()
            }
            prefsUtil.apiEndPoint
        }
    }


    fun setAccessToken(accessToken: String?) {
        this.ACCESS_TOKEN = accessToken
    }

    class ApiNotConfigured : Throwable(message = "Api endpoint is not configured")
    class InvalidResponse : Throwable(message = "Invalid response")

    companion object {
        fun buildClient(
                excludeApiKey: Boolean = false, url: String?,
                apiService: ApiService?,
                authToken: String?,
                excludeAuthenticator: Boolean = false,
        ): OkHttpClient {
            val builder = OkHttpClient.Builder()
            if (BuildConfig.DEBUG) {
                builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            }
            builder.callTimeout(60, TimeUnit.SECONDS)
            builder.readTimeout(60, TimeUnit.SECONDS)
            builder.readTimeout(90, TimeUnit.SECONDS)
            builder.connectTimeout(120, TimeUnit.SECONDS)
            if (url != null && apiService != null) {
                if (!excludeAuthenticator)
                    builder.authenticator(TokenAuthenticator(apiService))
            }
            if (SentinelState.isTorStarted()) {
                builder.callTimeout(90, TimeUnit.SECONDS)
                builder.readTimeout(90, TimeUnit.SECONDS)
                builder.readTimeout(90, TimeUnit.SECONDS)
                builder.connectTimeout(120, TimeUnit.SECONDS)
                getHostNameVerifier(builder)
                builder.proxy(SentinelState.torProxy)
            }

            /**
             * Intercept current request and add apiKey if needed
             * for more please refer https://code.samourai.io/dojo/samourai-dojo/-/blob/master/doc/POST_auth_login.md#authentication
             */
            if (!excludeApiKey)
                builder.addInterceptor(object : Interceptor {
                    override fun intercept(chain: Interceptor.Chain): Response {
                        val original = chain.request()
                        val newBuilder = original.newBuilder()
                        if (!authToken.isNullOrEmpty() && SentinelState.isDojoEnabled()) {
                            newBuilder.url(original.url.newBuilder()
                                    .addQueryParameter("at", authToken)
                                    .build())
                        }
                        val request = newBuilder.build()
                        return chain.proceed(request)
                    }
                })
            return builder.build()
        }

        @Throws(Exception::class)
        protected fun getHostNameVerifier(clientBuilder: OkHttpClient.Builder) {

            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}

                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                    return arrayOf()
                }
            })

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory


            clientBuilder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            clientBuilder.hostnameVerifier(HostnameVerifier { _, _ -> true })
        }
    }


}
