package com.samourai.sentinel.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.sentinel.BuildConfig
import com.samourai.sentinel.R
import com.samourai.sentinel.api.APIConfig
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.service.WebSocketHandler
import com.samourai.sentinel.service.WebSocketService
import com.samourai.sentinel.tor.TorServiceController
import com.samourai.sentinel.ui.SentinelActivity
import com.samourai.sentinel.ui.adapters.CollectionsAdapter
import com.samourai.sentinel.ui.collectionDetails.CollectionDetailsActivity
import com.samourai.sentinel.ui.dojo.DojoConfigureBottomSheet
import com.samourai.sentinel.ui.fragments.AddNewPubKeyBottomSheet
import com.samourai.sentinel.ui.settings.NetworkActivity
import com.samourai.sentinel.ui.settings.SettingsActivity
import com.samourai.sentinel.ui.utils.*
import com.samourai.sentinel.ui.views.confirm
import com.samourai.sentinel.util.FormatsUtil
import com.samourai.sentinel.util.MonetaryUtil
import io.matthewnelson.topl_service.prefs.TorServicePrefs
import kotlinx.android.synthetic.main.activity_home.*
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber


class HomeActivity : SentinelActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private val collectionsAdapter = CollectionsAdapter();
    private val webSocketHandler: WebSocketHandler by inject(WebSocketHandler::class.java);
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java);
    private var connectingDojo = false
    private lateinit var torServicePrefs: TorServicePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbarHome)
        torServicePrefs = TorServicePrefs(this)

        val model: HomeViewModel by viewModels()

        setUp()

        setUpCollectionList()

        model.getCollections().observe(this, {
            if (it.isNotEmpty())
                welcomeMessage.visibility = View.GONE
            else
                welcomeMessage.visibility = View.VISIBLE

            collectionsAdapter.update(it)
        })

        model.getBalance().observe(this, {
            updateBalance(it)
        })

        model.getFiatBalance().observe(this, { updateFiat(it) })

        fab.setOnClickListener {
            if (prefsUtil.haptics!!) {
                fab.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            connectingDojo = false
            if (!AndroidUtil.isPermissionGranted(Manifest.permission.CAMERA, applicationContext)) {
                this.askCameraPermission()
            } else {
                showPubKeyBottomSheet()
            }

        }

        model.loading().observe(this, {
            swipeRefreshCollection.isRefreshing = it
        })
        model.getErrorMessage().observe(this, {
            if (it != "null")
                this@HomeActivity.showFloatingSnackBar(fab, text = "Error: $it")
        })

        swipeRefreshCollection.setOnRefreshListener {
            swipeRefreshCollection.isRefreshing = false
            if (SentinelState.isTorRequired()) {
                if (SentinelState.torState == SentinelState.TorState.WAITING) {
                    this.showFloatingSnackBar(fab, anchorView = fab.id,
                            text = "Tor is bootstrapping! please wait and try again")
                }
                if (SentinelState.torState == SentinelState.TorState.OFF) {
                    this.showFloatingSnackBar(fab,
                            "Tor is required! Please turn on Tor",
                            actionText = "Turn on",
                            actionClick = {
                                TorServiceController.startTor()
                            })
                }
                if (SentinelState.torState == SentinelState.TorState.ON) {
                    model.fetchBalance()
                }
            } else {
                model.fetchBalance()
            }
        }

        fetch(model)

        if (SentinelState.isTorRequired()) {
            SentinelState.torStateLiveData().observe(this, {
                if (it == SentinelState.TorState.ON)
                    WebSocketService.start(applicationContext)
            })
        } else {
            WebSocketService.start(applicationContext)
        }

        checkClipBoard()
    }


    private fun fetch(model: HomeViewModel) {
        if (!SentinelState.isRecentlySynced()) {
            if (SentinelState.isTorRequired() && SentinelState.isTorStarted()) {
                model.fetchBalance()
            } else {
                SentinelState.torStateLiveData().observe(this, Observer {
                    if (it == SentinelState.TorState.ON) {
                        model.fetchBalance()
                    }
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (SentinelState.isTestNet() && !title.contains("TestNet")) {
            title = "$title | TestNet"
        }
    }

    private fun setUp() {
        if (prefsUtil.firstRun!! && BuildConfig.DEBUG) {
            this.confirm(label = "Choose network",
                    positiveText = "TestNet",
                    negativeText = "MainNet",
                    onConfirm = { confirm ->

                        prefsUtil.firstRun = false
                        if (confirm) {
                            prefsUtil.testnet = true
                        }
                        if (!SentinelState.isTestNet()) {
                            title = "$title".removeSuffix("| TestNet")
                        }
                        showServerConfig()
                    })
        } else {
            showServerConfig()
        }
    }

    private fun showServerConfig() {
        if (prefsUtil.apiEndPoint.isNullOrEmpty()) {
            this.confirm(label = "Notice",
                    message = getString(R.string.server_config_instruction),
                    positiveText = "Connect to Dojo",
                    negativeText = "Use default server",
                    isCancelable = false,
                    onConfirm = { confirm ->
                        if (confirm) {
                            connectingDojo = true
                            if (!AndroidUtil.isPermissionGranted(Manifest.permission.CAMERA, applicationContext)) {
                                this.askCameraPermission()
                            } else {
                                showDojoSetUpBottomSheet()
                            }
                        } else {
                            if (prefsUtil.testnet!!) {
                                prefsUtil.apiEndPoint = APIConfig.SAMOURAI_API_TESTNET
                                prefsUtil.apiEndPointTor = APIConfig.SAMOURAI_API_TOR_TESTNET
                            } else {
                                prefsUtil.apiEndPoint = APIConfig.SAMOURAI_API
                                prefsUtil.apiEndPointTor = APIConfig.SAMOURAI_API_TOR
                            }
                        }
                    })

        }
    }

    private fun checkClipBoard() {
        val clipData = AndroidUtil.getClipBoardString(applicationContext)

        if (clipData != null) {
            val formatted = FormatsUtil.extractPublicKey(clipData)
            if (FormatsUtil.isValidBitcoinAddress(formatted) || FormatsUtil.isValidXpub(formatted)) {
                showFloatingSnackBar(fab, text = "Public Key detected in clipboard", actionText = "Add", actionClick = {
                    val bottomSheetFragment = AddNewPubKeyBottomSheet(formatted)
                    bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                })
            }

        }
    }

    private fun showDojoSetUpBottomSheet() {
        val dojoConfigureBottomSheet = DojoConfigureBottomSheet()
        dojoConfigureBottomSheet.show(supportFragmentManager, dojoConfigureBottomSheet.tag)
        dojoConfigureBottomSheet.setDojoConfigurationListener(object : DojoConfigureBottomSheet.DojoConfigurationListener {
            override fun onDismiss() {
                if (!prefsUtil.isAPIEndpointEnabled()) {
                    showServerConfig()
                }
            }
        })
    }

    private fun updateBalance(it: Long) {
        homeBalanceBtc.text = "${MonetaryUtil.getInstance().formatToBtc(it)} BTC"
    }

    private fun updateFiat(it: String) {
        exchangeRateTxt.text = it
    }

    private fun showPubKeyBottomSheet() {
        val bottomSheetFragment = AddNewPubKeyBottomSheet()
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }


    private fun setUpCollectionList() {

        collectionsAdapter.setOnClickListener {
            startActivity(Intent(applicationContext, CollectionDetailsActivity::class.java).apply {
                putExtra("collection", it.id)
            })
        }

        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        val decorator = RecyclerViewItemDividerDecorator(ContextCompat.getDrawable(applicationContext, R.drawable.divider_home)!!);
        collectionRecyclerView.apply {
            adapter = collectionsAdapter
            layoutManager = linearLayoutManager
            itemAnimator = SlideInItemAnimator(slideFromEdge = Gravity.TOP)
            setHasFixedSize(true)
            addItemDecoration(decorator)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!SentinelState.checkedClipBoard) {
            checkClipBoard()
            SentinelState.checkedClipBoard = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (connectingDojo) {
                showDojoSetUpBottomSheet()
                return
            }
            showPubKeyBottomSheet()

        } else {
            if (requestCode == CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                if (connectingDojo) {
                    showDojoSetUpBottomSheet()
                    return
                }
                showPubKeyBottomSheet()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_options_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return super.onCreateOptionsMenu(menu)
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

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let { setNetWorkMenu(it) }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setNetWorkMenu(menu: Menu) {
        val alertMenuItem: MenuItem = menu.findItem(R.id.activity_home_menu_network)
        val rootView = alertMenuItem.actionView
        val statusCircle = rootView.findViewById<View>(R.id.home_menu_network_shape) as FrameLayout
        val shape = ContextCompat.getDrawable(applicationContext, R.drawable.circle_shape);
        shape?.setTint(ContextCompat.getColor(applicationContext, R.color.red))
        statusCircle.background = shape
        statusCircle.visibility = View.VISIBLE
        SentinelState.torStateLiveData().observe(this, Observer {
            if (it == SentinelState.TorState.ON) {
                shape?.setTint(ContextCompat.getColor(applicationContext, R.color.green_ui_2))
            }
            if (it == SentinelState.TorState.OFF) {
                shape?.setTint(ContextCompat.getColor(applicationContext, R.color.red))
            }
            if (it == SentinelState.TorState.WAITING) {
                shape?.setTint(ContextCompat.getColor(applicationContext, R.color.warning_yellow))
            }
            statusCircle.background = shape
            statusCircle.visibility = View.VISIBLE
        })

        rootView.setOnClickListener {
            startActivity(Intent(this, NetworkActivity::class.java))
        }
    }

    fun connectSocket() {
        try {
            webSocketHandler.connect()
        } catch (ex: Exception) {
        }
    }
}