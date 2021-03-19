package com.samourai.sentinel.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samourai.sentinel.api.ApiService
import com.samourai.sentinel.data.db.SentinelCollectionStore
import com.samourai.sentinel.helpers.toJSON
import com.samourai.sentinel.send.SuggestedFee
import com.samourai.sentinel.util.dataBaseScope
import com.samourai.wallet.util.FeeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.math.BigInteger
import kotlin.math.roundToInt

/**
 * sentinel-android
 */
class FeeRepository : FeeUtil() {

    private val apiService: ApiService by inject(ApiService::class.java)
    private var fees: MutableLiveData<ArrayList<SuggestedFee>> = MutableLiveData(arrayListOf())
    private val sentinelCollectionStore: SentinelCollectionStore by inject(SentinelCollectionStore::class.java)

    private var lowFee: SuggestedFee? = null
    private var normalFee: SuggestedFee? = null
    private var highFee: SuggestedFee? = null
    private var suggestedFee: SuggestedFee? = null

    companion object {
        const val FEE_LOW = 0
        const val FEE_NORMAL = 1
        const val FEE_PRIORITY = 2
        const val FEE_CUSTOM = 3
    }

    init {
        init()
    }

    fun init() {
        dataBaseScope.launch(Dispatchers.IO) {
            val json = sentinelCollectionStore.getFee().read<JSONObject>()
            if (json != null) {
                parse(json)
            }
        }
    }

    suspend fun getDynamicFees() {
        try {
            val response = apiService.getFees()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string())
                parse(json)
            }
        } catch (Ex: Exception) {
            throw Exception(Ex.message)
        }
    }

    public fun parse(jsonObject: JSONObject) {
        val feeParsed: ArrayList<SuggestedFee> = arrayListOf();
        if (jsonObject.has("2")) {
            val fee: Long = jsonObject.getInt("2").toLong()
            val suggestedFee = SuggestedFee()
            suggestedFee.defaultPerKB = BigInteger.valueOf(fee * 1000L)
            suggestedFee.isStressed = false
            suggestedFee.isOK = true
            suggestedFee.blockDelay = 2
            feeParsed.add(suggestedFee)
        }

        if (jsonObject.has("6")) {
            val fee: Long = jsonObject.getInt("6").toLong()
            val suggestedFee = SuggestedFee()
            suggestedFee.defaultPerKB = BigInteger.valueOf(fee * 1000L)
            suggestedFee.isStressed = false
            suggestedFee.isOK = true
            suggestedFee.blockDelay = 6
            feeParsed.add(suggestedFee)
        }

        if (jsonObject.has("24")) {
            val fee: Long = jsonObject.getInt("24").toLong()
            val suggestedFee = SuggestedFee()
            suggestedFee.defaultPerKB = BigInteger.valueOf(fee * 1000L)
            suggestedFee.isStressed = false
            suggestedFee.isOK = true
            suggestedFee.blockDelay = 24
            feeParsed.add(suggestedFee)
        }
        this.fees.postValue(feeParsed)
        setFees(feeParsed)
        saveState(jsonObject)
    }

    public fun setFees(estimatedFees: ArrayList<SuggestedFee>) {
        when (estimatedFees.size) {
            1 -> {
                suggestedFee = estimatedFees[0];
                normalFee = estimatedFees[0];
                highFee = estimatedFees[0];
                lowFee = estimatedFees[0];
            }
            2 -> {
                suggestedFee = estimatedFees[0]
                highFee = estimatedFees[0]
                normalFee = estimatedFees[0]
                lowFee = estimatedFees[1];
            }
            3 -> {
                highFee = estimatedFees[0];
                suggestedFee = estimatedFees[1];
                normalFee = estimatedFees[1];
                lowFee = estimatedFees[2];
            }
            else -> {
            }
        }
    }

    public fun getLowFee(): SuggestedFee {
        return if (lowFee == null) {
            getSuggested()
        } else {
            lowFee!!
        }
    }

    public fun getHighFee(): SuggestedFee {
        return if (highFee == null) {
            getSuggested()
        } else {
            highFee!!
        }
    }

    public fun getNormalFee(): SuggestedFee {
        return if (normalFee == null) {
            getSuggested()
        } else {
            normalFee!!
        }
    }

    public fun getSuggested(): SuggestedFee {
        return if (suggestedFee != null) {
            this.suggestedFee!!
        } else {
            val fee = SuggestedFee()
            fee.defaultPerKB = BigInteger.valueOf(10000L)
            fee
        }
    }

    public fun parse(map: Map<String, Long>) {
//        if (map.toJSON() != null)
//            this.parse(JSONObject(map.toJSON()!!))
    }

    private fun saveState(json: JSONObject) {
         Timber.i("saveState: $json")
        dataBaseScope.launch(Dispatchers.IO) {
            sentinelCollectionStore.getFee().write(json)
        }
    }

    fun estimatedFee(inputs: Int, outputs: Int): BigInteger? {
        val size = estimatedSize(inputs, outputs)
        return calculateFee(size, getSuggested().defaultPerKB)
    }

    fun estimatedFeeSegwit(inputsP2PKH: Int, inputsP2SHP2WPKH: Int, inputsP2WPKH: Int, outputs: Int): BigInteger? {
        val size: Int = estimatedSizeSegwit(inputsP2PKH, inputsP2SHP2WPKH, inputsP2WPKH, outputs, 0)
        return calculateFee(size, getSuggested().defaultPerKB)
    }

    fun estimatedSize(inputs: Int, outputs: Int): Int {
        return estimatedSizeSegwit(inputs, 0, 0, outputs, 0)
    }

    fun estimatedFee(inputs: Int, outputs: Int, feePerKb: BigInteger): BigInteger? {
        val size = estimatedSize(inputs, outputs)
        return calculateFee(size, feePerKb)
    }

    fun calculateFee(txSize: Int, feePerKb: BigInteger): BigInteger {
        val feePerB: Long = toFeePerB(feePerKb)
        val fee: Long = calculateFee(txSize, feePerB)
        return BigInteger.valueOf(fee)
    }
    fun getFees(): LiveData<ArrayList<SuggestedFee>> {
        return fees
    }

    private fun toFeePerB(feePerKb: BigInteger): Long {
        return (feePerKb.toDouble() / 1000.0) .toLong()
    }
    fun sanitizeFee() {
        if (suggestedFee != null)
            if (suggestedFee!!.defaultPerKB.toLong() < 1000L) {
                val suggestedFee = SuggestedFee()
                suggestedFee.defaultPerKB = BigInteger.valueOf(1200L)
                setSuggested(suggestedFee)
            }
    }

    public fun setLowFee(fee: SuggestedFee) {
        lowFee = fee
    }

    public fun setHighFee(fee: SuggestedFee) {
        highFee = fee
    }

    public fun setNormalFee(fee: SuggestedFee) {
        normalFee = fee
    }

    public fun setSuggested(fee: SuggestedFee) {
        suggestedFee = fee
    }
}