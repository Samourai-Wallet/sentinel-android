package com.samourai.sentinel.ui.utils

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.samourai.sentinel.BuildConfig
import timber.log.Timber


/**
 * Android specific utility methods
 */
class AndroidUtil {

    companion object {
        fun hideKeyboard(activity: Activity) {
            val imm: InputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            //Find the currently focused view, so we can grab the correct window token from it.
            var view: View? = activity.currentFocus
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = View(activity)
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        fun isPermissionGranted(permission: String, context: Context): Boolean =
                ContextCompat.checkSelfPermission(
                        context,
                        permission
                ) == PackageManager.PERMISSION_GRANTED

        fun getClipBoardString(context: Context): String? {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            return if(clipboard.hasPrimaryClip()){
                val item = clipboard.primaryClip?.getItemAt(0)
                item?.text.toString()
            }else{
                null
            }
        }

    }

}

fun logThreadInfo(location: String) {
    if (BuildConfig.DEBUG)
        Timber.i("Thread :$location: ${Thread.currentThread().name} ${Thread.currentThread().id}");
}