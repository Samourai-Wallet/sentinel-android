package com.samourai.sentinel.data.db

import android.content.Context
import io.paperdb.Book
import io.paperdb.Paper
import org.koin.java.KoinJavaComponent.inject


class DbHandler {

    private val context: Context by inject(Context::class.java)

    private val storeLocation = "${this.context.filesDir.path}/app_wallet"

    fun getTxStore(): Book {
        return Paper.bookOn(storeLocation, "sentinel.txs")!!
    }

    fun getUTXOsStore(): Book {
        return Paper.bookOn(storeLocation, "sentinel.utxo")
    }

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