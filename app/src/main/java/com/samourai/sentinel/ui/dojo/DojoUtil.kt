package com.samourai.sentinel.ui.dojo

import android.content.Context
import com.samourai.sentinel.api.ApiService
import com.samourai.sentinel.data.db.DbHandler
import com.samourai.sentinel.data.db.PayloadRecord
import com.samourai.sentinel.helpers.fromJSON
import com.samourai.sentinel.helpers.toJSON
import com.samourai.sentinel.ui.utils.PrefsUtil
import kotlinx.coroutines.*
import okhttp3.Response
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.io.File

/**
 * sentinel-android
 *
 **/

class DojoUtility {

    private var dojoPayload: DojoPairing? = null
    private var apiKey: String? = null
    private var isAuthenticated = false;
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java)
    private val dbHandler: DbHandler by inject(DbHandler::class.java)
    private val apiService: ApiService by inject(ApiService::class.java)

    private var dojoStore: PayloadRecord

    init {
        dojoStore = dbHandler.getDojoStore()
        read()
    }

    suspend fun setDojo(pairing: String): Response {
        val payload = fromJSON<DojoPairing>(pairing)
                ?: throw  Exception("Invalid payload")
        this.dojoPayload = payload
        prefsUtil.apiEndPoint = this.dojoPayload!!.pairing?.url
        prefsUtil.apiEndPointTor = this.dojoPayload!!.pairing?.url
        writePayload(dojoPayload!!);
        return apiService.authenticateDojo(dojoPayload!!.pairing!!.apikey!!);
    }


    fun validate(payloadString: String): Boolean {
        try {
            val payload = fromJSON<DojoPairing>(payloadString)
                    ?: throw  Exception("Invalid payload")
            if (payload.pairing == null) {
                return false
            }
            if (payload.pairing.type != "dojo.api") {
                return false
            }
            if (payload.pairing.apikey == null) {
                return false
            }
            if (payload.pairing.url == null) {
                return false
            }
            return true
        } catch (ex: Exception) {
            return false
        }
    }

    fun isAuthenticated(): Boolean {
        return isAuthenticated;
    }

    fun isDojoEnabled(): Boolean {
        return dojoPayload != null
    }

    private suspend fun writePayload(dojoPairing: DojoPairing) = withContext(Dispatchers.IO) {
        dbHandler.getDojoStore().write(dojoPairing, true)
    }

    private fun readPayload(): DojoPairing? {
        return if (dojoStore.file.exists()) {
            Timber.d("${dbHandler.getDojoStore().read<DojoPairing>()}")
            return dbHandler.getDojoStore().read<DojoPairing>()
        } else {
            null
        }
    }

    fun clearDojo() {
        CoroutineScope(Dispatchers.IO).launch {
            if (dojoStore.file.exists())
                dojoStore.file.delete()
        }
        dojoPayload = null
        prefsUtil.apiEndPointTor = null
        prefsUtil.apiEndPoint = null
    }

    fun setAuthToken(body: String) {
        val payload = JSONObject(body).getJSONObject("authorizations")
        val authorization = payload.getString("access_token")
        val refreshToken = payload.getString("refresh_token")
        prefsUtil.authorization = authorization
        prefsUtil.refreshToken = refreshToken
        isAuthenticated = true
    }

    fun getApiKey(): String? {
        return apiKey;
    }

    fun read() {
        CoroutineScope(Dispatchers.Default).launch {
            dojoPayload = readPayload()
            if (dojoPayload != null) {
                prefsUtil.apiEndPointTor = dojoPayload?.pairing?.url
                prefsUtil.apiEndPoint = dojoPayload?.pairing?.url
                apiKey = dojoPayload?.pairing?.apikey
                apiService.setAccessToken(prefsUtil.refreshToken)
            }
        }
    }

    fun store() {
        CoroutineScope(Dispatchers.Default).launch {
            dojoPayload?.let { writePayload(it) }
        }
    }

}
