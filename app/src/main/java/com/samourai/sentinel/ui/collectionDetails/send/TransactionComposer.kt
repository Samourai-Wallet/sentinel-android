package com.samourai.sentinel.ui.collectionDetails.send

import android.content.Context
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.core.SentinelState.Companion.bDust
import com.samourai.sentinel.core.hd.HD_Account
import com.samourai.sentinel.core.segwit.P2SH_P2WPKH
import com.samourai.sentinel.data.AddressTypes
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.data.Utxo
import com.samourai.sentinel.send.FeeUtil
import com.samourai.sentinel.send.PSBT
import com.samourai.sentinel.send.SendFactory
import com.samourai.wallet.segwit.SegwitAddress
import com.samourai.wallet.send.MyTransactionOutPoint
import com.samourai.wallet.send.UTXO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.codec.binary.Hex
import org.apache.commons.lang3.tuple.Triple
import org.bitcoinj.core.ECKey
import org.bitcoinj.params.MainNetParams
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.math.BigInteger
import java.util.*


/**
 * sentinel-android
 */
class TransactionComposer {

    data class ComposeException(private val displayMessage: String) : Exception(displayMessage)

    private val context: Context by inject(Context::class.java)
    private var changeECKey: ECKey? = null
    private var changeIndex: Int? = null
    private var psbt: PSBT? = null
    private var balance: Long = 0L
    private var selectedUtxos: ArrayList<UTXO> = arrayListOf()
    private var selectPubKeyModel: PubKeyModel? = null;
    private var address: String = ""
    private var amount: Double = 0.0
    private var selectedFee = 0L;
    private var receivers: HashMap<String, BigInteger> = hashMapOf()
    private lateinit var collection: PubKeyCollection;
    private var feeCallBack: ((Long, Long) -> Unit)? = null;

    fun setBalance(value: Long) {
        this.balance = value;
    }

    fun setFeeCallBack(callback: ((Long, Long) -> Unit)) {
        this.feeCallBack = callback
    }

    fun setPubKey(selectPubKeyModel: PubKeyModel) {
        this.selectPubKeyModel = selectPubKeyModel
    }

    suspend fun compose(
        inputUtxos: ArrayList<Utxo>,
        inputAddress: String,
        inputFee: Long,
        inputAmount: Double
    ): Boolean {
        if (selectPubKeyModel == null) {
            return false;
        }
        receivers = hashMapOf()
        selectedUtxos = arrayListOf()
        amount = inputAmount
        address = inputAddress
        selectedFee = inputFee
        var totalValueSelected = 0L
        var change: Long
        var fee: BigInteger? = BigInteger.ZERO

        val utxos: ArrayList<UTXO> = arrayListOf();
        for (utxoCoin in inputUtxos) {
            val u = UTXO()
            val outs: MutableList<MyTransactionOutPoint> = ArrayList()
            outs.add(utxoCoin.getOutPoints())
            u.outpoints = outs
            utxos.add(u)
        }
        // sort in ascending order by value
        Collections.sort(utxos, UTXO.UTXOComparator())
        utxos.reverse()
        receivers[address] = BigInteger.valueOf(amount.toLong())
        // get smallest 1 UTXO > than spend + fee + dust
        for (u in utxos) {
            val outpointTypes: Triple<Int, Int, Int> =
                FeeUtil.getInstance().getOutpointCount(Vector(u.outpoints))
            if (u.value >= amount + bDust.toLong() + FeeUtil.getInstance().estimatedFeeSegwit(
                    outpointTypes.left,
                    outpointTypes.middle,
                    outpointTypes.right,
                    2
                ).toLong()
            ) {
                selectedUtxos.add(u)
                totalValueSelected += u.value
                Timber.i("single output")
                Timber.i("amount:$amount")
                Timber.i("value selected:" + u.value)
                Timber.i("total value selected:$totalValueSelected")
                Timber.i("nb inputs:" + u.outpoints.size)
                break
            }
        }

        if (selectedUtxos.size == 0) {
            // sort in descending order by value
            Collections.sort(utxos, UTXO.UTXOComparator())
            var selected = 0
            var p2pkh = 0
            var p2sh_p2wpkh = 0
            var p2wpkh = 0

            // get largest UTXOs > than spend + fee + dust
            for (u in utxos) {
                selectedUtxos.add(u)
                totalValueSelected += u!!.value
                selected += u.outpoints.size

//                            Log.d("SendActivity", "value selected:" + u.getValue());
//                            Log.d("SendActivity", "total value selected/threshold:" + totalValueSelected + "/" + (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFee(selected, 2).longValue()));
                val outpointTypes: Triple<Int, Int, Int> =
                    FeeUtil.getInstance().getOutpointCount(Vector(u.outpoints))
                p2pkh += outpointTypes.left
                p2sh_p2wpkh += outpointTypes.middle
                p2wpkh += outpointTypes.right
                if (totalValueSelected >= amount + bDust.toLong() + FeeUtil.getInstance()
                        .estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, 2).toLong()
                ) {

                    break
                }
            }
        }

        change = (totalValueSelected - (amount + fee!!.toLong())).toLong()

        if (change > 0L && change < bDust.toLong()) {
            throw ComposeException("Change is dust")
        }
        
        val outpoints: MutableList<MyTransactionOutPoint?> = ArrayList()

