package com.samourai.sentinel.ui.collectionDetails.transactions

import android.app.Application
import android.util.Log
import androidx.arch.core.util.Function
import androidx.lifecycle.*
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.data.repository.TransactionsRepository
import com.samourai.sentinel.util.apiScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bitcoinj.core.Coin
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.inject
import java.util.concurrent.CancellationException


class TransactionsViewModel(application: Application) : AndroidViewModel(application) {


    private val transactionsRepository: TransactionsRepository by inject(TransactionsRepository::class.java)

    private val message = MutableLiveData<String>()
    private val balance = MutableLiveData<String>("")
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
        transactionsRepository.fetchFromLocal(collection.id)
    }

    fun getMessage(): LiveData<String> {
        return message
    }

    fun getTransactions(): LiveData<ArrayList<Tx>> {
        return transactionsRepository.getTransactionsLiveData()
                .map {
                    updateBalance()
                    it
                }
    }

    private fun updateBalance() {
        balance.postValue("${getBTCDisplayAmount(collection.balance)} BTC")
    }

    /**
     * Get balance live data for fragments
     * balance will be updated when transactions updated @see #getTransactions()
     */
    fun getBalance(): LiveData<String> {
        return balance
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


    private fun getBTCDisplayAmount(value: Long): String? {
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