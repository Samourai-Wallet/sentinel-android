package com.samourai.sentinel.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samourai.sentinel.api.ApiService
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.data.*
import com.samourai.sentinel.data.db.DbHandler
import com.samourai.sentinel.helpers.fromJSON
import com.samourai.sentinel.ui.utils.logThreadInfo
import com.samourai.sentinel.util.apiScope
import kotlinx.coroutines.*
import okhttp3.Response
import org.json.JSONObject
import org.koin.java.KoinJavaComponent

/**
 * sentinel-android
 *
 */


/**
 * Repository class for handling transaction related to a specific collection
 * Each collection needs to maintain its own repo for handling tx's
 * Tx data will be saved in a separate file
 * transactions that belongs to collection will be stored in a separate file
 */
class TransactionsRepository {

    private val transactions: ArrayList<Tx> = arrayListOf()
    private var transactionsLiveData: MutableLiveData<ArrayList<Tx>> = MutableLiveData(arrayListOf())
    private val dbHandler: DbHandler by KoinJavaComponent.inject(DbHandler::class.java)
    private val apiService: ApiService by KoinJavaComponent.inject(ApiService::class.java)
    private val collectionRepository: CollectionRepository by KoinJavaComponent.inject(CollectionRepository::class.java)
    private val loading: MutableLiveData<Boolean> = MutableLiveData(false)
    private val txStore = dbHandler.getTxStore()

    //track currently loading collection
    var loadingCollectionId = ""

    fun getTransactionsLiveData(): LiveData<ArrayList<Tx>> {
        return transactionsLiveData
    }

    fun fetchFromLocal(collectionId: String) = apiScope.launch {
        try {
            logThreadInfo("fetchFromLocal")
            val collection = collectionRepository.findById(collectionId) ?: return@launch
            val readValue: ArrayList<Tx> = txStore.read(collection.id)
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

    /**
     *  Api call for wallet api
     *  using coroutines concurrent execution
     */
    suspend fun fetchFromServer(collectionId: String) {
        try {
            logThreadInfo("fetchFromServer")
            val collection = collectionRepository.findById(collectionId) ?: return
            val newTransactions: ArrayList<Tx> = arrayListOf();
            val utxos: ArrayList<Utxo> = arrayListOf();
            val jobs: ArrayList<Deferred<Response>> = arrayListOf()
            transactions.clear()
            collection.pubs.forEach {
                val item = apiScope.async {
                    apiService.getWallet(it.pubKey)
                }
                jobs.add(item);
            }
            apiScope.launch(Dispatchers.Main) {
                loading.postValue(true)
            }

            jobs.forEach { job ->
                val index = jobs.indexOf(job)
                loadingCollectionId = collectionId
                try {
                    val res = jobs[index].await()
                    res.body?.let { it ->
                        val resString = it.string()
                        val pubKeyAssociated = collection.pubs[index]
                        val jsonObject = JSONObject(resString)
                        if (jsonObject.has("status")) {
                            if (jsonObject.getString("status") == "error") {
                                throw CancellationException("Invalid token")
                            }
                        }
                        val response: WalletResponse = fromJSON<WalletResponse>(resString)!!
                        SentinelState.blockHeight = response.info.latest_block
                        var latestBlockHeight = 1L
                        if (SentinelState.blockHeight != null)
                            latestBlockHeight = SentinelState.blockHeight?.height!!
                        val items = response.txs.map { tx ->
                            tx.associatedPubKey = pubKeyAssociated.pubKey
                            val txBlockHeight = tx.block_height ?: 0
                            tx.confirmations = if (latestBlockHeight > 0L && txBlockHeight  > 0L) (latestBlockHeight.minus(txBlockHeight)) + 1 else 0
                            tx
                        }
                        response.unspent_outputs?.let {
                            utxos.addAll(response.unspent_outputs)
                        }
                        collection.pubs[index].balance = response.wallet.final_balance
                        if (collection.pubs[index].type != AddressTypes.ADDRESS) {
                            val address = response.addresses?.find { address -> address.address == collection.pubs[index].pubKey }
                            if (address != null) {
                                address.accountIndex?.let { accIndex ->
                                    collection.pubs[index].account_index = accIndex
                                }
                                address.changeIndex?.let { changeIndex ->
                                    collection.pubs[index].change_index = changeIndex
                                }
                            }
                        }
                        newTransactions.addAll(items)
                        collection.updateBalance()
                        collection.lastRefreshed = System.currentTimeMillis()
                        val item = collectionRepository.pubKeyCollections.find { collection -> collection.id == collectionId }
                                ?: return
                        collectionRepository.update(collection, collectionRepository.pubKeyCollections.indexOf(item))
                    }
                } catch (e: Exception) {
                    throw  e
                }
            }
            apiScope.launch(Dispatchers.Main) {
                setTransactions(newTransactions)
            }
            saveTx(newTransactions, collectionId)
            saveUtxos(utxos, collectionId)
        } catch (e: Exception) {
            apiScope.launch(Dispatchers.Main) {
                loading.postValue(false)
            }
            throw  e
        }
    }

    private fun setTransactions(newTransactions: ArrayList<Tx>) {
        transactions.clear()
        transactions.addAll(newTransactions)
        transactionsLiveData.postValue(transactions)
        loading.postValue(false)
    }

    fun loadingState(): LiveData<Boolean> {
        return loading
    }

    private fun saveTx(transactions: ArrayList<Tx>, collectionId: String) = apiScope.launch {
        withContext(Dispatchers.IO) {
            txStore.write(collectionId, transactions)
        }
    }


    private fun saveUtxos(utxos: ArrayList<Utxo>, collectionId: String) = apiScope.launch {
        withContext(Dispatchers.IO) {
            dbHandler.getUTXOsStore().write(collectionId, utxos);
        }
    }

    fun removeTxsRelatedToPubKey(pubKeyModel: PubKeyModel, collectionId: String) {
        transactions.clear()
        transactions.addAll(transactions.filter { it.associatedPubKey.toLowerCase() != pubKeyModel.pubKey.toLowerCase() })
        transactionsLiveData.postValue(transactions)
        val collection = collectionRepository.findById(collectionId)
        if (collection != null) {
            collectionRepository.update(collection)
        }
        saveTx(transactions, collectionId)
    }

    fun clear() {
        transactions.clear()
        transactionsLiveData = MutableLiveData()
    }

    suspend fun fetchFromServer(collection: PubKeyCollection) {
        return this.fetchFromServer(collection.id)
    }
}