package com.samourai.sentinel.ui.broadcast

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import com.samourai.sentinel.R
import com.samourai.sentinel.api.ApiService
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.ui.utils.AndroidUtil
import com.samourai.sentinel.ui.views.GenericBottomSheet
import com.samourai.sentinel.ui.views.codeScanner.CameraFragmentBottomSheet
import kotlinx.android.synthetic.main.layout_broadcast_bottom_sheet.*
import kotlinx.coroutines.*
import org.bitcoinj.core.Transaction
import org.bouncycastle.util.encoders.Hex
import org.koin.java.KoinJavaComponent.inject


class BroadcastTx : GenericBottomSheet() {


    class BroadcastVm : ViewModel() {

        private val _hex = MutableLiveData("")
        val hex: LiveData<String> get() = _hex
        private val apiService: ApiService by inject(ApiService::class.java);
        fun broadCast(): Job {
            return viewModelScope.launch(Dispatchers.IO) {
                hex.value?.let {
                    try {
                        apiService.broadcast(it)
                    } catch (e: Exception) {
                        throw CancellationException(e.message)
                    }
                }
            };
        }

        fun setHex(hex: String) {
            _hex.postValue(hex)
        }
    }

    private var onBroadcastSuccess: ((hash: String) -> Unit)? = null

    private val model: BroadcastVm by viewModels()
    private var hash = "";

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_broadcast_bottom_sheet, container);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        disableBtn(broadCastTransactionBtn, false)

        pasteHex.setOnClickListener { _ ->
            val string = AndroidUtil.getClipBoardString(requireContext())
            string?.let {
                model.viewModelScope.launch(Dispatchers.Default) {
                    validate(it)
                }
            }
        }

        hexTextView.movementMethod = ScrollingMovementMethod()

        model.hex.observe(viewLifecycleOwner, Observer {
            hexTextView.text = it
        })

        pasteHex.setOnClickListener {
            val clipboardData = AndroidUtil.getClipBoardString(requireContext());
            clipboardData?.takeIf { it.isNotEmpty() }?.let { string ->
                model.viewModelScope.launch(Dispatchers.Default) {
                    validate(string)
                }
            }

        }
        scanHex.setOnClickListener {
            val camera = CameraFragmentBottomSheet()
            camera.show(requireActivity().supportFragmentManager, camera.tag)
            camera.setQrCodeScanLisenter {
                model.viewModelScope.launch(Dispatchers.Default) {
                    validate(it)
                }
            }

        }

        broadCastTransactionBtn.setOnClickListener {
            showLoading(true)
            model.broadCast().invokeOnCompletion {
                model.viewModelScope.launch(Dispatchers.Main) {
                    showLoading(false)
                    if (it == null) {
                        onBroadcastSuccess?.invoke(hash)
                        dismiss()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Unable to broadcast ${it.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    }

    private suspend fun validate(hex: String) {
        try {
            val transaction = Transaction(SentinelState.getNetworkParam(), Hex.decode(hex))
            hash = transaction.hashAsString;
            withContext(Dispatchers.Main) {
                disableAllButtons(true)
                model.setHex(hex)
            }
        } catch (ex: Exception) {
            withContext(Dispatchers.Main) {
                disableAllButtons(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        broadcastProgress.visibility = if (show) View.VISIBLE else View.GONE
        broadCastTransactionBtn?.text = if (show) " " else getString(R.string.broadcast_transaction)
        disableAllButtons(!show)
    }

    fun setOnBroadcastSuccess(listener: (hash: String) -> Unit) {
        this.onBroadcastSuccess = listener;
    }

    private fun disableAllButtons(enable: Boolean) {
        disableBtn(hexTextView, enable)
        disableBtn(scanHex, enable)
        disableBtn(pasteHex, enable)
        disableBtn(broadCastTransactionBtn, enable)
    }

    private fun disableBtn(button: View, enable: Boolean) {
        button.isEnabled = enable
        button.alpha = if (enable) 1F else 0.5f
    }

}