package com.samourai.sentinel.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samourai.sentinel.data.exchange.ExchangeProviderImpl
import com.samourai.sentinel.data.exchange.LBTCExchangeProvider
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.util.apiScope
import kotlinx.coroutines.*
import org.koin.java.KoinJavaComponent
import timber.log.Timber

/**
 * sentinel-android
 *
 * @author Sarath
 */

class ExchangeRateRepository {

    data class Rate(var currency: String, var rate: Long)

    private var job: Job? = null;
    private val prefsUtil: PrefsUtil by KoinJavaComponent.inject(PrefsUtil::class.java);
    private val rateLiveData: MutableLiveData<Rate> = MutableLiveData()
    private val message: MutableLiveData<String> = MutableLiveData()

    private val exchanges: ArrayList<ExchangeProviderImpl> = arrayListOf(
            LBTCExchangeProvider()
    )

    private var selectedExchange: ExchangeProviderImpl = exchanges.first()

    init {
        emitChanges()
    }

    fun getCurrencies(): ArrayList<String> {
        return selectedExchange.getCurrencies()
    }

    fun reloadChanges() {
        exchanges.find { it.getKey() == prefsUtil.exchangeSelection }?.let {
            selectedExchange = it
        }
    }

    fun getExchanges(): ArrayList<String> {
        return ArrayList<String>().apply {
            this.addAll(exchanges.map { it.getKey() }.toTypedArray())
        }
    }

    fun getRateLive(): LiveData<Rate> {
        return rateLiveData
    }

    fun fetch() {
        job?.let {
            if (it.isActive) {
                it.cancel()
            }
        }
        job = apiScope.launch {
            try {
                selectedExchange.fetch()
            } catch (e: Exception) {
                throw CancellationException(e.message)
            }
        }
        job?.invokeOnCompletion {
            if (it != null) {
                message.postValue(it.message)
            } else {
                emitChanges()
            }
        }
    }

    private fun emitChanges() {
        if (prefsUtil.exchangeRate!! <= 1) {
            return
        }
        rateLiveData.postValue(Rate(prefsUtil.selectedCurrency!!, prefsUtil.exchangeRate!!))
        Timber.i("emitChanges: ")
    }

}