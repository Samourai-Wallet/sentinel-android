package com.samourai.sentinel.data

import java.util.*

data class CollectionModel(
        var collectionLabel: String = "",
        var balance: Long = 0L,
        var id:String = "",
        val pubs: List<PubKeyModel> = listOf()
)
