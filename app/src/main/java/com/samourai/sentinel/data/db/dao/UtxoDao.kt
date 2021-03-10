package com.samourai.sentinel.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.data.Utxo

/**
 * sentinel-android
 *
 */
@Dao
interface UtxoDao {

    @Query("SELECT * from utxos WHERE collectionId=:collectionId")
    fun getUTXObyCollection(collectionId: String): LiveData<List<Utxo>>

    @Query("SELECT * from utxos WHERE collectionId=:collectionId")
    fun getUTXObyCollectionAsList(collectionId: String):  List<Utxo>

    @Query("SELECT * from utxos WHERE collectionId=:collectionId AND pubKey=:pubKey")
    fun getUTXObyCollectionAndPubKey(collectionId: String,pubKey: String): LiveData<List<Utxo>>

    @Query("SELECT * from utxos WHERE pubKey=:pubKey")
    fun getUtxoWithPubKey(pubKey: String): LiveData<List<Utxo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(utxo: Utxo)

    @Query("DELETE FROM utxos")
    suspend fun delete()

    @Query("DELETE FROM utxos WHERE txHash=:txHash AND txOutputN=:outputN")
    suspend fun delete(txHash: String,outputN:Int)

    @Query("DELETE FROM utxos WHERE pubKey=:pubKey")
    suspend fun deleteByPubKey(pubKey: String)

    @Query("DELETE FROM utxos WHERE collectionId=:collectionId")
    suspend fun deleteByCollection(collectionId: String)
}