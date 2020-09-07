package com.samourai.sentinel.data.exchange

import com.samourai.sentinel.api.ApiService
import com.samourai.sentinel.ui.utils.PrefsUtil
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/**
 * sentinel-android
 *
 * @author Sarath
 */
class LBTCExchangeProvider : ExchangeProviderImpl {

    private val localBitcoinEndPoint = "https://localbitcoins.com/bitcoinaverage/ticker-all-currencies/"
    private val apiService: ApiService by inject(ApiService::class.java)
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java)
    private val availableCurrencies = arrayListOf(
            "USD",
            "EUR",
            "INR",
            "COP",
            "BOB",
            "TWD",
            "GHS",
            "NGN",
            "EGP",
            "IDR",
            "BGN",
            "SZL",
            "CRC",
            "PEN",
            "AMD",
            "ILS",
            "GBP",
            "MWK",
            "DOP",
            "BAM",
            "XRP",
            "DKK",
            "RSD",
            "AUD",
            "PKR",
            "JPY",
            "TZS",
            "VND",
            "KWD",
            "RON",
            "HUF",
            "CLP",
            "MYR",
            "GTQ",
            "JMD",
            "ZMW",
            "UAH",
            "JOD",
            "LTC",
            "SAR",
            "ETH",
            "CAD",
            "SEK",
            "SGD",
            "HKD",
            "GEL",
            "BWP",
            "VES",
            "CHF",
            "IRR",
            "BBD",
            "KRW",
            "CNY",
            "XOF",
            "BDT",
            "HRK",
            "NZD",
            "TRY",
            "THB",
            "XAF",
            "BYN",
            "ARS",
            "UYU",
            "RWF",
            "KZT",
            "NOK",
            "RUB",
            "ZAR",
            "PYG",
            "PAB",
            "MXN",
            "CZK",
            "BRL",
            "MAD",
            "PLN",
            "PHP",
            "KES",
            "AED"
    )
    private var rate: Long = 1L

    override fun getRate(): Long {
        if (prefsUtil.selectedCurrency.isNullOrBlank()) {
            return 1L
        }
        return rate
    }

    override fun parse(response: Response) {

    }

    override fun getCurrencies(): ArrayList<String> {
        return availableCurrencies
    }

    override fun getCurrency(): String {
        return prefsUtil.selectedCurrency!!
    }

    override fun setRate(rate: Long) {
        this.rate = rate
        prefsUtil.exchangeRate = rate
    }

    override fun getKey(): String {
        return "localbitcoins.com"
    }

    override suspend fun fetch() {
        try {
            val request = Request.Builder()
                    .url(localBitcoinEndPoint)
                    .build()
            try {
                val response = apiService.request(request)
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    body?.let { responseBody ->
                        val jsonObject = JSONObject(responseBody)
                        if (jsonObject.has(prefsUtil.selectedCurrency)) {
                            var avgPrice: Long? = null
                            val amountObj = jsonObject.getJSONObject(prefsUtil.selectedCurrency)
                            when {
                                amountObj.has("avg_12h") -> {
                                    avgPrice = amountObj.getLong("avg_12h");
                                }
                                amountObj.has("avg_24h") -> {
                                    avgPrice = amountObj.getLong("avg_24h");
                                }
                                amountObj.has("avg_1h") -> {
                                    avgPrice = amountObj.getLong("avg_1h");
                                }
                            }
                            avgPrice?.let {
                                setRate(it)
                            }
                        }
                    }
                }
            } catch (e: ApiService.ApiNotConfigured) {
                throw CancellationException(e.message)
            }

        } catch (e: Exception) {
            throw CancellationException(e.message)
        }
    }
}