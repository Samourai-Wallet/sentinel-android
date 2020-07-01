package com.samourai.sentinel.data

import java.util.*
import kotlin.collections.ArrayList

data class CollectionModel(
        var collectionLabel: String = "",
        var balance: Long = 0L,
        var id:String = "",
        var pubs: ArrayList<PubKeyModel> = arrayListOf()
)
