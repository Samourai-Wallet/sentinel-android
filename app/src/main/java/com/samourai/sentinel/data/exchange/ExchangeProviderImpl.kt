package com.samourai.sentinel.data.exchange

import okhttp3.Response

/**
 * sentinel-android
 *
 * @author Sarath
 */
interface ExchangeProviderImpl {


    fun getRate(): Long

    fun parse(response: Response)

    fun getCurrencies(): ArrayList<String>

    fun getCurrency(): String

    fun setRate(rate: Long)

    fun getKey(): String

    suspend fun fetch() {}
}