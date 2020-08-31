package com.samourai.sentinel.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.marginTop
import androidx.fragment.app.FragmentTransaction
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionSet
import com.samourai.sentinel.R
import com.samourai.sentinel.core.access.AccessFactory
import com.samourai.sentinel.data.db.DbHandler
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.data.repository.TransactionsRepository
import com.samourai.sentinel.ui.SentinelActivity
import com.samourai.sentinel.ui.dojo.DojoUtility
import com.samourai.sentinel.ui.home.HomeActivity
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.ui.utils.showFloatingSnackBar
import com.samourai.sentinel.ui.views.LockScreenDialog
import com.samourai.sentinel.ui.views.confirm
import com.samourai.sentinel.util.Hash
import com.samourai.sentinel.util.apiScope
import com.samourai.sentinel.util.dataBaseScope
import kotlinx.android.synthetic.main.settings_activity.*
import kotlinx.coroutines.*
import org.koin.java.KoinJavaComponent.inject
import java.io.File
import java.security.MessageDigest


class SettingsActivity : SentinelActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {


    private var requireRestart: Boolean = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setSupportActionBar(toolbarSettings)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, MainSettingsFragment())
                    .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
    ): Boolean {
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                pref.fragment
        ).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }
        val transitionEnter = TransitionSet()
        val slide = Slide(Gravity.RIGHT)
        transitionEnter.addTransition(slide)
        transitionEnter.addTransition(Fade())
        val transitionExit = TransitionSet()
        transitionExit.addTransition(Slide(Gravity.LEFT))
        transitionExit.addTransition(Fade())
        fragment.enterTransition = transitionEnter
        fragment.exitTransition = transitionExit
        supportFragmentManager.beginTransaction()
                .replace(R.id.settings, fragment)
                .commit()
        title = pref.title
        return true
    }

    class MainSettingsFragment : PreferenceFragmentCompat() {

        private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);
        private val dbHandler: DbHandler by inject(DbHandler::class.java);
        private val accessFactory: AccessFactory by inject(AccessFactory::class.java);
        private val collectionRepository: CollectionRepository by inject(CollectionRepository::class.java);
        private val dojoUtility: DojoUtility by inject(DojoUtility::class.java);
        private val settingsScope = CoroutineScope(context = Dispatchers.Main)

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.main_preferences, rootKey)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

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
        }

        private fun clearWallet() {
            (activity as AppCompatActivity).confirm(label = "Confirm",
                    message = "Are you sure want to remove this public key ?",
                    positiveText = "Yes",
                    negativeText = "No",
                    onConfirm = { confirmed ->
                        if (confirmed) {
                            settingsScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        dbHandler.getUTXOsStore().destroy()
                                        dbHandler.getTxStore().destroy()
                                        collectionRepository.reset()
                                        dojoUtility.clearDojo()
                                        prefsUtil.clearAll()
                                    }
                                    withContext(Dispatchers.Main) {
                                        (activity as SettingsActivity).requireRestart = true
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
                                        apiScope.cancel("UserCancel")
                                        dataBaseScope.cancel("UserCancel")
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

        override fun onDestroy() {
            settingsScope.cancel()
            super.onDestroy()
        }

    }
}