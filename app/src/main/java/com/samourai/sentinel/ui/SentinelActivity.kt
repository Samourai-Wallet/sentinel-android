package com.samourai.sentinel.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.sentinel.R
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.core.access.AccessFactory
import com.samourai.sentinel.tor.TorServiceController
import com.samourai.sentinel.ui.fragments.AddNewPubKeyBottomSheet
import com.samourai.sentinel.ui.utils.PrefsUtil
import org.koin.java.KoinJavaComponent.inject

open class SentinelActivity : AppCompatActivity() {

    final val CAMERA_PERMISSION = 20
    private val accessFactory: AccessFactory by  inject(AccessFactory::class.java);
    private val prefsUtil: PrefsUtil by  inject(PrefsUtil::class.java);


    override fun onResume() {
        super.onResume()
        if(prefsUtil.pinHash!!.isNotBlank() && accessFactory.isTimedOut){
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        }
        if(prefsUtil.displaySecure!!){
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun askCameraPermission() {
        MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.permission_alert_dialog_title_camera))
                .setMessage(resources.getString(R.string.permission_dialog_message_camera))
                .setNegativeButton(resources.getString(R.string.no)) { _, _ ->
                    val bottomSheetFragment = AddNewPubKeyBottomSheet()
                    bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                }
                .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION)
                    }
                }
                .show()
    }

}