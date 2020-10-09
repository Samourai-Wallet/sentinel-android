package com.samourai.sentinel.ui.utxos

import androidx.lifecycle.*
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.data.Utxo
import com.samourai.sentinel.data.db.dao.UtxoDao
import com.samourai.sentinel.data.repository.CollectionRepository
import org.koin.java.KoinJavaComponent.inject

/**
 * sentinel-android
 *
 * @author sarath
 */
internal class UtxoActivityViewModel(private val pubKeyCollection: PubKeyCollection) : ViewModel() {


    private val collectionRepository: CollectionRepository by inject(CollectionRepository::class.java)
    private val utxoDao: UtxoDao by inject(UtxoDao::class.java)

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


    fun getUtxo(pubKey: String): LiveData<List<Utxo>> {
        return utxoDao.getUTXObyCollectionAndPubKey(pubKeyCollection.id, pubKey)
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
