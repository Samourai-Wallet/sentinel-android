package com.samourai.sentinel.data.db

import com.google.gson.reflect.TypeToken
import com.samourai.sentinel.core.access.AccessFactory
import com.samourai.sentinel.helpers.fromJSON
import com.samourai.sentinel.helpers.toJSON
import com.samourai.sentinel.ui.utils.logThreadInfo
import com.samourai.wallet.crypto.AESUtil
import com.samourai.wallet.util.CharSequenceX
import timber.log.Timber
import java.io.File

/**
 * For managing encrypted and non encrypted app payload files
 * this includes info like collections and its related pubKeys
 * Class itself will handle encryption and decryption when the pin code is set
 */
class PayloadRecord(private val location: String, val name: String) {

    val file = File("${this.location}${File.separatorChar}$name")

    inline fun <reified T> write(value: T, replace: Boolean = false) {
        logThreadInfo("Record write")
        if (file.exists()) {
            if (replace) {
                val data = this.read<T>()
                if (data is ArrayList<*>) {
                    (data as ArrayList<*>).addAll(value as Collection<Nothing>)
                    writeToFile(data.toJSON().toString())
                } else {
                    if (value != null) {
                        writeToFile(value.toJSON().toString())
                    }
                }
            } else {
                writeToFile(value?.toJSON().toString())
            }
        } else {
            if (!file.parentFile.exists())
                file.parentFile.mkdirs()
            file.createNewFile()
            if (value != null) {
                writeToFile(value.toJSON().toString())
            }
        }
    }

    inline fun <reified T> read(): T? {
        val itemType = object : TypeToken<T>() {}.type
        return if (file.exists()) {
            try {
                val string = decrypt(file.readText())
                return fromJSON<T>(string, itemType)
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        } else {
            null
        }
    }

    fun writeToFile(value: String) {
        val file = File("${this.location}${File.separatorChar}$name")
        file.writeText(encrypt(value))
    }

    private fun encrypt(value: String): String {
        val pin = AccessFactory.getInstance(null).pin
        if (!pin.isNullOrEmpty()) {
            return AESUtil.encryptSHA256(value, CharSequenceX(pin), AESUtil.DefaultPBKDF2Iterations);
        }
        return value
    }

    fun decrypt(value: String): String {
        val pin = AccessFactory.getInstance(null).pin

        if (!pin.isNullOrEmpty()) {
            return AESUtil.decryptSHA256(value, CharSequenceX(pin), AESUtil.DefaultPBKDF2Iterations);
        }

        return value
    }

}