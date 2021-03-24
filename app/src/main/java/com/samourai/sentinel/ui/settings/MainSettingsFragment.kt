package com.samourai.sentinel.ui.settings;

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentTransaction
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.sentinel.R
import com.samourai.sentinel.core.access.AccessFactory
import com.samourai.sentinel.data.db.dao.TxDao
import com.samourai.sentinel.data.db.dao.UtxoDao
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.data.repository.ExchangeRateRepository
import com.samourai.sentinel.ui.SentinelActivity
import com.samourai.sentinel.ui.dojo.DojoUtility
import com.samourai.sentinel.ui.home.HomeActivity
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.ui.utils.showFloatingSnackBar
import com.samourai.sentinel.ui.views.LockScreenDialog
import com.samourai.sentinel.ui.views.alertWithInput
import com.samourai.sentinel.ui.views.confirm
import com.samourai.sentinel.util.*
import com.samourai.wallet.crypto.AESUtil
import com.samourai.wallet.util.CharSequenceX
import kotlinx.coroutines.*
import org.koin.java.KoinJavaComponent.inject
import java.io.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class MainSettingsFragment : PreferenceFragmentCompat() {

    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);
    private val txDao: TxDao by inject(TxDao::class.java);
    private val utxoDao: UtxoDao by inject(UtxoDao::class.java);
    private val accessFactory: AccessFactory by inject(AccessFactory::class.java);
    private val collectionRepository: CollectionRepository by inject(CollectionRepository::class.java);
    private val exchangeRateRepository: ExchangeRateRepository by inject(ExchangeRateRepository::class.java);
    private val dojoUtility: DojoUtility by inject(DojoUtility::class.java);
    private val settingsScope = CoroutineScope(context = Dispatchers.Main)
    private var exportedBackUp: String? = null
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setExchangeSettings()

        val pineEntryCheckBox = findPreference<CheckBoxPreference>("pinEnabled")
        pineEntryCheckBox?.let {
            it.setOnPreferenceChangeListener { _, newValue ->
                val lockScreenDialog = LockScreenDialog(lockScreenMessage = if (newValue == true) "Enter new pin code" else "Enter current pin code")
                val transaction = parentFragmentManager.beginTransaction()
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                transaction.add(android.R.id.content, lockScreenDialog).addToBackStack("Lock").commit()
                lockScreenDialog.setOnPinEntered { pin ->
                    if (newValue == true) {
                        setPinCode(pin, it)
                        lockScreenDialog.dismiss()
                        parentFragmentManager.popBackStack()
                    } else {
                        removePinCode(pin, lockScreenDialog, pineEntryCheckBox)
                    }
                }

                false
            }
        }
        val clearWallet = findPreference<Preference>("clear")

        val shareErrorLog = findPreference<Preference>("shareErrorLog")
        shareErrorLog?.setOnPreferenceClickListener {
            val file = File("${requireActivity().cacheDir}/error_dump.log")

            if (file.exists()) {
                val intent = Intent()
                intent.action = Intent.ACTION_SEND
                intent.type = "text/plain"
                if (Build.VERSION.SDK_INT >= 24) {
                    //From API 24 sending FIle on intent ,require custom file provider
                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            requireContext(),
                            requireContext()
                                    .packageName + ".provider", file))
                } else {
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                }
                requireActivity().startActivity(Intent.createChooser(intent, requireContext().getText(R.string.send_payment_code)))
            } else {
                Toast.makeText(requireContext(), "File does not exist", Toast.LENGTH_SHORT).show()
            }
            true
        }
        clearWallet?.setOnPreferenceClickListener {
            clearWallet()
            true
        }

        findPreference<Preference>("import")
                ?.setOnPreferenceClickListener {
                    startActivity(Intent(requireActivity(), ImportBackUpActivity::class.java))
                    true
                }
        findPreference<Preference>("export")
                ?.setOnPreferenceClickListener {
                    export()
                    true
                }
    }

    private fun setExchangeSettings() {

        fun setCurrencies() {
            prefsUtil.exchangeRate = -1L
            val entries: Array<CharSequence> = exchangeRateRepository.getCurrencies().toArray(arrayOfNulls<CharSequence>(exchangeRateRepository.getCurrencies().size))
            findPreference<ListPreference>("selectedCurrency")
                    ?.apply {
                        setEntries(entries)
                        entryValues = entries
                        value = prefsUtil.selectedCurrency
                        setOnPreferenceChangeListener { _, newValue ->
                            prefsUtil.selectedCurrency = newValue as String
                            exchangeRateRepository.fetch()
                            true
                        }
                    }

        }

        val entries: Array<CharSequence> = exchangeRateRepository.getExchanges().toArray(arrayOfNulls<CharSequence>(exchangeRateRepository.getExchanges().size))

        findPreference<ListPreference>("exchangeSelection")
                ?.apply {
                    setEntries(entries)
                    entryValues = entries
                    value = prefsUtil.exchangeSelection
                    setOnPreferenceChangeListener { preference, newValue ->
                        exchangeRateRepository.fetch()
                        prefsUtil.exchangeSelection = newValue as String
                        exchangeRateRepository.reloadChanges()
                        setCurrencies()
                        true
                    }
                }
        setCurrencies()

    }

    private fun clearWallet() {
        (activity as AppCompatActivity).confirm(label = "Confirm",
                message = "Are you sure want to clear all the collections ?",
                positiveText = "Yes",
                negativeText = "No",
                onConfirm = { confirmed ->
                    if (confirmed) {
                        settingsScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    utxoDao.delete()
                                    txDao.delete()
                                    collectionRepository.reset()
                                    dojoUtility.clearDojo()
                                    prefsUtil.clearAll()
                                }
                                withContext(Dispatchers.Main) {
                                    (activity as SettingsActivity).setRequireRestart(true)
                                    (activity as AppCompatActivity).showFloatingSnackBar(
                                            parent_view = requireView(),
                                            text = "Successfully cleared",
                                            actionClick = {
                                                startActivity(Intent(activity, HomeActivity::class.java).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                                })
                                                (activity as SettingsActivity).overridePendingTransition(R.anim.fade_in, R.anim.bottom_sheet_slide_out);
                                                (activity as SettingsActivity).finish()
                                            },
                                            actionText = "Restart"
                                    )
                                }
                            } catch (ex: Exception) {
                                withContext(Dispatchers.Main) {
                                    (activity as AppCompatActivity).showFloatingSnackBar(
                                            parent_view = requireView(),
                                            text = "Error : $ex"
                                    )
                                }
                            }
                        }
                    }
                }
        )
    }

    private fun removePinCode(pin: String, lockScreenDialog: LockScreenDialog, checkBox: CheckBoxPreference) {
        settingsScope.launch {
            withContext(Dispatchers.IO) {
                val pinHash = prefsUtil.pinHash
                pinHash?.let {
                    if (accessFactory.validateHash(pin, pinHash)) {
                        prefsUtil.pinHash = ""
                        prefsUtil.pinEnabled = false
                        accessFactory.pin = ""
                        accessFactory.reset()
                        dojoUtility.store()
                        collectionRepository.sync()
                        withContext(Dispatchers.Main) {
                            lockScreenDialog.dismiss()
                            checkBox.isChecked = false
                            parentFragmentManager.popBackStack()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            lockScreenDialog.showError()
                        }
                    }
                }
            }
        }

    }

    private fun setPinCode(pin: String, it: CheckBoxPreference) {

        settingsScope.launch {
            withContext(Dispatchers.IO) {
                val digest = MessageDigest.getInstance("SHA-256")
                val b = digest.digest(pin.toByteArray(charset("UTF-8")))
                val hash = Hash(b)
                prefsUtil.pinHash = hash.toString()
                accessFactory.setIsLoggedIn(true)
                accessFactory.pin = pin
                collectionRepository.sync()
            }
            withContext(Dispatchers.Main) {
                it.isChecked = true
                prefsUtil.pinEnabled = true
            }
        }
    }


    private fun export() {
        fun makeExportPayload(copyToClipBoard: Boolean) {
            settingsScope.launch(Dispatchers.IO) {
                val payload = ExportImportUtil().makePayload()
                withContext(Dispatchers.Main) {
                    (activity as SentinelActivity).alertWithInput(labelEditText = "Password",
                            buttonLabel = "Encrypt",
                            maskInput = true,
                            maxLen = 12,
                            label = "Add payload password", onConfirm = {
                        val payloadEncrypted = ExportImportUtil().addVersionInfo(AESUtil.encryptSHA256(payload.toString(), CharSequenceX(it), AESUtil.DefaultPBKDF2HMACSHA256Iterations))
                        exportedBackUp = payloadEncrypted.toString()
                        if (copyToClipBoard) {
                            val cm = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                            val clipData = ClipData
                                    .newPlainText("Sentinel BackUp", payloadEncrypted.toString())
                            if (cm != null) {
                                cm.setPrimaryClip(clipData)
                                Toast.makeText(context, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                            }
                            view?.let { it1 -> (activity as SentinelActivity).showFloatingSnackBar(it1, getString(R.string.copied_to_clipboard)) }
                        } else {
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                            intent.addCategory(Intent.CATEGORY_OPENABLE)
                            intent.type = "text/plain"
                            val current = Calendar.getInstance().time
                            val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                            intent.putExtra(Intent.EXTRA_TITLE, "sentinel_backup_${formatter.format(current)}.txt")
                            startActivityForResult(intent, REQ_CODE_WRITE_BACKUP)
                        }
                    })
                }
            }
        }

        val options = arrayListOf("Export and copy to clipboard", "Export to file")
        MaterialAlertDialogBuilder(requireContext())
            .setItems(options.toTypedArray()) { _, index ->
                makeExportPayload(index==0)
            }
            .setTitle(getString(R.string.choose_export_opt))
            .show()

    }

    private fun import() {

    }

    override fun onDestroy() {
        settingsScope.cancel()
        super.onDestroy()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_WRITE_BACKUP) {
            when (resultCode) {
                Activity.RESULT_OK -> if (data != null
                        && data.data != null) {
                    writeInFile(data.data!!)
                    (activity as AppCompatActivity).showFloatingSnackBar(
                            parent_view = requireView(),
                            text = "File written Successfully"
                    )
                }
                Activity.RESULT_CANCELED -> {
                }
            }
        }
    }

    private fun writeInFile(uri: Uri) {
        val outputStream: OutputStream
        try {
            outputStream = requireContext().contentResolver.openOutputStream(uri)!!
            val bw = BufferedWriter(OutputStreamWriter(outputStream))
            bw.write(exportedBackUp)
            bw.flush()
            bw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val REQ_CODE_WRITE_BACKUP = 12
    }
}
