package com.samourai.sentinel.data

enum class AddressTypes {
    BIP44 {
        override fun toString(): String {
            return "BIP44"
        }
    },
    BIP49,
    BIP84,
    ADDRESS
}