package com.samourai.sentinel.data.db

import android.content.Context
import android.util.Log
import io.paperdb.Book
import io.paperdb.Paper
import org.koin.java.KoinJavaComponent.inject


class DbHandler() {

    private val context: Context by inject(Context::class.java);

    private val storeLocation = "${this.context.filesDir.path}/app_wallet"


    fun getTxStore(): Book {
        return Paper.bookOn(storeLocation, "sentinel.txs")!!
    }

    fun getUTXOsStore(): Book? {
        return Paper.bookOn(storeLocation, "sentinel.utxo")
    }

    fun getCollectionStore(): Book? {
        return Paper.bookOn(storeLocation, "sentinel.collections")
    }
}