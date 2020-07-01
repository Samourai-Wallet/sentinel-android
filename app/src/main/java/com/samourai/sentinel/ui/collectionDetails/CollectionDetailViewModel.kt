package com.samourai.sentinel.ui.collectionDetails

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.samourai.sentinel.data.CollectionModel
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.data.repository.CollectionRepository
import org.koin.java.KoinJavaComponent

class CollectionDetailViewModel : ViewModel() {
    private val repository: CollectionRepository by KoinJavaComponent.inject(CollectionRepository::class.java)
    val message: MutableLiveData<String> = MutableLiveData("")
    private val collectionLiveData: MutableLiveData<CollectionModel> = MutableLiveData()
    private val pubKeys: MutableLiveData<ArrayList<PubKeyModel>> = MutableLiveData()

    private fun addNew(collection: CollectionModel) {
        repository.addNew(collection)
    }

    private fun saveExisting(collectionModel: CollectionModel, index: Int) {
        repository.update(collectionModel, index)
    }

    fun save() {
        val collection = collectionLiveData.value
        if (collection != null) {
            if (collection.id == "") {
                if (pubKeys.value != null) {
                    collection.pubs = pubKeys.value!!
                }
                addNew(collection)
                message.postValue("Saved...")
            } else {
                saveExisting(collection, repository.collections.indexOf(collection))
                message.postValue("Collection Updated")
            }
        }

    }


    fun setCollection(collectionModel: CollectionModel) {
        collectionLiveData.postValue(collectionModel)
    }

    fun setPubKeys(items: ArrayList<PubKeyModel>) {
        pubKeys.postValue(items);
    }

    fun getCollection(): MutableLiveData<CollectionModel> {
        return collectionLiveData;
    }

    fun getPubKeys(): MutableLiveData<ArrayList<PubKeyModel>> {
        return pubKeys;
    }

    fun removePubKey(it: Int) {
        val keys = pubKeys.value;
        keys?.removeAt(it)
        pubKeys.postValue(keys)
        this.save()
    }

    fun updateKey(index: Int, model: PubKeyModel) {
        val keys = pubKeys.value
        if (keys != null) {
            keys[index] = model;
            this.pubKeys.postValue(keys);
        }
        this.save()
    }


}