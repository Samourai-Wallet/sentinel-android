package com.samourai.sentinel.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentTransaction
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionSet
import com.samourai.sentinel.R
import com.samourai.sentinel.core.access.AccessFactory
import com.samourai.sentinel.data.db.DbHandler
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.data.repository.ExchangeRateRepository
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
            showFragment(MainSettingsFragment())
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun showFragment(fragment:PreferenceFragmentCompat){
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, fragment)
                .commit()
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


    fun setRequireRestart(enable:Boolean){
        requireRestart = enable
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

}