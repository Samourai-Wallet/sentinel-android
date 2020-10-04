package com.samourai.sentinel.ui.webview

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.webkit.*
import com.samourai.sentinel.R
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.core.SentinelState.TorState.*
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.ui.utils.showFloatingSnackBar
import com.samourai.sentinel.ui.views.confirm
import io.matthewnelson.topl_service.TorServiceController
import kotlinx.android.synthetic.main.activity_explorer_web_view.*
import timber.log.Timber


class ExplorerWebViewActivity : AppCompatActivity() {

    lateinit var client: WebViewClient
    var tx: Tx? = null
    var url = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explorer_web_view)

        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (SentinelState.selectedTx != null) {
            tx = SentinelState.selectedTx!!
            title = tx!!.hash
        } else {
            finish()
        }

        webView.setBackgroundColor(0)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(applicationContext) { }
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
        }
        //Check webkit supports proxy
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            if (SentinelState.isTorStarted()) {
                setProxy()
            } else {
                this.confirm(label = "Confirm",
                        isCancelable = false,
                        message = "Tor is not enabled, built in web browser supports tor proxy",
                        negativeText = "Continue without tor", positiveText = "Turn on tor and load") {
                    if (!it) {
                        load()
                    } else {
                        torStartAndLoad()
                    }
                }
            }
        } else {
            this.showFloatingSnackBar(webView, text = "Your android does not support proxy enabled WebView", actionText = "Continue", actionClick = {
                load()
            }
            )
        }
    }

    private fun setProxy() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {

            val proxyConfig = ProxyConfig.Builder()
                    .addProxyRule("SOCKS:/${SentinelState.torProxy?.address().toString()}")
                    .build()

            Timber.i("Proxy: SOCKS:/${SentinelState.torProxy?.address().toString()}")
            ProxyController.getInstance().setProxyOverride(proxyConfig, {

            }, {

            })
            load()
        }
    }

    private fun torStartAndLoad() {
        TorServiceController.startTor()
        SentinelState.torStateLiveData().observe(this, {
            if (it == ON) {
                setProxy()
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun load() {
        webView.settings.builtInZoomControls = true
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressWeb.visibility = View.VISIBLE
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                Timber.i("onPageCommitVisible: ")
                progressWeb.visibility = View.INVISIBLE
            }
        }
        tx?.let {
            url = ExplorerRepository.getExplorer(it.hash)
            webView.loadUrl(url)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.web_explorer, menu)
        SentinelState.torStateLiveData().observe(this, {
            menu.findItem(R.id.menu_web_tor).icon = ContextCompat.getDrawable(applicationContext, R.drawable.ic_tor_on)
            val icon = menu.findItem(R.id.menu_web_tor).icon
            when (it) {
                WAITING -> {
                    icon.setTint(ContextCompat.getColor(applicationContext, R.color.md_amber_300))
                }
                ON -> {
                    icon.setTint(ContextCompat.getColor(applicationContext, R.color.md_green_600))
                }
                OFF -> {
                    menu.findItem(R.id.menu_web_tor).icon = ContextCompat.getDrawable(applicationContext, R.drawable.ic_tor_disabled)
                    menu.findItem(R.id.menu_web_tor).icon.setTint(ContextCompat.getColor(applicationContext, R.color.md_grey_400))
                }
                else -> {
                }
            }
        })
        return super.onCreateOptionsMenu(menu)
    }


    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.menu_web_copy_tx -> {
                tx?.hash?.let { copyText(it) }
            }
            R.id.menu_web_copy_url -> {
                webView.url?.let { copyText(it) }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else
            super.onBackPressed()
    }

    private fun copyText(string: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clipData = ClipData
                .newPlainText("", string)
        if (cm != null) {
            cm.setPrimaryClip(clipData)
            Toast.makeText(applicationContext, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
        }
    }
}