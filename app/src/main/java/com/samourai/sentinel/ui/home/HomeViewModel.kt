package com.samourai.sentinel.ui.home

import androidx.lifecycle.*
import com.samourai.sentinel.api.ApiService
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.data.repository.ExchangeRateRepository
import com.samourai.sentinel.data.repository.FeeRepository
import com.samourai.sentinel.data.repository.TransactionsRepository
import com.samourai.sentinel.ui.dojo.DojoUtility
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.util.MonetaryUtil
import com.samourai.sentinel.util.apiScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bitcoinj.core.Coin
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class HomeViewModel : ViewModel() {


    private val repository: CollectionRepository by inject(CollectionRepository::class.java)
    private val exchangeRateRepository: ExchangeRateRepository by inject(ExchangeRateRepository::class.java)
    private val transactionsRepository: TransactionsRepository by inject(TransactionsRepository::class.java)
    private val dojoUtility: DojoUtility by inject(DojoUtility::class.java)
    private val apiService: ApiService by inject(ApiService::class.java)
    private val feeRepository: FeeRepository by inject(FeeRepository::class.java)

    private val errorMessage: MutableLiveData<String> = MutableLiveData()
    private val balance: MutableLiveData<Long> = MutableLiveData()
    private var netWorkJobs: ArrayList<Job?> = arrayListOf()
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);
    private val balanceFiat: MutableLiveData<String> = MutableLiveData()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                repository.read()
            } catch (e: Exception) {
                Timber.e(e)
                errorMessage.postValue(e.message);
            }

        }
    }

    fun getCollections(): LiveData<ArrayList<PubKeyCollection>> {
        return repository.collectionsLiveData.map {
            updateBalance()
            it
        }
    }

    fun getBalance(): LiveData<Long> {
        return balance
    }

    fun fetchBalance() {
        if (netWorkJobs.isNotEmpty()) {
            netWorkJobs.forEach {
                it?.cancel()
            }
        }
        try {
            exchangeRateRepository.fetch()
            repository.pubKeyCollections.forEach {

                val job = apiScope.launch {
                    try {
                        transactionsRepository.fetchFromServer(it)
                    } catch (e: Exception) {
                        Timber.e(e)
                        throw  CancellationException(e.message)
                    }
                }
                netWorkJobs.add(job)
            }
            val job = apiScope.launch {
                try {
                    feeRepository.getDynamicFees()
                } catch (e: Exception) {
                    Timber.e(e)
                    throw  CancellationException(e.message)
                }
            }
            netWorkJobs.add(job)

            if (netWorkJobs.isNotEmpty()) {
                //Save last sync time to prefs
                netWorkJobs[netWorkJobs.lastIndex]?.let {
                    it.invokeOnCompletion { error ->
                        if (error != null) {
                            Timber.e(error)
                            errorMessage.postValue("${error.message}")
                            return@invokeOnCompletion
                        }
                        prefsUtil.lastSynced = System.currentTimeMillis()
                    }
                }
            }
        } catch (ex: Exception) {
            Timber.e(ex)
            errorMessage.postValue("${ex.message}")
        }
    }

    fun getErrorMessage(): LiveData<String> {
        return errorMessage
    }

    fun loading(): LiveData<Boolean> {
        return transactionsRepository.loadingState()
    }

    fun getFiatBalance(): LiveData<String> {

        val mediatorLiveData = MediatorLiveData<String>();

        mediatorLiveData.addSource(exchangeRateRepository.getRateLive()) {
            mediatorLiveData.value = getFiatBalance(balance.value, it)
        }
        mediatorLiveData.addSource(balance) {
            mediatorLiveData.value = getFiatBalance(it, exchangeRateRepository.getRateLive().value)
        }
        return mediatorLiveData
    }

    private fun getFiatBalance(balance: Long?, rate: ExchangeRateRepository.Rate?): String {
        if (rate != null) {
            balance?.let {
                return try {
                    val fiatRate = MonetaryUtil.getInstance().getFiatFormat(prefsUtil.selectedCurrency)
                            .format((balance / 1e8) * rate.rate)
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


    private fun updateBalance() {
        if (repository.collectionsLiveData.value == null) {
            return
        }
        if (repository.collectionsLiveData.value!!.size == 0) {
            balance.postValue(0L)
        }
        repository.collectionsLiveData.value?.let { pubkeys ->
            if (pubkeys.isEmpty()) {
                return
            }
            try {
                val total = pubkeys.map { it.balance }.reduce { acc, l -> acc + l }
                balance.postValue(total)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}