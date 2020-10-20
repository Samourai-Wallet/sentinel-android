package com.samourai.sentinel.ui.collectionDetails.transactions

import android.app.Application
import androidx.lifecycle.*
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.data.repository.ExchangeRateRepository
import com.samourai.sentinel.data.repository.TransactionsRepository
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.util.MonetaryUtil
import com.samourai.sentinel.util.apiScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bitcoinj.core.Coin
import org.koin.java.KoinJavaComponent.inject
import java.util.concurrent.CancellationException


class TransactionsViewModel(application: Application) : AndroidViewModel(application) {


    private val transactionsRepository: TransactionsRepository by inject(TransactionsRepository::class.java)
    private val exchangeRateRepository: ExchangeRateRepository by inject(ExchangeRateRepository::class.java)
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java)

    private val message = MutableLiveData<String>()
    private val balance = MutableLiveData<Long>(0L)
    private lateinit var collection: PubKeyCollection
    private val loading: MutableLiveData<Boolean> = MutableLiveData(false)

    private var netWorkRequestJob: Job? = null;


    /**
     * When user navigates between multiple collection
     * Repository will load based on the selected collection
     * this will reset loaded cache from
     */
    override fun onCleared() {
        //Check if any ongoing requests , if there this will cancel it
        netWorkRequestJob?.let {
            if (it.isActive) {
                it.cancel()
            }
        }
        super.onCleared()
    }

    fun setCollection(collection: PubKeyCollection) {
        this.collection = collection
    }

    fun getMessage(): LiveData<String> {
        return message
    }

    private fun updateBalance() {
        balance.postValue(collection.balance)
    }

    /**
     * Get balance live data for fragments
     * balance will be updated when transactions updated @see #getTransactions()
     */
    fun getBalance(): LiveData<Long> {
        return balance
    }

    fun getFiatBalance(): LiveData<String> {
        val mediatorLiveData = MediatorLiveData<String>();
        mediatorLiveData.addSource(exchangeRateRepository.getRate()) {
            mediatorLiveData.value = getFiatBalance(balance.value, it)
        }
        mediatorLiveData.addSource(balance) {
            mediatorLiveData.value = getFiatBalance(it, exchangeRateRepository.getRate().value)
        }
        return mediatorLiveData
    }


    fun getLoadingState(): LiveData<Boolean> {
        return transactionsRepository.loadingState()
                .map {
                    if (it) {
                        transactionsRepository.loadingCollectionId == collection.id
                    } else {
                        false
                    }
                }
    }

    private fun getFiatBalance(balance: Long?, rate: ExchangeRateRepository.Rate?): String {
        if (rate != null) {
            balance?.let {
                return try {
                    val fiatRate = MonetaryUtil.getInstance().getFiatFormat(prefsUtil.selectedCurrency)
                            .format((balance/1e8) * rate.rate)
                    "$fiatRate ${rate.currency}"
                } catch (e: Exception) {
                    "00.00 ${rate.currency}"
                }
            }
            return "00.00"
        } else {
            return "00.00"
        }
    }


    fun getBTCDisplayAmount(value: Long): String? {
        return Coin.valueOf(value).toPlainString()
    }


    fun fetch() {
        // if a pending or an active network request is being processed we will cancel and request a new one
        // this will ensure only one request takes place at a time
        if (netWorkRequestJob != null && netWorkRequestJob?.isActive!!) {
            if (loading.value!!) {
                loading.postValue(false)
            }
            netWorkRequestJob?.cancel(CancellationException("User Canceled Request"))
        }
        try {
            loading.postValue(true)
            try {
                netWorkRequestJob = apiScope.launch(Dispatchers.IO) {
                    try {
                        transactionsRepository.fetchFromServer(collection)
                    } catch (e: Exception) {
                        throw CancellationException(e.message)
                    }
                }
                netWorkRequestJob?.invokeOnCompletion {
                    if (it != null) {
                        message.postValue("${it.message}")
                    }
                    loading.postValue(false)
                }
            } catch (e: Exception) {
                message.postValue("${e.message}")
            }

        } catch (ex: Exception) {
            loading.postValue(false)
            message.postValue(ex.message)
        }
    }


}