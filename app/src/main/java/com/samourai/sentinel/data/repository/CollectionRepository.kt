package com.samourai.sentinel.data.repository

import androidx.lifecycle.MutableLiveData
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.db.SentinelCollectionStore
import com.samourai.sentinel.util.dataBaseScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import java.util.*
import kotlin.collections.ArrayList

class CollectionRepository {

    val pubKeyCollections: ArrayList<PubKeyCollection> = arrayListOf()
    val collectionsLiveData: MutableLiveData<ArrayList<PubKeyCollection>> = MutableLiveData();

    private val dbHandler: SentinelCollectionStore by inject(SentinelCollectionStore::class.java)

    fun addNew(pubKeyCollection: PubKeyCollection) {
        pubKeyCollection.id = UUID.randomUUID().toString()
        pubKeyCollections.add(pubKeyCollection)
        this.sync()
    }

    fun delete(index: Int) {
        pubKeyCollections.removeAt(index);
        this.sync()
    }

    fun update(pubKeyCollection: PubKeyCollection, index: Int) {
        pubKeyCollections[index] = pubKeyCollection
        pubKeyCollections[index].updateBalance()
        this.sync()
    }


    fun findById(id: String): PubKeyCollection? {
        return pubKeyCollections.find { it.id == id }
    }

    /**
     * write changes to the db
     * sync needs to be called after changing collection (edit,delete,add)
     */
    @Synchronized
    fun sync() {
        val dupRemoved = pubKeyCollections.distinctBy { it.id }
        pubKeyCollections.clear()
        pubKeyCollections.addAll(dupRemoved)
        dataBaseScope.launch(Dispatchers.IO) {
            dbHandler.getCollectionStore().write(pubKeyCollections);
        }
        this.emit()
    }

    @Synchronized
    fun read() = dataBaseScope.launch(Dispatchers.IO) {
        pubKeyCollections.clear()
        try {
            val readValue: ArrayList<PubKeyCollection> = dbHandler.getCollectionStore().read()
                    ?: arrayListOf()
            if (readValue.size != 0) {
                pubKeyCollections.addAll(readValue.distinctBy { it.id })
            } else {
                pubKeyCollections.clear()
            }
        } catch (e: Exception) {
            pubKeyCollections.clear()
            throw  e
        }
        emit()
    }


    private fun emit() {
        val array = pubKeyCollections.distinctBy { it.id }
        collectionsLiveData.postValue(array as ArrayList<PubKeyCollection>)
//        if (array.isNotEmpty())
//            apiScope.launch(Dispatchers.Main) {
//
//            }
    }

    fun update(pubKeyCollection: PubKeyCollection) {
        if (this.pubKeyCollections.contains(pubKeyCollection)) {
            return this.update(pubKeyCollection, this.pubKeyCollections.indexOf(pubKeyCollection))
        }
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        val item = pubKeyCollections.find { it.id == id }
        pubKeyCollections.remove(item)
        sync()
        withContext(Dispatchers.Main) {
            collectionsLiveData.postValue(pubKeyCollections)
        }
    }

    fun reset() {
        this.pubKeyCollections.clear()
        this.sync()
    }

}