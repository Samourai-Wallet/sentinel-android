package com.samourai.sentinel.ui

import android.Manifest
import android.content.Intent
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.sentinel.R
import com.samourai.sentinel.core.access.AccessFactory
import com.samourai.sentinel.ui.fragments.AddNewPubKeyBottomSheet
import com.samourai.sentinel.ui.utils.PrefsUtil
import org.koin.java.KoinJavaComponent.inject
import kotlin.math.roundToInt

open class SentinelActivity : AppCompatActivity() {

    final val CAMERA_PERMISSION = 20
    private val accessFactory: AccessFactory by inject(AccessFactory::class.java);
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);
    private val displayMetrics: DisplayMetrics by lazy { resources.displayMetrics }


    override fun onResume() {
        super.onResume()
        if (prefsUtil.pinHash!!.isNotBlank() && accessFactory.isTimedOut) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        }
        if (prefsUtil.displaySecure!!) {
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


    //Gets NavBar Height
    public fun getNavHeight(): Float {
        val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimension(resourceId)
        } else 0F
    }

    //Gets StatusBar Height
    public fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }


    val screenRectPx: Rect
        get() = displayMetrics.run { Rect(0, 0, widthPixels, heightPixels) }

    val screenRectDp: RectF
        get() = displayMetrics.run { RectF(0f, 0f, widthPixels.px2dp, heightPixels.px2dp) }

    val Number.px2dp: Float
        get() = this.toFloat() / displayMetrics.density

    val Number.dp2px: Int
        get() = (this.toFloat() * displayMetrics.density).roundToInt()

}