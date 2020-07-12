package com.samourai.sentinel.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fasterxml.jackson.module.kotlin.readValue
import com.samourai.sentinel.api.ApiService
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.data.WalletResponse
import com.samourai.sentinel.data.db.DbHandler
import com.samourai.sentinel.helpers.mapper
import kotlinx.coroutines.*
import okhttp3.Response
import org.koin.java.KoinJavaComponent

/**
 * sentinel-android
 *
 * @author Sarath
 */


/**
 * Repository class for handling transaction related to a specific collection
 * Each collection needs to maintain its own repo for handling tx's
 * Tx data will be saved in a separate file
 * transactions that belongs to collection will be stored in a separate file
 */
class TransactionsRepository(private var collection: PubKeyCollection) {

    private val transactions: ArrayList<Tx> = arrayListOf()
    private val transactionsLiveData: MutableLiveData<ArrayList<Tx>> = MutableLiveData(arrayListOf())
    private val dbHandler: DbHandler by KoinJavaComponent.inject(DbHandler::class.java)
    private val sentinelState: SentinelState by KoinJavaComponent.inject(SentinelState::class.java)
    private val apiService: ApiService by KoinJavaComponent.inject(ApiService::class.java)
    private val repository: CollectionRepository by KoinJavaComponent.inject(CollectionRepository::class.java)


    init {
        fetchFromLocal()
    }

    fun getTransactionsLiveData(): LiveData<ArrayList<Tx>> {
        return transactionsLiveData
    }

    private fun fetchFromLocal() = GlobalScope.launch(Dispatchers.IO) {
        try {
            val readValue: ArrayList<Tx> = dbHandler.getTxStore(collection.id).read("txs")
                    ?: arrayListOf()
            if (readValue.size != 0) {
                withContext(Dispatchers.Main) {
                    setTransactions(readValue)
                }
            }
        } catch (e: Exception) {
            throw  e
        }
    }

    fun fetchFromServer() = GlobalScope.launch(Dispatchers.IO) {

        try {
            val newTransactions: ArrayList<Tx> = arrayListOf();
            val jobs: ArrayList<Deferred<Response>> = arrayListOf()
            transactions.clear()
            collection.pubs.forEach {
                val item = async {
                    apiService.getTx(it.pubKey)
                }
                jobs.add(item);
            }
            repeat(jobs.size) { index ->
                val res = jobs[index].await()
                res.body?.let {
                    val pubKeyAssociated = collection.pubs[index]
                    val response: WalletResponse = mapper.readValue(it.string())
                    sentinelState.blockHeight = response.info.latest_block
                    var latestBlockHeight = 1L
                    if (sentinelState.blockHeight != null)
                        latestBlockHeight = sentinelState.blockHeight?.height!!
                    val items = response.txs.map { tx ->
                        tx.associatedPubKey = pubKeyAssociated.pubKey
                        tx.confirmations = if (latestBlockHeight > 0L && tx.block_height > 0L) (latestBlockHeight.minus(tx.block_height)) + 1 else 0
                        tx
                    }
                    collection.pubs[index].balance = response.wallet.final_balance
                    newTransactions.addAll(items)
                    collection.updateBalance()
                    val item = repository.pubKeyCollections.find { collection -> collection.id == collection.id }
                    repository.update(collection, repository.pubKeyCollections.indexOf(item))
                }
            }
            setTransactions(newTransactions)
        } catch (e: Exception) {
            throw  e
        }
    }

    private fun setTransactions(newTransactions: ArrayList<Tx>) {
        transactions.addAll(newTransactions)
        transactionsLiveData.postValue(transactions)
        saveTransactions()
    }

    fun loadLocal(): Job {
        return fetchFromLocal()
    }

    private fun saveTransactions() = GlobalScope.launch(Dispatchers.IO) {
        dbHandler.getTxStore(collection.id).write("txs", transactions)
    }

    fun removeTx(pubKeyModel: PubKeyModel): Job {
        transactions.clear()
        transactions.addAll(transactions.filter { it.associatedPubKey != pubKeyModel.pubKey })
        transactionsLiveData.postValue(transactions)
        return saveTransactions()
    }


}