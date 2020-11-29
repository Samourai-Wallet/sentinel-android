package com.samourai.sentinel.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samourai.sentinel.api.ApiService
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.data.*
import com.samourai.sentinel.data.db.SentinelCollectionStore
import com.samourai.sentinel.data.db.dao.TxDao
import com.samourai.sentinel.data.db.dao.UtxoDao
import com.samourai.sentinel.helpers.fromJSON
import com.samourai.sentinel.ui.utils.logThreadInfo
import com.samourai.sentinel.util.apiScope
import kotlinx.coroutines.*
import okhttp3.Response
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

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

    private val txDao: TxDao by inject(TxDao::class.java)
    private val utxoDao: UtxoDao by inject(UtxoDao::class.java)
    private val apiService: ApiService by inject(ApiService::class.java)
    private val collectionRepository: CollectionRepository by inject(CollectionRepository::class.java)
    private val feeRepository: FeeRepository by inject(FeeRepository::class.java)
    private val loading: MutableLiveData<Boolean> = MutableLiveData(false)

    //track currently loading collection
    var loadingCollectionId = ""

    fun getTransactionsLiveData(collectionId: String): LiveData<List<Tx>> {
        return txDao.getAllTx(collectionId)
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
                         Timber.i("fetchFromServer: ${response.info.fees}")
                        response.info.fees?.let { it1 -> feeRepository.parse(it1) }
                        SentinelState.blockHeight = response.info.latest_block
                        var latestBlockHeight = 1L
                        if (SentinelState.blockHeight != null)
                            latestBlockHeight = SentinelState.blockHeight?.height!!
                        val items = response.txs.map { tx ->
                            tx.associatedPubKey = pubKeyAssociated.pubKey
                            val txBlockHeight = tx.block_height ?: 0
                            tx.collectionId = collectionId
                            tx.confirmations = if (latestBlockHeight > 0L && txBlockHeight > 0L) (latestBlockHeight.minus(txBlockHeight)) + 1 else 0
                            tx
                        }
                        response.unspent_outputs?.let {
                            val list = response.unspent_outputs.toMutableList().map {
                                it.pubKey = pubKeyAssociated.pubKey
                                it.idx = "${it.txHash}:${it.txOutputN}"
                                it.collectionId = collectionId
                                if(it.xpub!=null){
                                    it.path = it.xpub?.path!!
                                }
                                it
                            }.toList()
                            utxos.addAll(list)
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
                loading.postValue(false)
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

    fun loadingState(): LiveData<Boolean> {
        return loading
    }

    private fun saveTx(transactions: ArrayList<Tx>, collectionId: String) = apiScope.launch {
        withContext(Dispatchers.IO) {
            transactions.forEach {
                txDao.insert(it)
            }
        }
    }


    private fun saveUtxos(utxos: ArrayList<Utxo>, collectionId: String) = apiScope.launch {
        withContext(Dispatchers.IO) {
            utxos.forEach {
                utxoDao.insert(it)
            }
        }
    }

    fun removeTxsRelatedToPubKey(pubKeyModel: PubKeyModel, collectionId: String) {
        val collection = collectionRepository.findById(collectionId)
        if (collection != null) {
            txDao.deleteRelatedCollection(collection.id, pubKeyModel.pubKey)
        }
    }

    suspend fun fetchFromServer(collection: PubKeyCollection) {
        return this.fetchFromServer(collection.id)
    }
}