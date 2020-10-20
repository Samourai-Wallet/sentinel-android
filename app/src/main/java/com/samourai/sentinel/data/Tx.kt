package com.samourai.sentinel.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.samourai.sentinel.helpers.fromJSON
import com.samourai.sentinel.helpers.toJSON
import org.json.JSONObject
import java.lang.reflect.Type

@Entity(tableName = "transactions")
data class Tx(
        @PrimaryKey
        val hash: String,
        val time: Long,
        var associatedPubKey: String = "",
        var collectionId: String = "",
        val version: Int,
        val locktime: Int,
        val result: Long?,
        val inputs: List<Inputs>,
        val out: List<Out>,
        val block_height: Long?,
        var confirmations: Long = 0
) {
    fun isBelongsToPubKey(pubKey: String): Boolean {

        var belongsTo = false

        this.inputs.forEach {
            if (it.prev_out?.addr == pubKey) {
                belongsTo = true
            } else if (it.prev_out?.xpub != null) {
                if (it.prev_out.xpub.m == pubKey) {
                    belongsTo = true
                }
            }
        }
        this.out.forEach {
            if (it.addr == pubKey) {
                belongsTo = true
            } else if (it.xpub != null) {
                if (it.xpub.m == pubKey) {
                    belongsTo = true
                }
            }
        }


        return belongsTo
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tx

        if (hash != other.hash) return false
        if (time != other.time) return false
        if (confirmations != other.confirmations) return false
        if (associatedPubKey != other.associatedPubKey) return false
        if (version != other.version) return false
        if (block_height != other.block_height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + associatedPubKey.hashCode()
        result = 31 * result + version
        result = 31 * result + (block_height?.hashCode() ?: 0)
        return result
    }

}

data class Out(
        val n: Int,
        val value: Long,
        val addr: String?,
        val xpub: Xpub?
)


data class Inputs(
        val vin: Int,
        val sequence: Long?,
        val prev_out: prevOut?
)

data class Xpub(
        val m: String?,
        val path: String?
)

data class prevOut(
        val addr: String?,
        val txid: String,
        val value: Long,
        val vout: Int,
        val xpub: Xpub?
)


data class Wallet(
        val final_balance: Long
)

data class Info(
        val latest_block: LatestBlock,
        val fees: JSONObject?
)

data class LatestBlock(
        val hash: String,
        val height: Long,
        val time: Long
)

data class Address(
        @SerializedName("account_index")
        var accountIndex: Int? = null,
        @SerializedName("address")
        var address: String? = null,
        @SerializedName("change_index")
        var changeIndex: Int? = null,
        @SerializedName("final_balance")
        var finalBalance: Long? = null,
        @SerializedName("n_tx")
        var nTx: Int? = null
)


data class WalletResponse(
        val info: Info,
        val addresses: List<Address>?,
        val txs: List<Tx>,
        val wallet: Wallet,
        val unspent_outputs: List<Utxo>?
)

class TxInputConverter {
    @TypeConverter
    fun fromStringToInputs(value: String?): List<Inputs>? {
        return if (value == null) {
            arrayListOf()
        } else {
            val inputType: Type = object : TypeToken<List<Inputs>?>() {}.type
            fromJSON<List<Inputs>>(value, inputType) ?: arrayListOf()
        }
    }

    @TypeConverter
    fun toInputString(input: List<Inputs>): String? {
        return input.toJSON()
    }

    @TypeConverter
    fun fromStringToOuts(value: String?): List<Out> {
        return if (value == null) {
            arrayListOf()
        } else {
            val inputType: Type = object : TypeToken<List<Out>?>() {}.type
            fromJSON<List<Out>>(value, inputType) ?: arrayListOf()
        }
    }
    @TypeConverter
    fun toOutsString(input: List<Out>): String? {
        return input.toJSON()
    }
}

data class TxResponse(val n_tx: Int, val page: Int, val n_tx_page: Int, val txs: List<Tx>, val block_height: Long)

data class TxRvModel(var section: String?, var time: Long, val tx: Tx = Tx("", 0, associatedPubKey = "", confirmations = 0, inputs = arrayListOf(), out = arrayListOf(), block_height = 0, result = 0, locktime = 0, version = 0))