package com.samourai.sentinel.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.core.segwit.bech32.Bech32Util
import com.samourai.wallet.send.MyTransactionOutPoint
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.script.Script
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.util.*

@Entity(tableName = "utxos")
data class Utxo(
        @PrimaryKey()
        var idx: String = "",
        @SerializedName("addr")
        var addr: String? = null,
        @SerializedName("confirmations")
        var confirmations: Int? = null,
        @SerializedName("script")
        var script: String? = null,
        @SerializedName("tx_hash")
        var txHash: String? = null,
        @SerializedName("tx_locktime")
        var txLocktime: Long? = null,
        @SerializedName("tx_output_n")
        var txOutputN: Int? = null,
        @SerializedName("tx_version")
        var txVersion: Int? = null,
        @SerializedName("value")
        var value: Long? = null,
        @SerializedName("path")
        var path: String = "",

        var pubKey: String = "",

        var collectionId: String = "",

        //helper field to indicate section in a recyclerview
        var section: String? = null,
        var selected: Boolean = false,

        @Ignore
        var xpub: Xpub? = null,
) {

    fun getOutPoints(): MyTransactionOutPoint {
        val hashBytes = Hex.decode(txHash)
        val txHash = Sha256Hash.wrap(hashBytes)
        val value = value?.let { BigInteger.valueOf(it) }
        val scriptBytes = Hex.decode(script)
        var address: String? = null
        address = if (Bech32Util.getInstance().isBech32Script(script)) {
            Bech32Util.getInstance().getAddressFromScript(script)
        } else {
            Script(scriptBytes).getToAddress(SentinelState.getNetworkParam()).toString()
        }
        // Construct the output
        val outPoint = MyTransactionOutPoint(SentinelState.getNetworkParam(), txHash, txOutputN!!, value, scriptBytes, address)
        outPoint.confirmations = confirmations!!
        return outPoint
    }

    // sorts in descending order by amount
    class UTXOComparator : Comparator<Utxo> {
        override fun compare(utxo1: Utxo, utxo2: Utxo): Int {
            val BEFORE = -1
            val EQUAL = 0
            val AFTER = 1
            return when {
                utxo1.value!! > utxo2.value!! -> {
                    BEFORE
                }
                utxo1.value!! < utxo2.value!! -> {
                    AFTER
                }
                else -> {
                    EQUAL
                }
            }
        }
    }

    // sorts in descending order by amount
    class OutpointComparator : Comparator<MyTransactionOutPoint> {
        override fun compare(o1: MyTransactionOutPoint, o2: MyTransactionOutPoint): Int {
            val BEFORE = -1
            val EQUAL = 0
            val AFTER = 1
            return if (o1.value.longValue() > o2.value.longValue()) {
                BEFORE
            } else if (o1.value.longValue() < o2.value.longValue()) {
                AFTER
            } else {
                EQUAL
            }
        }
    }

}