package com.samourai.sentinel.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.sentinel.R
import com.samourai.sentinel.core.access.AccessFactory
import com.samourai.sentinel.ui.dojo.DojoConfigureBottomSheet
import com.samourai.sentinel.ui.dojo.DojoUtility
import com.samourai.sentinel.ui.home.HomeActivity
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.ui.views.LockScreenDialog
import kotlinx.coroutines.*
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.io.File

/**
 * sentinel-android
 *
 */

class MainActivity : AppCompatActivity() {

    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);
    private val dojoUtil: DojoUtility by inject(DojoUtility::class.java);
    private val accessFactory: AccessFactory by inject(AccessFactory::class.java);

    private var dontDeleteFile = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pinHash = prefsUtil.pinHash
        if (!pinHash.isNullOrEmpty()) {
            val fragmentManager = supportFragmentManager
            val newFragment = LockScreenDialog(cancelable = false, lockScreenMessage = "Enter pin code")
            val transaction = fragmentManager.beginTransaction()
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            transaction.add(android.R.id.content, newFragment).commit()
            newFragment.setOnPinEntered {
                if (AccessFactory.getInstance(this).validateHash(it, pinHash)) {
                    accessFactory.pin = it
                    //this will re-read config files and update instance
                    runBlocking {
                        delay(100)
                        dojoUtil.read()
                        navigate()
                    }

                } else {
                    newFragment.showError()
                }
            }
        } else {
             navigate()
        }
    }

    private fun navigate() {

        if (!checkMigration()) {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
            this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish()
        } else {
            startActivityForResult(Intent(this, MigrationActivity::class.java), 20)
            this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }


    private fun checkMigration(): Boolean {
        if(dontDeleteFile){
            return false
        }
        val dir: File = applicationContext.getDir("wallet", Context.MODE_PRIVATE)
        val file = File(dir, "sentinel.dat")
        Timber.i(file.exists().toString());
        return file.exists()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 20) {
            dontDeleteFile = true;
            navigate()
        }
    }

    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.confirm_exit))
                .setMessage(resources.getString(R.string.ask_you_sure_exit))
                .setNegativeButton(resources.getString(R.string.no)) { _, _ ->
                }
                .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                    super.onBackPressed()
                }
                .show()
    }
}