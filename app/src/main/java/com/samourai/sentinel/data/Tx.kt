package com.samourai.sentinel.data

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class Tx(
        val hash: String,
        val time: Long,
        var associatedPubKey: String = "",
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


data class TxResponse(val n_tx: Int, val page: Int, val n_tx_page: Int, val txs: List<Tx>, val block_height: Long)

data class TxRvModel(var section: String?, var time: Long, val tx: Tx = Tx("", 0, associatedPubKey = "", confirmations = 0, inputs = arrayListOf(), out = arrayListOf(), block_height = 0, result = 0, locktime = 0, version = 0))