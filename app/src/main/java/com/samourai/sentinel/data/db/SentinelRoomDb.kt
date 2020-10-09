package com.samourai.sentinel.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.data.TxInputConverter
import com.samourai.sentinel.data.Utxo
import com.samourai.sentinel.data.db.dao.TxDao
import com.samourai.sentinel.data.db.dao.UtxoDao

@Database(entities = [Tx::class, Utxo::class], version = 1, exportSchema = false)
@TypeConverters(TxInputConverter::class)
abstract class SentinelRoomDb : RoomDatabase() {

    abstract fun txDao(): TxDao
    abstract fun utxoDao(): UtxoDao

    companion object {
        @Volatile
        private var INSTANCE: SentinelRoomDb? = null
        fun getDatabase(context: Context): SentinelRoomDb {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        SentinelRoomDb::class.java,
                        "sentinel_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}