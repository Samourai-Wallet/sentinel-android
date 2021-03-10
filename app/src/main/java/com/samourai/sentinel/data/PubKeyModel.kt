package com.samourai.sentinel.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PubKeyModel(val pubKey: String,
                       var label: String,
                       var type: AddressTypes?,
                       var balance: Long = 0L,
                       var change_index: Int = 0,
                       var account_index: Int = 0
) : Parcelable {
    fun getPurpose(): Int {
        return  when(type){
            AddressTypes.BIP49->{
                return  49
            }
            AddressTypes.BIP84->{
                return  84
            }
            AddressTypes.BIP44->{
                return  44
            }
            else -> 0
        }
    }
}
