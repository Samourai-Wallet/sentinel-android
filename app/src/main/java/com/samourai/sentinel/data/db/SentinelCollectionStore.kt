package com.samourai.sentinel.data.db

import android.content.Context
import org.koin.java.KoinJavaComponent.inject


class SentinelCollectionStore {

    private val context: Context by inject(Context::class.java)

    private val storeLocation = "${this.context.filesDir.path}/app_wallet"

    fun getCollectionStore(): PayloadRecord {
        return PayloadRecord(storeLocation, "collections.payload")
    }


    fun getDojoStore(): PayloadRecord {
        return PayloadRecord(storeLocation, "dojo.payload")
    }

    fun getUtxoMetaData(): PayloadRecord {
        return PayloadRecord(storeLocation, "utxo.meta.payload")
    }

}