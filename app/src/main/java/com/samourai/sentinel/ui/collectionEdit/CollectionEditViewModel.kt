package com.samourai.sentinel.ui.collectionEdit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.data.repository.TransactionsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent

class CollectionEditViewModel : ViewModel() {
    private val repository: CollectionRepository by KoinJavaComponent.inject(CollectionRepository::class.java)
    private val transactionsRepository: TransactionsRepository by KoinJavaComponent.inject(TransactionsRepository::class.java)
    val message: MutableLiveData<String> = MutableLiveData("")
    private val pubKeyCollectionLiveData: MutableLiveData<PubKeyCollection> = MutableLiveData()
    private val pubKeys: MutableLiveData<ArrayList<PubKeyModel>> = MutableLiveData()

    private fun addNew(pubKeyCollection: PubKeyCollection) {
        repository.addNew(pubKeyCollection)
    }


    fun save() {
        val collection = pubKeyCollectionLiveData.value
        if (collection != null) {
            if (collection.id == "") {
                if (pubKeys.value != null) {
                    collection.pubs = pubKeys.value!!
                }
                addNew(collection)
                message.postValue("Saved...")
            } else {
                collection.pubs = pubKeys.value!!
                repository.update(collection)
                pubKeyCollectionLiveData.postValue(collection)
                message.postValue("Collection Updated")
            }
        }

    }


    fun setCollection(pubKeyCollection: PubKeyCollection) {
        pubKeyCollectionLiveData.postValue(pubKeyCollection)
    }

    fun setPubKeys(items: ArrayList<PubKeyModel>) {
        pubKeys.postValue(items)
    }

    fun getCollection(): LiveData<PubKeyCollection> {
        return pubKeyCollectionLiveData;
    }

    fun getPubKeys(): LiveData<ArrayList<PubKeyModel>> {
        return pubKeys
    }

    fun removePubKey(it: Int) {
        val keys = pubKeys.value
        keys?.removeAt(it)
        pubKeys.postValue(keys)
        this.save()
    }

    fun updateKey(index: Int, model: PubKeyModel) {
        val keys = pubKeys.value
        if (keys != null) {
            keys[index] = model
            this.pubKeys.postValue(keys)
        }
        this.save()
    }

    suspend fun removeCollection(id: String) {
        val collection = repository.findById(id)
        collection?.pubs?.forEach {
            transactionsRepository.removeTxsRelatedToPubKey(it, collection.id)
        }
        repository.remove(id)
    }


}