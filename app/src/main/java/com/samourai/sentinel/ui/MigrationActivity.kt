package com.samourai.sentinel.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.CycleInterpolator
import android.widget.Toast
import com.samourai.sentinel.R
import com.samourai.sentinel.data.AddressTypes
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.ui.dojo.DojoConfigureBottomSheet
import com.samourai.sentinel.ui.dojo.DojoUtility
import com.samourai.sentinel.ui.home.HomeActivity
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.ui.utils.showFloatingSnackBar
import com.samourai.sentinel.ui.views.confirm
import com.samourai.sentinel.util.FormatsUtil
import kotlinx.android.synthetic.main.activity_migration.*
import kotlinx.android.synthetic.main.fragment_bottomsheet_view_pager.*
import kotlinx.android.synthetic.main.keypad_view.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.inject
import java.io.File
import java.lang.Exception

class MigrationActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val publicKeys: ArrayList<PubKeyModel> = arrayListOf()
    private var dojoPayload: String? = null
    private val collectionRepo: CollectionRepository by inject(CollectionRepository::class.java);
    private val dojoUtil: DojoUtility by inject(DojoUtility::class.java);

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_migration)

        importBtn.setOnClickListener {
            startMigrating()
        }
        startAsFresh.setOnClickListener {
            confirm(label = "Confirm", message = "This will reset current sentinel database and starts with fresh one", positiveText = "Yes", negativeText = "No") {
                if (it) {
                    try {
                        val dir: File = applicationContext.getDir("wallet", Context.MODE_PRIVATE)
                        val file = File(dir, "sentinel.dat")
                        file.delete()
                        setResult(RESULT_CANCELED)
                        finish()
                    } catch (e: Exception) {
                        showFloatingSnackBar(startAsFresh, "Error ${e.message}")
                    }
                }
            }
        }
        readOldData()
    }

    private fun startMigrating() {
        importBtn.isEnabled = false
        startAsFresh.isEnabled = false
        progressBar.isIndeterminate = true
        progressBar.visibility = View.VISIBLE

        if (dojoPayload != null) {
            showDojoDialog()
        } else {
            migratePubKeys()
        }

    }

    private fun showDojoDialog() {
        val dojoConfigureBottomSheet = DojoConfigureBottomSheet()
        dojoConfigureBottomSheet.show(supportFragmentManager, dojoConfigureBottomSheet.tag)
        dojoConfigureBottomSheet.isCancelable = false
        dojoConfigureBottomSheet.setPayload(dojoPayload)
        dojoConfigureBottomSheet.setDojoConfigurationListener(object : DojoConfigureBottomSheet.DojoConfigurationListener {
            override fun onDismiss() {
                if (dojoUtil.isDojoEnabled()) {
                    migratePubKeys()
                }
            }
        })
    }

    private fun migratePubKeys() {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                val pubKeyCollection = PubKeyCollection(
                        collectionLabel = "Sentinel",
                        pubs = publicKeys,
                        balance = 0,
                )
                collectionRepo.addNew(pubKeyCollection)
                val dir: File = applicationContext.getDir("wallet", Context.MODE_PRIVATE)
                val file = File(dir, "sentinel.dat")
                file.delete()
            }
            progressBar.isIndeterminate = false
            showFloatingSnackBar(importBtn, text = "Migrated ${publicKeys.size} public keys", actionText = "Ok", actionClick = {
                setResult(RESULT_OK)
                finish()
            })
        }
    }

    private fun readOldData() {

        restoreImg.animate()
                .rotation(-360f)
                .setDuration(1100)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()


        val dir: File = applicationContext.getDir("wallet", Context.MODE_PRIVATE)
        val file = File(dir, "sentinel.dat")

        scope.launch {
            try {
                val payload = file.readText()
                val json = JSONObject(payload)

                if (json.has("xpubs")) {
                    val xpubs = json.getJSONArray("xpubs")
                    repeat(xpubs.length()) {
                        val xpubObject = xpubs.getJSONObject(it)
                        xpubObject.keys().forEach { key ->
                            val type = validate(key);
                            if (type == AddressTypes.BIP44) {
                                val pubKeyModel = PubKeyModel(pubKey = key,
                                        label = xpubObject.getString(key), type = AddressTypes.BIP44)
                                publicKeys.add(pubKeyModel)
                            }
                        }
                    }
                }

                if (json.has("bip49")) {
                    val xpubs = json.getJSONArray("bip49")
                    repeat(xpubs.length()) {
                        val xpubObject = xpubs.getJSONObject(it)
                        xpubObject.keys().forEach { key ->
                            val type = validate(key);
                            if (type == AddressTypes.BIP49) {
                                val pubKeyModel = PubKeyModel(pubKey = key,
                                        label = xpubObject.getString(key), type = AddressTypes.BIP49)
                                publicKeys.add(pubKeyModel)
                            }
                        }
                    }
                }

                if (json.has("bip84")) {
                    val xpubs = json.getJSONArray("bip84")
                    repeat(xpubs.length()) {
                        val xpubObject = xpubs.getJSONObject(it)
                        xpubObject.keys().forEach { key ->
                            val type = validate(key);
                            if (type == AddressTypes.BIP84) {
                                val pubKeyModel = PubKeyModel(pubKey = key,
                                        label = xpubObject.getString(key), type = AddressTypes.BIP84)
                                publicKeys.add(pubKeyModel)
                            }
                        }
                    }
                }

                if (json.has("legacy")) {
                    val xpubs = json.getJSONArray("legacy")
                    repeat(xpubs.length()) {
                        val xpubObject = xpubs.getJSONObject(it)
                        xpubObject.keys().forEach { key ->
                            val type = validate(key);
                            if (type == AddressTypes.ADDRESS) {
                                val pubKeyModel = PubKeyModel(pubKey = key,
                                        label = xpubObject.getString(key), type = AddressTypes.ADDRESS)
                                publicKeys.add(pubKeyModel)
                            }
                        }
                    }
                }

                if (json.has("dojo")) {
                    if (dojoUtil.validate(json.getString("dojo"))) {
                        dojoPayload = JSONObject(json.getString("dojo")).toString()
                    }
                }

            } catch (ex: Exception) {

                withContext(Dispatchers.Main) {
                    showFloatingSnackBar(startAsFresh, "Error: ${ex.message}")
                }
            }
        }

    }


    private fun validate(code: String): AddressTypes? {

        var payload = code

        if (code.startsWith("BITCOIN:")) {
            payload = code.substring(8)

        }
        if (code.startsWith("bitcoin:")) {
            payload = code.substring(8)
        }
        if (code.startsWith("bitcointestnet:")) {
            payload = code.substring(15)
        }
        if (code.contains("?")) {
            payload = code.substring(0, code.indexOf("?"))
        }
        if (code.contains("?")) {
            payload = code.substring(0, code.indexOf("?"))
        }

        var type = AddressTypes.ADDRESS

        if (code.startsWith("xpub") || code.startsWith("tpub")) {
            type = AddressTypes.BIP44
        } else if (code.startsWith("ypub") || code.startsWith("upub")) {
            type = AddressTypes.BIP49
        } else if (code.startsWith("zpub") || code.startsWith("vpub")) {
            type = AddressTypes.BIP84
        }

        return if (type == AddressTypes.ADDRESS) {
            FormatsUtil.isValidBitcoinAddress(code)
            null
        } else {
            if (FormatsUtil.isValidXpub(code)) {
                type
            } else {
                null
            }
        }
    }
}