        for (utxo in selectedUtxos) {
            balance += utxo.value
        }
//            List<UTXO> utxos = preselectedUTXOs;
        // sort in descending order by value
        for (utxo in selectedUtxos) {
            outpoints.addAll(utxo.outpoints)
        }
        val outpointTypes = FeeUtil.getInstance().getOutpointCount(Vector(outpoints))
        fee = FeeUtil.getInstance().estimatedFeeSegwit(
            outpointTypes.left,
            outpointTypes.middle,
            outpointTypes.right,
            2
        )
        if (amount.toLong() == balance) {
            fee = FeeUtil.getInstance().estimatedFeeSegwit(
                outpointTypes.left,
                outpointTypes.middle,
                outpointTypes.right,
                1
            )
            amount -= fee.toLong()
            receivers.clear()
            receivers[address] = BigInteger.valueOf(amount.toLong())

        }

        change = (totalValueSelected - (amount + fee.toLong())).toLong()
        //
        //                    Log.d("SendActivity", "change:" + change);

        if (change < 0L && change < bDust.toLong()) {
            throw ComposeException("Change is dust")
        }
        val changeAddress = getNextChangeAddress()
            ?: throw ComposeException("Change address is invalid");
        receivers[changeAddress] = BigInteger.valueOf(change)


        val outPoints: ArrayList<MyTransactionOutPoint> = ArrayList()


        for (u in selectedUtxos) {
            outPoints.addAll(u.outpoints)
        }


        val transaction = try {
            SendFactory.getInstance(context)
                .makeTransaction(0, outPoints, receivers)
        } catch (e: Exception) {
            throw   ComposeException("Unable to create tx")
        }

        psbt = PSBT(transaction)

        val networkParameters = SentinelState.getNetworkParam();
        val type = if (networkParameters is MainNetParams) 0 else 1

        val purpose = selectPubKeyModel?.getPurpose();

        try {
            //The chain parameter will be always 1 since its a change back output
            psbt?.addOutput(
                SentinelState.getNetworkParam(),
                Hex.decodeHex("0d8c85ab"), changeECKey, purpose!!, type, 0, 1, changeIndex!!
            )
            psbt?.addOutputSeparator()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        if (purpose != 0) {
            for (outPoint in outPoints) {
                inputUtxos.forEach {
                    if (it.txHash == outPoint.hash.toString() && outPoint.txOutputN == it.txOutputN) {
                        val path: String = it.path;
                        val addressIndex: Int = path.split("/".toRegex()).toTypedArray()[2].toInt()
                        val chainIndex = path.split("/".toRegex()).toTypedArray()[1].toInt()
                        val eckeyInput = if (chainIndex == 1) {
                            getAccount()?.change?.getAddressAt(addressIndex)?.ecKey
                        } else {
                            getAccount()?.receive?.getAddressAt(addressIndex)?.ecKey
                        }
                        psbt?.addInput(
                            networkParameters,
                            Hex.decodeHex("0d8c85ab"),
                            eckeyInput,
                            outPoint.value.value,
                            purpose!!,
                            addressIndex,
                            0,
                            chainIndex,
                            addressIndex
                        )
                    }
                }
            }
        }

        if (psbt != null && feeCallBack != null) {
            val transaction = psbt!!.transaction
            withContext(Dispatchers.Main) {
//                if (transaction.fee != null)
//                    feeCallBack?.invoke(transaction.fee.value, transaction.fee.value);
            }
        }

        return true
    }


    private fun getAccount(): HD_Account? {
        val pubKey = this.selectPubKeyModel;
        return when (this.selectPubKeyModel?.type) {
            AddressTypes.BIP44 -> {
                return HD_Account(SentinelState.getNetworkParam(), pubKey?.pubKey, "", 0);
            }
            AddressTypes.BIP49 -> {
                return HD_Account(SentinelState.getNetworkParam(), pubKey?.pubKey, "", 0)
            }
            AddressTypes.BIP84 -> {
                return HD_Account(SentinelState.getNetworkParam(), pubKey?.pubKey, "", 0)
            }
            AddressTypes.ADDRESS -> {
                return null;
            }
            else -> null
        }
    }

    private fun getNextChangeAddress(): String? {
        val pubKey = this.selectPubKeyModel;
        changeIndex = pubKey?.change_index!! + 1
        val account = getAccount()

        return when (this.selectPubKeyModel?.type) {
            AddressTypes.BIP44 -> {
                val address = account?.change?.getAddressAt(changeIndex!!)
                changeECKey = address?.ecKey
                address?.addressString
            }
            AddressTypes.BIP49 -> {
                val address = account?.change?.getAddressAt(changeIndex!!)
                changeECKey = address?.ecKey
                val p2shP2wpkH = P2SH_P2WPKH(changeECKey?.pubKey, SentinelState.getNetworkParam())
                p2shP2wpkH.addressAsString
            }
            AddressTypes.BIP84 -> {
                val address = account?.getChain(0)?.getAddressAt(changeIndex!!)
                val ecKey = address?.ecKey
                changeECKey = address?.ecKey
                val segwitAddress = SegwitAddress(ecKey?.pubKey, SentinelState.getNetworkParam())
                segwitAddress.bech32AsString
            }
            AddressTypes.ADDRESS -> {
                pubKey.pubKey
            }
            else -> ""
        }
    }

    fun getPSBT(): PSBT? {
        return psbt;
    }

}