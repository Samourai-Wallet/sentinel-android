package com.samourai.sentinel.ui.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlin.reflect.KProperty


const val CURRENT_FIAT = "currentFiat"
const val CURRENT_FIAT_SEL = "currentFiatSel"
const val CURRENT_EXCHANGE = "currentExchange"
const val CURRENT_EXCHANGE_SEL = "currentExchangeSel"
const val BLOCK_EXPLORER = "blockExplorer"
const val FIRST_RUN = "1stRun"
const val SIM_IMSI = "IMSI"
const val ENABLE_TOR = "ENABLE_TOR"
const val PIN_HASH = "pinHash"
const val XPUB = "xpub"
const val SCRAMBLE_PIN = "scramblePin"
const val HAPTIC_PIN = "hapticPin"
const val CURRENT_FEE_TYPE = "currentFeeType"
const val TESTNET = "testnet"
const val USE_RICOCHET = "useRicochet"
const val RICOCHET_STAGGERED = "ricochetStaggeredDelivery"
const val USE_LIKE_TYPED_CHANGE = "useLikeTypedChange"

class PrefsUtil(context: Context) : Preferences(context, "${context.packageName}_preferences") {
    var currentFiat by stringPref(defaultValue = "")
    var currentExchange by stringPref(defaultValue = "")
    var lastSynced by  longPref(defaultValue = 0)
    var enableTor by booleanPref(defaultValue = false)
    var currentFeeType by intPref(defaultValue = 0)
    var haptics by booleanPref(defaultValue = true)
    var pinHash by stringPref(defaultValue = "")
    var displaySecure by booleanPref(defaultValue = false)
    var scramblePin by booleanPref(defaultValue = false)
    var pinEnabled by booleanPref(defaultValue = false)
    var authorization by stringPref(defaultValue = "")
    var exchangeSelection by stringPref(defaultValue = "localbitcoins.com")
    var exchangeRate by longPref(defaultValue = 1L)
    var selectedCurrency by stringPref(defaultValue = "USD")
    var refreshToken by stringPref(defaultValue = "")
    var apiEndPoint by stringPref(defaultValue = null)
    var selectedExplorer by stringPref(defaultValue = "oxt.me")
    var apiEndPointTor by stringPref(defaultValue = null)
    var testnet by booleanPref(defaultValue = false)
    var ricochetStaggeredDelivery by booleanPref(defaultValue = false)
    var useRicochet by booleanPref(defaultValue = false)
    var offlineMode by booleanPref(defaultValue = false)
    var blockHeight by longPref(defaultValue = 0L)
    var firstRun by booleanPref(defaultValue = true)

    fun isAPIEndpointEnabled(): Boolean {
        return !this.apiEndPointTor.isNullOrEmpty() && !this.apiEndPoint.isNullOrEmpty()
    }
}


@Suppress("UNCHECKED_CAST", "unused")
abstract class Preferences(private var context: Context, private val name: String? = null) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(name ?: javaClass.simpleName, Context.MODE_PRIVATE)
    }

    private val listeners = mutableListOf<SharedPrefsListener>()

    abstract class PrefDelegate<T>(val prefKey: String?) {
        abstract operator fun getValue(thisRef: Any?, property: KProperty<*>): T
        abstract operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
    }

    interface SharedPrefsListener {
        fun onSharedPrefChanged(property: KProperty<*>)
    }

    fun addListener(sharedPrefsListener: SharedPrefsListener) {
        listeners.add(sharedPrefsListener)
    }

    fun removeListener(sharedPrefsListener: SharedPrefsListener) {
        listeners.remove(sharedPrefsListener)
    }

    fun clearListeners() = listeners.clear()

    enum class StorableType {
        String,
        Int,
        Float,
        Boolean,
        Long,
        StringSet
    }

    inner class GenericPrefDelegate<T>(prefKey: String? = null, private val defaultValue: T?, val type: StorableType) :
            PrefDelegate<T?>(prefKey) {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? =
                when (type) {
                    StorableType.String ->
                        prefs.getString(prefKey ?: property.name, defaultValue as String?) as T?
                    StorableType.Int ->
                        prefs.getInt(prefKey ?: property.name, defaultValue as Int) as T?
                    StorableType.Float ->
                        prefs.getFloat(prefKey ?: property.name, defaultValue as Float) as T?
                    StorableType.Boolean ->
                        prefs.getBoolean(prefKey ?: property.name, defaultValue as Boolean) as T?
                    StorableType.Long ->
                        prefs.getLong(prefKey ?: property.name, defaultValue as Long) as T?
                    StorableType.StringSet ->
                        prefs.getStringSet(prefKey
                                ?: property.name, defaultValue as Set<String>) as T?
                }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            listeners.forEach { it.onSharedPrefChanged(property) }
            when (type) {
                StorableType.String -> {
                    prefs.edit().putString(prefKey ?: property.name, value as String?).apply()
                }
                StorableType.Int -> {
                    prefs.edit().putInt(prefKey ?: property.name, value as Int).apply()
                }
                StorableType.Float -> {
                    prefs.edit().putFloat(prefKey ?: property.name, value as Float).apply()
                }
                StorableType.Boolean -> {
                    prefs.edit().putBoolean(prefKey ?: property.name, value as Boolean).apply()
                }
                StorableType.Long -> {
                    prefs.edit().putLong(prefKey ?: property.name, value as Long).apply()
                }
                StorableType.StringSet -> {
                    prefs.edit().putStringSet(prefKey
                            ?: property.name, value as Set<String>).apply()
                }
            }
        }

    }

    fun stringPref(prefKey: String? = null, defaultValue: String? = null) =
            GenericPrefDelegate(prefKey, defaultValue, StorableType.String)

    fun intPref(prefKey: String? = null, defaultValue: Int = 0) =
            GenericPrefDelegate(prefKey, defaultValue, StorableType.Int)

    fun floatPref(prefKey: String? = null, defaultValue: Float = 0f) =
            GenericPrefDelegate(prefKey, defaultValue, StorableType.Float)

    fun booleanPref(prefKey: String? = null, defaultValue: Boolean = false) =
            GenericPrefDelegate(prefKey, defaultValue, StorableType.Boolean).let { it }

    fun longPref(prefKey: String? = null, defaultValue: Long = 0L) =
            GenericPrefDelegate(prefKey, defaultValue, StorableType.Long)

    fun stringSetPref(prefKey: String? = null, defaultValue: Set<String> = HashSet()) =
            GenericPrefDelegate(prefKey, defaultValue, StorableType.StringSet)


    fun clearAll(){
        prefs.edit().clear().apply()
    }
}