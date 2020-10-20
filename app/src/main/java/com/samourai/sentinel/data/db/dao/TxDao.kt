package com.samourai.sentinel.data.db.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samourai.sentinel.data.Tx

/**
 * sentinel-android
 *
 */
@Dao
interface TxDao {

    @Query("SELECT * from transactions WHERE collectionId=:collectionID  ORDER BY time ASC LIMIT 200")
    fun getAllTx(collectionID: String): LiveData<List<Tx>>


    @Query("SELECT * from transactions WHERE collectionId=:collectionID  AND associatedPubKey=:associatedPubKey ORDER BY time ")
    fun getTxByPubKey(collectionID: String, associatedPubKey: String): DataSource.Factory<Int, Tx>

    @Query("SELECT * from transactions WHERE collectionId=:collectionID AND associatedPubKey=:pubKey  ORDER BY time ASC LIMIT 70")
    fun getAssociated(collectionID: String, pubKey: String): List<Tx>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: Tx)

    @Query("DELETE FROM transactions")
    suspend fun delete()

    @Query("DELETE FROM transactions WHERE collectionId=:collectionID AND associatedPubKey=:pubKey")
    fun deleteRelatedCollection(collectionID: String, pubKey: String)
}