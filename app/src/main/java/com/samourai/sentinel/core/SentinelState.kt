package com.samourai.sentinel.core

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samourai.sentinel.data.LatestBlock
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.data.repository.ExchangeRateRepository
import com.samourai.sentinel.data.repository.TransactionsRepository
import com.samourai.sentinel.ui.dojo.DojoUtility
import com.samourai.sentinel.ui.utils.Preferences
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.util.apiScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.TestNet3Params
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
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

        private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java)
        private val dojoUtility: DojoUtility by inject(DojoUtility::class.java)
        private val transactionsRepository: TransactionsRepository by inject(TransactionsRepository::class.java)
        private val exchangeRateRepository: ExchangeRateRepository by inject(ExchangeRateRepository::class.java)
        private val collectionRepository: CollectionRepository by inject(CollectionRepository::class.java)
        private var testnetParams: NetworkParameters? = NetworkParameters.fromID(NetworkParameters.ID_TESTNET)
        private var mainNetParams: NetworkParameters? = NetworkParameters.fromID(NetworkParameters.ID_MAINNET)
        private var networkParams: NetworkParameters? = mainNetParams

        var blockHeight: LatestBlock? = null
        private var isOffline = false
        private var torStateLiveData: MutableLiveData<TorState> = MutableLiveData(TorState.OFF)

        private var countDownTimer: CountDownTimer? = null
        var torProxy: Proxy? = null

        //Shared field for passing tx object between activities and fragments
        var selectedTx: Tx? = null

        val bDust: BigInteger = BigInteger.valueOf(Coin.parseCoin("0.00000546").longValue())

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
            runApiChecking()
        }

        private fun runApiChecking() {
            //App will try to refresh balance every 3 minutes
            //If the user refreshed a recently this wont be executed
            countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 180000) {
                // This is called after every 3.5 min interval.
                override fun onTick(millisUntilFinished: Long) {

                    if (isTorRequired() && isTorStarted()) {
                        refreshCollection()
                    } else {
                        if (!isTorRequired()) {
                            refreshCollection()
                        }
                    }

                }

                override fun onFinish() {
                    start()
                    Timber.i("onFinish: ")
                }
            }.start()
        }

        private fun refreshCollection() {
            if (!isRecentlySynced()) {
                exchangeRateRepository.fetch()
                collectionRepository.pubKeyCollections.forEach {
                    val job = apiScope.launch {
                        try {
                            transactionsRepository.fetchFromServer(it)
                        } catch (e: Exception) {
                            Timber.e(e)
                            throw CancellationException(e.message)
                        }
                    }
                    job.invokeOnCompletion {
                        it?.let {
                            Timber.e(it)
                        }
                        if (it == null) {
                            prefsUtil.lastSynced = System.currentTimeMillis()
                        }
                    }
                }
            }

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


        fun isRecentlySynced(): Boolean {
            val lastSync = prefsUtil.lastSynced!!
            val currentTime = System.currentTimeMillis()
            return currentTime.minus(lastSync) < 60000
        }
    }
}
