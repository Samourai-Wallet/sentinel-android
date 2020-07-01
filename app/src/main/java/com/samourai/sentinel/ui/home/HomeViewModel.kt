package com.samourai.sentinel.ui.home

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samourai.sentinel.data.CollectionModel
import com.samourai.sentinel.data.repository.CollectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import java.sql.Time

class HomeViewModel : ViewModel() {


    private val repository: CollectionRepository by inject(CollectionRepository::class.java)

    var collections: MutableLiveData<ArrayList<CollectionModel>> = MutableLiveData()
    private val errorMessage: MutableLiveData<String> = MutableLiveData();


    init {
        viewModelScope.launch {
            try {
                collections = repository.collectionsLiveData;
                repository.read()
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage.postValue(e.message);
            }

        }
    }

    fun addNewCollection() {
        val stamp = System.currentTimeMillis();
        repository.addNew(CollectionModel(collectionLabel = "HELLO ${stamp}", pubs = arrayListOf(), balance = stamp))
    }

    fun setVal(list: ArrayList<CollectionModel>?) {
        collections.postValue(list);
    }

}