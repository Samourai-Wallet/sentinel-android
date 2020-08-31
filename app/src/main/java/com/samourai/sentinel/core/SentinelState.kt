package com.samourai.sentinel.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samourai.sentinel.data.LatestBlock
import com.samourai.sentinel.ui.dojo.DojoUtility
import com.samourai.sentinel.ui.utils.Preferences
import com.samourai.sentinel.ui.utils.PrefsUtil
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.TestNet3Params
import org.koin.java.KoinJavaComponent.inject
import java.math.BigInteger
import java.net.Proxy
import kotlin.reflect.KProperty

/**
 * Utility class for handling basic app states
 */
class SentinelState {


    enum class TorState {
        WAITING,
        ON,
        OFF
    }

    companion object {
        var torProxy: Proxy? = null
        private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java)
        private val dojoUtility: DojoUtility by inject(DojoUtility::class.java)
        public val bDust: BigInteger = BigInteger.valueOf(Coin.parseCoin("0.00000546").longValue())
        private var isOffline = false
        private var testnetParams: NetworkParameters? = NetworkParameters.fromID(NetworkParameters.ID_TESTNET)
        private var mainNetParams: NetworkParameters? = NetworkParameters.fromID(NetworkParameters.ID_MAINNET)

        private var networkParams: NetworkParameters? = mainNetParams

        var blockHeight: LatestBlock? = null

        private var torStateLiveData: MutableLiveData<TorState> = MutableLiveData(TorState.OFF)

        var torState: TorState = TorState.OFF
            set(value) {
                field = value
                torStateLiveData.postValue(value)
            }


        fun isTorStarted(): Boolean {
            return torState == TorState.ON
        }


        fun isConnected(): Boolean {
             return true
        }

        fun getNetworkParam(): NetworkParameters? {
            return this.networkParams
        }

        fun isTestNet(): Boolean {
            return getNetworkParam() is TestNet3Params
        }

        public fun torStateLiveData(): LiveData<TorState> {
            return torStateLiveData
        }

        init {
            readPrefs()
            prefsUtil.addListener(object : Preferences.SharedPrefsListener {
                override fun onSharedPrefChanged(property: KProperty<*>) {
                    readPrefs()
                }
            })
        }

        private fun readPrefs() {
            this.networkParams = if (prefsUtil.testnet!!) testnetParams else mainNetParams
            this.isOffline = prefsUtil.offlineMode!!
        }


        fun isTorRequired(): Boolean {
            return dojoUtility.isDojoEnabled() || prefsUtil.enableTor!!
        }

        fun isTorRequiredAndStarted(): Boolean {
            return isTorRequired() && isTorStarted()
        }

        fun isDojoEnabled(): Boolean {
            return dojoUtility.isDojoEnabled();
        }
    }
}
