package com.samourai.sentinel.ui.collectionDetails.receive

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.repository.TransactionsRepository
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.inject

class ReceiveViewModel : ViewModel() {


    private val transactionsRepository: TransactionsRepository by  inject(TransactionsRepository::class.java)


    private val _balance = MutableLiveData("")


    init {

    }



    fun setCollection(collection: PubKeyCollection) {
//        transactionsRepository.
    }
    val balance: LiveData<String> = _balance


}