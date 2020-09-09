package com.samourai.sentinel.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.samourai.sentinel.R
import com.samourai.sentinel.api.ApiService
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.ui.views.GenericBottomSheet
import com.samourai.sentinel.ui.webview.ExplorerWebViewActivity
import com.samourai.sentinel.util.apiScope
import kotlinx.android.synthetic.main.content_transactions_details.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.inject
import java.lang.Exception

/**
 * sentinel-android
 *
 * @author Sarath
 *
 */


class TransactionsDetailsBottomSheet(private var tx: Tx) : GenericBottomSheet() {

    data class TxFeeData(val fee: Long?, val feeRate: Long?)

    private val apiService: ApiService by inject(ApiService::class.java);
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);
    var job: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_transactions_details, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        txDetailsOpenInExplorerBtn.setOnClickListener {
            SentinelState.selectedTx = tx
            startActivity(Intent(requireContext(), ExplorerWebViewActivity::class.java))
        }
        setTx(tx)
        fetchFee()
    }

    private fun setTx(tx: Tx) {
        txDetailsAmount.text = tx.result.toString()
        txDetailsBlockId.text = "${tx.block_height}"
        txDetailsConfirmation.text = tx.confirmations.toString()
        txDetailsIOsize.text = "${tx.inputs.size}/${tx.out.size}"
        txDetailsTime.text = tx.time.toString()
        txDetailsHash.text = tx.hash
    }

    private fun setFeeDetails(txFeeData: TxFeeData) {
        txDetailsFeesProgress.visibility = View.GONE
        txDetailsFeesRateProgress.visibility = View.GONE
        txDetailsFees.text = txFeeData.fee.toString()
        txDetailsFeeRate.text = txFeeData.feeRate.toString()
    }

    private fun fetchFee() {
        job = apiScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getTx(tx.hash)
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val jsonObject = JSONObject(body)
                        var fee = 0L
                        var feeRate = 0L
                        if (jsonObject.has("fees")) {
                            fee = jsonObject.getLong("fees")
                        }
                        if (jsonObject.has("feerate")) {
                            feeRate = jsonObject.getLong("feerate")
                        }
                        val txFeeData = TxFeeData(fee, feeRate)
                        withContext(Dispatchers.Main) {
                            setFeeDetails(txFeeData)
                        }
                    }
                }
            } catch (ex: Exception) {
                throw CancellationException(ex.message)
            }
        }
        job?.invokeOnCompletion {
            if (it != null)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "Error: ${it?.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroy() {
        job?.let {
            if (it.isActive) it.cancel()
        }
        super.onDestroy()
    }


}