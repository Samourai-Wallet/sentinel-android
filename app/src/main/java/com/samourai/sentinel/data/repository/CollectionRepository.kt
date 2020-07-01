package com.samourai.sentinel.data.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.samourai.sentinel.data.CollectionModel
import com.samourai.sentinel.data.db.DbHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.util.*
import kotlin.collections.ArrayList

class CollectionRepository {

    val collections: ArrayList<CollectionModel> = arrayListOf();
    val collectionsLiveData: MutableLiveData<ArrayList<CollectionModel>> = MutableLiveData();

    private val dbHandler: DbHandler by inject(DbHandler::class.java)

    fun addNew(collectionModel: CollectionModel) {
        collectionModel.id = UUID.randomUUID().toString()
        collections.add(collectionModel);
        this.sync()
    }

    fun delete(index: Int) {
        collections.removeAt(index);
        this.sync()
    }

    fun update(collectionModel: CollectionModel, index: Int) {
        collections[index] = collectionModel
        Log.i("collection","${collections[index]}")
        this.sync()
    }

    init {
        this.read();
    }

    /**
     * write changes to the db
     * sync needs to be called after changing collection (edit,delete,add)
     */
    private fun sync() {
        GlobalScope.launch(Dispatchers.IO) {
            dbHandler.getCollectionStore()?.write("collections", collections);
        }
        this.emit()
    }


    fun read() = GlobalScope.launch(Dispatchers.IO) {
        try {
            val readValue: ArrayList<CollectionModel> = dbHandler.getCollectionStore()?.read("collections")
                    ?: arrayListOf();
            if (readValue.size != 0) {
                collections.clear()
                collections.addAll(readValue);
            } else {
                collections.clear();
            }
        } catch (e: Exception) {
            collections.clear()
            throw  e;
        }
        collectionsLiveData.postValue(collections);
    }


    private fun emit() {
        collectionsLiveData.postValue(collections);
    }

}