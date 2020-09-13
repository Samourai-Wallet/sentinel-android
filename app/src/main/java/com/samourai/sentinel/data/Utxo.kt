package com.samourai.sentinel.data

import com.google.gson.annotations.SerializedName


data class Utxo(
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
        @SerializedName("xpub")
        var xpub: Xpub? = null,

        //helper field to indicate section in a recyclerview
        var section: String? = null,
        var selected: Boolean = false

) {
    fun isBelongsToPubKey(pubKey: String): Boolean {

        var belongsTo = false

        this.xpub?.let {
            if (pubKey == this.xpub?.m) {
                belongsTo = true
            }
        }

        this.addr?.let {
            if (pubKey == this.addr) {
                belongsTo = true
            }
        }
        return belongsTo
    }

    fun getAssociatedPubKey(): String {

        if (this.xpub != null) {
            if (this.xpub!!.m != null) {
                return this.xpub?.m!!
            }
        }
        if (this.addr != null) {
            return this.addr!!
        }
        return ""
    }
}