package com.samourai.sentinel.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.transition.TransitionManager
import com.google.android.material.transition.MaterialSharedAxis
import com.samourai.sentinel.R
import com.samourai.sentinel.data.AddressTypes
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.ui.SentinelActivity
import com.samourai.sentinel.ui.collectionDetails.send.SendViewModel
import com.samourai.sentinel.ui.home.HomeActivity
import com.samourai.sentinel.ui.utils.AndroidUtil
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.ui.utils.logThreadInfo
import com.samourai.sentinel.ui.utils.showFloatingSnackBar
import com.samourai.sentinel.util.ExportImportUtil
import com.samourai.sentinel.util.FormatsUtil
import kotlinx.android.synthetic.main.activity_import_back_up.*
import kotlinx.android.synthetic.main.content_choose_address_type.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.koin.ext.scope
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader

class ImportBackUpActivity : SentinelActivity() {

    enum class ImportType {
        SAMOURAI,
        SENTINEL,
        SENTINEL_LEGACY
    }

    class ImportBackUpViewModel : ViewModel()

    private var payloadObject: JSONObject? = null
    private var importType = ImportType.SENTINEL
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);
    private var requireRestart = false
    private val viewModel: ImportBackUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_back_up)
        setSupportActionBar(toolbarImportActivity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        importChoosePayloadBtn.setOnClickListener {
            var intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent = Intent.createChooser(intent, "Choose a file")
            startActivityForResult(intent, REQUEST_FILE_CODE)
            importPayloadTextView.text = ""
        }

        importPastePayloadBtn.setOnClickListener {
            if (AndroidUtil.getClipBoardString(applicationContext) != null) {
                AndroidUtil.getClipBoardString(applicationContext)?.let {
                    importPayloadTextView.text = ""
                    validatePayload(it)
                }
            }
        }

        importStartBtn.isEnabled = false

        importStartBtn.setOnClickListener {
            if (importPasswordInput.text?.length == 0) {
                importPasswordInput.error = "Please type payload password"
            } else {
                decryptPayload()
            }
        }
        showImportButton(true)
    }


    private fun showImportButton(hide: Boolean){
        val sharedAxis = MaterialSharedAxis(MaterialSharedAxis.Y, !hide)
        TransitionManager.beginDelayedTransition(importStartBtn.rootView as ViewGroup, sharedAxis)
        importStartBtn.isEnabled  = !hide
        importStartBtn.visibility  = if(hide) View.GONE else View.VISIBLE
    }

    private fun decryptPayload() {
        when (importType) {
            ImportType.SENTINEL -> {
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val payload = ExportImportUtil().decryptSentinel(
                            payloadObject.toString(),
                            importPasswordInput.text.toString()
                        )
                        withContext(Dispatchers.Main) {
                            importPasswordInputLayout.visibility = View.INVISIBLE
                            importSentinelBackUpLayout.visibility = View.VISIBLE
                            importCollections.text =
                                "${importCollections.text} (${payload.first?.size})"
                        }
                        if (importCollections.isChecked) {
                            payload.first?.let {
                                ExportImportUtil().startImportCollections(
                                    it,
                                    importClearExisting.isChecked
                                )
                            }
                        }
                        if (importPrefs.isChecked) {
                            payload.second.let { ExportImportUtil().importPrefs(it) }
                        }
                        if (importDojo.isChecked) {
                            payload.third?.let { ExportImportUtil().importDojo(it) }
                            prefsUtil.apiEndPointTor = payload.second.getString("apiEndPointTor")
                            prefsUtil.apiEndPoint = payload.second.getString("apiEndPoint")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        throw CancellationException(e.message)
                    }
                }.invokeOnCompletion {
                    if (it == null) {
                        requireRestart = true
                        showFloatingSnackBar(
                            importPastePayloadBtn, "Successfully imported",
                            anchorView = importStartBtn.id,
                            actionText = "restart"
                        )
                    } else {
                        Timber.e(it)
                        showFloatingSnackBar(
                            importPastePayloadBtn,
                            "Error: ${it.message}",
                            anchorView = importStartBtn.id
                        )
                    }
                }
            }
            ImportType.SENTINEL_LEGACY -> {
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val payload = ExportImportUtil().decryptSentinelLegacy(
                            payloadObject.toString(),
                            importPasswordInput.text.toString()
                        )
                        val pubKeys = payload.first
                        if(pubKeys.isNotEmpty()){
                            val collection = PubKeyCollection()
                            collection.pubs = pubKeys
                            collection.collectionLabel = "Sentinel Import"
                            ExportImportUtil().startImportCollections(
                                arrayListOf(collection),
                                false
                            )
                        }else{
                            throw  CancellationException("0 public keys found")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        throw CancellationException(e.message)
                    }
                }
                    .invokeOnCompletion {
                    if (it == null) {
                        requireRestart = true
                        showFloatingSnackBar(
                            importPastePayloadBtn, "Successfully imported",
                            anchorView = importStartBtn.id,
                            actionText = "restart"
                        )
                    } else {
                        showFloatingSnackBar(
                            importPastePayloadBtn,
                            "Error: ${it.message}",
                            anchorView = importStartBtn.id
                        )
                    }
                }
            }
            else -> {
                val payload = ExportImportUtil().decryptAndParseSamouraiPayload(
                    payloadObject.toString(),
                    importPasswordInput.text.toString()
                )
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    ExportImportUtil().startImportCollections(arrayListOf(payload), false)
                }.invokeOnCompletion {
                    if (it == null) {
                        requireRestart = true
                        showFloatingSnackBar(
                            importPastePayloadBtn,
                            "Successfully imported",
                            actionClick = { restart() },
                            actionText = "restart"
                        )
                    } else {
                        showFloatingSnackBar(importPastePayloadBtn, "Error: ${it.message}")
                    }
                }
            }
        }
    }

    private fun restart() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        })
        overridePendingTransition(R.anim.fade_in, R.anim.bottom_sheet_slide_out);
        finish()
    }

    /**
     * Validates backup payloads
     * Method uses coroutines to parse json
     */
    private fun validatePayload(string: String) {
        viewModel.viewModelScope.launch(Dispatchers.Default) {
            try {
                val json = JSONObject(string)
                withContext(Dispatchers.Main) {
                    importPayloadTextView.text = "${importPayloadTextView.text}${json.toString(2)}"
                    if (json.has("external") && json.has("payload")) {
                        payloadObject = json
                        importType = ImportType.SAMOURAI
                        showImportButton(false)
                    } else if (json.has("time") && json.has("payload")) {
                        payloadObject = json
                        importType = ImportType.SENTINEL
                        showImportButton(false)
                        importSentinelBackUpLayout.visibility = View.VISIBLE
                    } else if (json.has("payload")) {
                        payloadObject = json
                        importType = ImportType.SENTINEL_LEGACY
                        showImportButton(false)
                    } else {
                        showImportButton(false)
                        showFloatingSnackBar(importStartBtn, text = "Invalid payload")
                    }
                }
            } catch (e: Exception) {
                throw  CancellationException((e.message))
            }
        }.invokeOnCompletion {
            if (it != null) {
                Timber.e(it)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && data.data != null && data.data!!.path != null && requestCode == REQUEST_FILE_CODE) {
            val job =
                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    try {
                        val inputStream = contentResolver.openInputStream(data.data!!);
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val size = inputStream?.available()
                        if (size != null) {
                            if (size > 5e+6) {
                                throw  IOException("File size is too large to open")
                            }
                        }
                        var string = ""
                        string = reader.buffered().readText();
                        withContext(Dispatchers.Main) {
                            validatePayload(string)
                        }
                    } catch (fn: FileNotFoundException) {
                        fn.printStackTrace()
                        throw CancellationException((fn.message))
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                        throw CancellationException((ioe.message))
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        throw CancellationException((ex.message))
                    }
                }
            job.invokeOnCompletion {
                if (it != null) {
                    this.showFloatingSnackBar(importPastePayloadBtn, "Error ${it.message}")
                }
            }
        }
    }

    override fun onBackPressed() {
        if (requireRestart) {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
            overridePendingTransition(R.anim.fade_in, R.anim.bottom_sheet_slide_out);
            finish()
        } else
            super.onBackPressed()
    }

    companion object {
        const val REQUEST_FILE_CODE = 44
    }

}