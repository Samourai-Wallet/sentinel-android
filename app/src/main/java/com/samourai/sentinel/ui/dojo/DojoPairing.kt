package com.samourai.sentinel.ui.dojo

import com.google.gson.annotations.SerializedName


data class DojoPairing(
    @SerializedName("pairing")
    val pairing: Pairing? = Pairing()
)
data class Pairing(
        @SerializedName("apikey")
        val apikey: String? = "",
        @SerializedName("type")
        val type: String? = "",
        @SerializedName("url")
        val url: String? = "",
        @SerializedName("version")
        val version: String? = ""
)