package com.samourai.sentinel.api

import android.util.Log
import com.samourai.sentinel.ui.dojo.DojoUtility
import com.samourai.sentinel.ui.utils.PrefsUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.internal.EMPTY_REQUEST
import org.json.JSONObject
import org.koin.java.KoinJavaComponent


/**
 * Based on OkHttp OAuth pattern
 * https://square.github.io/okhttp/4.x/okhttp/okhttp3/-authenticator/
 *
 * Authenticate to the backend by providing the API key expected by the server.
 * If authentication succeeds, the endpoint returns a json embedding an access token and a refresh token (JSON Web Tokens).
 * The refresh token must be passed as an argument or in the Authorization HTTP header for later calls to /auth/refresh allowing to generate a new access token.
 */
class TokenAuthenticator(private val apiService: ApiService) : Authenticator {

    private val prefsUtil: PrefsUtil by KoinJavaComponent.inject(PrefsUtil::class.java)

    override fun authenticate(route: Route?, response: Response): Request? {
        val responseJson = JSONObject(response.body?.string()!!)
        val retryCount = retryCount(response)
        val client = ApiService.buildClient(true, apiService.getAPIUrl(), null, "")
        // Give up acquiring token
        // Both Authentication and renewing requests are already passed
        if (retryCount == 2) {
            return null
        }
        if (response.code == 401) {
            if (responseJson.has("error")) {
                val error = responseJson.getString("error")
                val checkPoint = error.contains("Invalid JSON Web Token")
                if (checkPoint && retryCount == 0) {
                    val request = Request.Builder()
                            .url("${apiService.getAPIUrl()}/auth/refresh")
                            .addHeader("Authorization", "Bearer ${prefsUtil.refreshToken}")
                            .post(EMPTY_REQUEST)
                            .build()
                    val refreshResponse = client.newCall(request).execute()
                    if (refreshResponse.isSuccessful) {
                        val body = refreshResponse.body?.string()
                        val payload = JSONObject(body).getJSONObject("authorizations")
                        if (payload.has("access_token")) {
                            val token = payload.getString("access_token");
                            prefsUtil.authorization = token
                            return rewriteRequest(response.request, retryCount, token)
                        }
                        return rewriteRequest(response.request, retryCount, prefsUtil.authorization)
                    }
                } else {
                    runBlocking {
                        apiService.authenticateDojo()
                        delay(100)
                    }
                    return rewriteRequest(response.request, retryCount, prefsUtil.authorization)
                }
            }

        }
        return rewriteRequest(response.request, retryCount, prefsUtil.authorization)
    }

    private fun retryCount(response: Response?): Int {
        return response?.request?.header("X-RetryCount")?.toInt() ?: 0
    }


    /**
     * Rewrite current request with newly acquired token
     * Also retry count header to track trials
     */
    private fun rewriteRequest(
            staleRequest: Request?, retryCount: Int, token: String?
    ): Request? {
        return staleRequest?.newBuilder()
                .apply {
                    this!!.url(staleRequest?.url?.newBuilder()
                            ?.removeAllQueryParameters("at")
                            ?.addQueryParameter("at", token)
                            ?.build()!!)
                    header(
                            "X-RetryCount",
                            "${retryCount + 1}"
                    )
                }?.build()
    }

}