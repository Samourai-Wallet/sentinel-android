package com.samourai.sentinel.ui.dojo

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.samourai.sentinel.R
import com.samourai.sentinel.SamouraiSentinel
import com.samourai.sentinel.service.WebSocketService
import com.samourai.sentinel.tor.TorManager
import com.samourai.sentinel.tor.TorManager.CONNECTION_STATES
import com.samourai.sentinel.tor.TorService
import com.samourai.sentinel.ui.dojo.DojoConfigureBottomSheet.DojoConfigurationListener
import com.samourai.sentinel.util.AppUtil
import com.samourai.sentinel.util.ConnectivityStatus
import com.samourai.sentinel.util.PrefsUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONException
import java.io.IOException

class NetworkActivity : AppCompatActivity() {
    var torRenewBtn: TextView? = null
    var torConnectionStatus: TextView? = null
    var dojoConnectionStatus: TextView? = null
    var torButton: Button? = null
    var dojoButton: Button? = null
    var torConnectionIcon: ImageView? = null
    var dojoConnectionIcon: ImageView? = null
    var activeColor = 0
    var disabledColor = 0
    var waiting = 0
    var disposables = CompositeDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)
        setSupportActionBar(findViewById(R.id.toolbarCollectionDetails))
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        activeColor = ContextCompat.getColor(this, R.color.green_ui_2)
        disabledColor = ContextCompat.getColor(this, R.color.disabledRed)
        waiting = ContextCompat.getColor(this, R.color.warning_yellow)
        torButton = findViewById(R.id.networking_tor_btn)
        torRenewBtn = findViewById(R.id.networking_tor_renew)
        dojoButton = findViewById(R.id.networking_dojo_btn)
        torConnectionIcon = findViewById(R.id.network_tor_status_icon)
        torConnectionStatus = findViewById(R.id.network_tor_status)
        dojoConnectionIcon = findViewById(R.id.network_dojo_status_icon)
        dojoConnectionStatus = findViewById(R.id.network_dojo_status)
        torRenewBtn!!.setOnClickListener(View.OnClickListener {
            if (TorManager.getInstance(applicationContext).isConnected) {
                startService(Intent(this, TorService::class.java).setAction(TorService.RENEW_IDENTITY))
            }
        })
        dojoButton!!.setOnClickListener(View.OnClickListener { view: View? ->
            if (DojoUtil.getInstance(application).isDojoEnabled) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Confirm")
                builder.setMessage("Are you sure want to disable dojo ?")
                builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                    DojoUtil.getInstance(application).removeDojoParams()
                    try {
                        SamouraiSentinel.getInstance(application).serialize(SamouraiSentinel.getInstance(application).toJSON(), null)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    setDojoState()
                }
                builder.setNegativeButton("Cancel") { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                builder.show()
            }
            val dojoConfigureBottomSheet = DojoConfigureBottomSheet()
            dojoConfigureBottomSheet.show(supportFragmentManager, dojoConfigureBottomSheet.tag)
            dojoConfigureBottomSheet.setDojoConfigurationListener(object : DojoConfigurationListener {
                override fun onConnect() {
                    setDojoState()
                }

                override fun onError() {
                    Toast.makeText(applicationContext, "Error while connecting dojo", Toast.LENGTH_LONG).show()
                }
            })
        })
        listenToTorStatus()
        //
        torButton!!.setOnClickListener {
            if (TorManager.getInstance(applicationContext).isRequired) {
//                if(DojoUtil.getInstance(NetworkDashboard.this).getDojoParams() !=null ){
//                    Toast.makeText(this,R.string.cannot_disable_tor_dojo,Toast.LENGTH_LONG).show();
//                    return;
//                }
                stopTor()
                PrefsUtil.getInstance(applicationContext).setValue(PrefsUtil.ENABLE_TOR, false)
            } else {
                if (AppUtil.getInstance(this.applicationContext).isServiceRunning(WebSocketService::class.java)) {
                    stopService(Intent(this.applicationContext, WebSocketService::class.java))
                }
                startTor()
                PrefsUtil.getInstance(applicationContext).setValue(PrefsUtil.ENABLE_TOR, true)
            }
        }
        setDojoState()
    }

    private fun stopTor() {
        val startIntent = Intent(applicationContext, TorService::class.java)
        startIntent.action = TorService.STOP_SERVICE
        startService(startIntent)
    }

    private fun setDojoState() {
        if (DojoUtil.getInstance(application).isDojoEnabled) {
            dojoConnectionIcon!!.setColorFilter(activeColor)
            dojoConnectionStatus!!.text = getString(R.string.Enabled)
            dojoButton!!.setText(R.string.disable)
        } else {
            dojoConnectionIcon!!.setColorFilter(disabledColor)
            dojoConnectionStatus!!.text = getString(R.string.disabled)
            dojoButton!!.setText(R.string.enable)
        }
    }

    private fun listenToTorStatus() {
        val disposable = TorManager.getInstance(applicationContext).torStatus
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { enabled: CONNECTION_STATES -> setTorConnectionState(enabled) }
        disposables.add(disposable)
        runOnUiThread { setTorConnectionState(if (TorManager.getInstance(applicationContext).isConnected) CONNECTION_STATES.CONNECTED else CONNECTION_STATES.DISCONNECTED) }
    }

    private fun setTorConnectionState(enabled: CONNECTION_STATES) {
        runOnUiThread {
            when (enabled) {
                CONNECTION_STATES.CONNECTED -> {
                    torButton!!.text = getString(R.string.disable)
                    torButton!!.isEnabled = true
                    torConnectionIcon!!.setColorFilter(activeColor)
                    torConnectionStatus!!.text = getString(R.string.Enabled)
                    torRenewBtn!!.visibility = View.VISIBLE
                    //                if(waitingForPairing)    {
    //                    waitingForPairing = false;
    //
    //                    if (strPairingParams != null) {
    //                        DojoUtil.getInstance(NetworkDashboard.this).setDojoParams(strPairingParams);
    //                        Toast.makeText(NetworkDashboard.this, "Tor enabled for Dojo pairing:" + DojoUtil.getInstance(this).getDojoParams(), Toast.LENGTH_SHORT).show();
    //                        initDojo();
    //                    }
    //
    //                }
                }
                CONNECTION_STATES.CONNECTING -> {
                    torRenewBtn!!.visibility = View.INVISIBLE
                    torButton!!.text = getString(R.string.loading)
                    torButton!!.isEnabled = false
                    torConnectionIcon!!.setColorFilter(waiting)
                    torConnectionStatus!!.text = getString(R.string.tor_initializing)
                }
                else -> {
                    torRenewBtn!!.visibility = View.INVISIBLE
                    torButton!!.text = getString(R.string.enable)
                    torButton!!.isEnabled = true
                    torConnectionIcon!!.setColorFilter(disabledColor)
                    torConnectionStatus!!.text = getString(R.string.disabled)
                }
            }
        }
    }

    private fun startTor() {
        if (ConnectivityStatus.hasConnectivity(applicationContext)) {
            val startIntent = Intent(applicationContext, TorService::class.java)
            startIntent.action = TorService.START_SERVICE
            startService(startIntent)
        } else {
            Toast.makeText(applicationContext, R.string.in_offline_mode, Toast.LENGTH_SHORT)
                    .show()
        }
    }

    override fun onDestroy() {
        disposables.dispose()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}