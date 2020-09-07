package com.samourai.sentinel.ui.collectionDetails.receive

import androidx.lifecycle.*
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.data.repository.ExchangeRateRepository
import com.samourai.sentinel.data.repository.TransactionsRepository
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.util.MonetaryUtil
import org.bitcoinj.core.Coin
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.inject

class ReceiveViewModel : ViewModel() {

    var pubKeyCollection: PubKeyCollection? = null;

    fun setCollection(collection: PubKeyCollection) {
        this.pubKeyCollection = collection
    }


}