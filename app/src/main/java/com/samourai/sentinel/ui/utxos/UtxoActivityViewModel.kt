package com.samourai.sentinel.ui.utxos

import androidx.lifecycle.*
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.data.Utxo
import com.samourai.sentinel.data.db.DbHandler
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.ui.utils.logThreadInfo
import com.samourai.sentinel.util.apiScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

/**
 * sentinel-android
 *
 * @author sarath
 */
internal class UtxoActivityViewModel(private val pubKeyCollection: PubKeyCollection) : ViewModel() {


    private val collectionRepository: CollectionRepository by inject(CollectionRepository::class.java)
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java)
    private val dbHandler: DbHandler by inject(DbHandler::class.java)

    private val utxosLiveData: MutableLiveData<ArrayList<Utxo>> = MutableLiveData()

    init {
        val job = viewModelScope.launch {
            fetchFromLocal()
        }
        job.invokeOnCompletion {
            if(it != null){
                 Timber.e(it)
            }
        }
    }

    fun getPubKeys(): LiveData<ArrayList<PubKeyModel>> {
        val mediator = MediatorLiveData<ArrayList<PubKeyModel>>();
        mediator.addSource(collectionRepository.collectionsLiveData) { pubCollection ->
            pubCollection.forEach {
                if (it.id == pubKeyCollection.id) {
                    mediator.value = it.pubs
                } else {
                    mediator.value = ArrayList()
                }
            }
        }
        return mediator
    }

    private suspend fun fetchFromLocal() {
        try {

            val readValue: ArrayList<Utxo> = dbHandler.getUTXOsStore().read(pubKeyCollection.id)
                    ?: arrayListOf()
            if (readValue.size != 0) {
                withContext(Dispatchers.Main) {
                    utxosLiveData.postValue(readValue)
                }
            }
        } catch (e: Exception) {
            throw  e
        }
    }

    public fun getUtxo(pubKey: String): LiveData<ArrayList<Utxo>> {
        val mediator = MediatorLiveData<ArrayList<Utxo>>()
        mediator.addSource(utxosLiveData) { utxos ->
            mediator.postValue(utxos.filter { it.isBelongsToPubKey(pubKey) }.toCollection(ArrayList()))
        }
        return mediator
    }

    class UtxoViewModelViewModelFactory(private val pubKeyCollection: PubKeyCollection) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UtxoActivityViewModel::class.java)) {
                return UtxoActivityViewModel(pubKeyCollection) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        fun getFactory(pubKeyCollection: PubKeyCollection): UtxoViewModelViewModelFactory {
            return UtxoViewModelViewModelFactory(pubKeyCollection)
        }
    }
}
