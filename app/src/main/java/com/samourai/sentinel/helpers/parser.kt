package com.samourai.sentinel.helpers

import com.google.gson.Gson
import java.lang.Exception
import java.lang.reflect.Type

val gson: Gson = Gson()

fun Any.toJSON(): String? {
    return try {
        Gson().toJson(this)
    } catch (Ex: Exception) {
        null
    }
}

inline fun <reified T> fromJSON(payload: String, type: Type? = null): T? {
    return try {
        return if(type!=null){
            gson.fromJson<T>(payload, type)
        }else{
            gson.fromJson<T>(payload, T::class.java)
        }
    } catch (Ex: Exception) {
        null
    }
}