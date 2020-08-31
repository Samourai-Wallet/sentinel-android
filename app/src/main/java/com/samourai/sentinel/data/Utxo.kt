package com.samourai.sentinel.data


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Utxo(
    @JsonProperty("addr")
    var addr: String? = null,
    @JsonProperty("confirmations")
    var confirmations: Int? = null,
    @JsonProperty("script")
    var script: String? = null,
    @JsonProperty("tx_hash")
    var txHash: String? = null,
    @JsonProperty("tx_locktime")
    var txLocktime: Int? = null,
    @JsonProperty("tx_output_n")
    var txOutputN: Int? = null,
    @JsonProperty("tx_version")
    var txVersion: Int? = null,
    @JsonProperty("value")
    var value: Int? = null,
    @JsonProperty("xpub")
    var xpub: Xpub? = null
)