package com.samourai.sentinel.data.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.db.DbHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.util.*
import kotlin.collections.ArrayList

class CollectionRepository {

    val pubKeyCollections: ArrayList<PubKeyCollection> = arrayListOf();
    val collectionsLiveData: MutableLiveData<ArrayList<PubKeyCollection>> = MutableLiveData();

    private val dbHandler: DbHandler by inject(DbHandler::class.java)

    fun addNew(pubKeyCollection: PubKeyCollection) {
        pubKeyCollection.id = UUID.randomUUID().toString()
        pubKeyCollections.add(pubKeyCollection);
        this.sync()
    }

    fun delete(index: Int) {
        pubKeyCollections.removeAt(index);
        this.sync()
    }

    fun update(pubKeyCollection: PubKeyCollection, index: Int) {
        pubKeyCollections[index] = pubKeyCollection
        Log.i("collection:update", "${pubKeyCollections[index]}")
        this.sync()
    }


    fun findById(id: String): PubKeyCollection? {
       return pubKeyCollections.find { it.id == id }
    }

    init {
        this.read()
    }

    /**
     * write changes to the db
     * sync needs to be called after changing collection (edit,delete,add)
     */
    private fun sync() {
        GlobalScope.launch(Dispatchers.IO) {
            dbHandler.getCollectionStore()?.write("collections", pubKeyCollections);
        }
        this.emit()
    }


    fun read() = GlobalScope.launch(Dispatchers.IO) {
        try {
            val readValue: ArrayList<PubKeyCollection> = dbHandler.getCollectionStore()?.read("collections")
                    ?: arrayListOf();
            if (readValue.size != 0) {
                pubKeyCollections.clear()
                pubKeyCollections.addAll(readValue);
            } else {
                pubKeyCollections.clear();
            }
        } catch (e: Exception) {
            pubKeyCollections.clear()
            throw  e;
        }
        collectionsLiveData.postValue(pubKeyCollections);
    }


    private fun emit() {
        collectionsLiveData.postValue(pubKeyCollections);
    }

}