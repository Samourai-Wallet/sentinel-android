package com.samourai.sentinel.ui.dojo

import android.Manifest
import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.samourai.sentinel.R
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.tor.TorEventsReceiver
import com.samourai.sentinel.tor.TorServiceController
import com.samourai.sentinel.ui.views.codeScanner.CameraFragmentBottomSheet
import com.samourai.sentinel.ui.views.codeScanner.CodeScanner
import com.samourai.sentinel.ui.views.codeScanner.CodeScannerView
import com.samourai.sentinel.ui.views.codeScanner.DecodeCallback
import com.samourai.sentinel.ui.utils.AndroidUtil
import com.samourai.sentinel.ui.views.GenericBottomSheet
import com.samourai.sentinel.util.apiScope
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_bottomsheet_view_pager.*
import kotlinx.coroutines.*
import org.koin.java.KoinJavaComponent

class DojoConfigureBottomSheet : GenericBottomSheet() {
    private val scanFragment = ScanFragment()
    private val dojoConfigureBottomSheet = DojoNodeInstructions()
    private val dojoConnectFragment = DojoConnectFragment()
    private val compositeDisposables = CompositeDisposable()
    private var dojoConfigurationListener: DojoConfigurationListener? = null
    private var cameraFragmentBottomSheet: CameraFragmentBottomSheet? = null

    private var payload: String = ""

    private val dojoUtil: DojoUtility by KoinJavaComponent.inject(DojoUtility::class.java);


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet, container)
        view.findViewById<TextView>(R.id.dialogTitle).text = "Setup Dojo Node"
        val configLayout = inflater.inflate(R.layout.fragment_bottomsheet_view_pager, container)
        val content = view.findViewById<FrameLayout>(R.id.contentContainer)
        content.addView(configLayout)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViewPager()
        dojoConfigureBottomSheet.setConnectListener(View.OnClickListener {
            pager.setCurrentItem(1, true)
        })
        scanFragment.setOnScanListener {
            if (dojoUtil.validate(it)) {
                payload = it
                pager.setCurrentItem(2, true)
            } else {
                scanFragment.resetCamera()
                Toast.makeText(requireContext(), "Invalid payload", Toast.LENGTH_SHORT).show()
            }
        }
        pager.registerOnPageChangeCallback(pagerCallBack)
    }

    private val pagerCallBack = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            if (position == 2) {
                startTorAndConnect()
            }
        }
    }

    private fun startTorAndConnect() {
        if (SentinelState.isTorStarted()) {
            dojoConnectFragment.showTorProgressSuccess()
            setDojo()
        } else {
            TorServiceController.startTor()
            dojoConnectFragment.showTorProgress()
            TorServiceController.appEventBroadcaster.let {
                (it as TorEventsReceiver).torLogs.observe(this.viewLifecycleOwner, { log ->
                    if (log.contains("Bootstrapped 100%")) {
                        dojoConnectFragment.showTorProgressSuccess()
                        setDojo()
                    }
                })
            }
        }
    }

    private fun setDojo() {
        dojoConnectFragment.showDojoProgress()
        apiScope.launch {
            try {
                val call = async { dojoUtil.setDojo(payload) }
                val response = call.await()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        dojoUtil.setAuthToken(body)
                    }
                }
                withContext(Dispatchers.Main) {
                    dojoConnectFragment.showDojoProgressSuccess()
                    delay(500)
                    Handler().postDelayed(Runnable {
                        this@DojoConfigureBottomSheet.dojoConfigurationListener?.onDismiss()
                        this@DojoConfigureBottomSheet.dismiss()
                    }, 500)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    fun setDojoConfigurationListener(dojoConfigurationListener: DojoConfigurationListener?) {
        this.dojoConfigurationListener = dojoConfigurationListener
    }

    private fun setUpViewPager() {
        val item = arrayListOf<Fragment>()
        item.add(dojoConfigureBottomSheet)
        item.add(scanFragment)
        item.add(dojoConnectFragment)
        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return item.size
            }

            override fun createFragment(position: Int): Fragment {
                return item[position];
            }

        }
        pager.isUserInputEnabled = false

        //Fix for making BottomSheet same height across all the fragments
        pager.visibility = View.GONE
        pager.currentItem = 1
        pager.currentItem = 0
        pager.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        pager.unregisterOnPageChangeCallback(pagerCallBack)
        super.onDestroyView()
    }

    private fun showConnectionAlert() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Dialog)
        dialog.setContentView(R.layout.dojo_connect_dialog)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        dialog.findViewById<View>(R.id.dojo_scan_qr).setOnClickListener { view: View? ->
            dialog.dismiss()
            cameraFragmentBottomSheet = CameraFragmentBottomSheet()
            cameraFragmentBottomSheet!!.show(requireActivity().supportFragmentManager, cameraFragmentBottomSheet!!.tag)
            cameraFragmentBottomSheet!!.setQrCodeScanLisenter { code: String ->
                cameraFragmentBottomSheet!!.dismiss()
                connectToDojo(code)
            }
        }

        dialog.findViewById<View>(R.id.dojo_paste_config).setOnClickListener { view: View? ->
            try {
                val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val item = clipboard.primaryClip!!.getItemAt(0)
                connectToDojo(item.text.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            dialog.dismiss()
        }
    }

    private fun connectToDojo(dojoParams: String) {
//        btnGroup!!.visibility = View.INVISIBLE
//        progressGroup!!.visibility = View.VISIBLE
//        dojoConnectProgress!!.progress = 30
//        if (TorManager.getInstance(requireActivity().applicationContext).isConnected) {
//            dojoConnectProgress!!.progress = 60
//            progressStates!!.text = "Tor Connected, Connecting to Dojo Node..."
////            DojoUtil.getInstance(requireActivity().applicationContext).clear()
//            doPairing(dojoParams)
//        } else {
//            progressStates!!.text = "Waiting for Tor..."
//            val startIntent = Intent(requireActivity().applicationContext, TorService::class.java)
//            startIntent.action = TorService.START_SERVICE
//            requireActivity().startService(startIntent)
//            val disposable = TorManager.getInstance(requireActivity().applicationContext).torStatus
//                    .subscribeOn(Schedulers.newThread())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe { state: CONNECTION_STATES ->
//                        if (state == CONNECTION_STATES.CONNECTING) {
//                            progressStates!!.text = "Waiting for Tor..."
//                        } else if (state == CONNECTION_STATES.CONNECTED) {
//                            PrefsUtil.getInstance(activity).setValue(PrefsUtil.ENABLE_TOR, true)
//                            dojoConnectProgress!!.progress = 60
//                            progressStates!!.text = "Tor Connected, Connecting to Dojo Node..."
////                            DojoUtil.getInstance(requireActivity().applicationContext).clear()
//                            doPairing(dojoParams)
//                        }
//                    }
//            compositeDisposables.add(disposable)
//        }
    }


    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        this.dojoConfigurationListener?.onDismiss()
    }

    override fun onDestroy() {
        if (!compositeDisposables.isDisposed) compositeDisposables.dispose()
        super.onDestroy()
    }

    interface DojoConfigurationListener {
        fun onDismiss()
    }
}

class DojoNodeInstructions : Fragment() {

    private var connectOnClickListener: View.OnClickListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottomsheet_dojo_configure_instruction, container, false);
    }

    fun setConnectListener(listener: View.OnClickListener) {
        connectOnClickListener = listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<MaterialButton>(R.id.connect_dojo).setOnClickListener { view ->
            connectOnClickListener?.onClick(view)
        }
    }


}


class DojoConnectFragment : Fragment() {


    private lateinit var checkImageTor: ImageView
    private lateinit var checkImageDojo: ImageView
    private lateinit var progressTor: ProgressBar
    private lateinit var progressDojo: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottomsheet_dojo_connection, container, false)
        progressTor = view.findViewById(R.id.progressTor)
        checkImageTor = view.findViewById(R.id.checkImageTor)

        checkImageDojo = view.findViewById(R.id.checkImageDojo)
        progressDojo = view.findViewById(R.id.progressDojo)

        return view
    }


    fun showTorProgress() {
        progressTor.visibility = View.VISIBLE
        checkImageTor.visibility = View.INVISIBLE
        progressTor.animate()
                .alpha(1f)
                .setDuration(600)
                .start()
    }

    fun showTorProgressSuccess() {
        progressTor.visibility = View.INVISIBLE
        checkImageTor.visibility = View.VISIBLE
    }

    fun showDojoProgress() {
        progressDojo.visibility = View.VISIBLE
        checkImageDojo.visibility = View.INVISIBLE
    }

    fun showDojoProgressSuccess() {
        progressDojo.visibility = View.INVISIBLE
        checkImageDojo.visibility = View.VISIBLE
    }
}

class ScanFragment : Fragment() {

    private var mCodeScanner: CodeScanner? = null
    private val appContext: Context by KoinJavaComponent.inject(Context::class.java);
    private var onScan: (scanData: String) -> Unit = {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_scan_layout, container, false);
    }

    fun setOnScanListener(callback: (scanData: String) -> Unit) {
        this.onScan = callback
    }

    fun resetCamera() {
        this.mCodeScanner?.stopPreview()
        this.mCodeScanner?.startPreview()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mCodeScanner = CodeScanner(appContext, view.findViewById(R.id.scannerViewXpub))
        view.findViewById<TextView>(R.id.scanInstructions).text = getString(R.string.dojo_scan_instruction)
        view.findViewById<TextView>(R.id.scanInstructions).textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        mCodeScanner?.decodeCallback = DecodeCallback {
            GlobalScope.launch(Dispatchers.Main) {
                mCodeScanner?.stopPreview()
                mCodeScanner?.releaseResources()
                onScan(it.text)
            }
        }

        view.findViewById<CodeScannerView>(R.id.scannerViewXpub).setOnClickListener {
            if (mCodeScanner?.isPreviewActive == false) {
                mCodeScanner?.startPreview()
            }
        }
        val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        view.findViewById<Button>(R.id.pastePubKey).setOnClickListener {
            when {
                !clipboard.hasPrimaryClip() -> {
                    Toast.makeText(context, "Payload not found in clipboard", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    try {
                        val item = clipboard.primaryClip?.getItemAt(0)
                        onScan(item?.text.toString())
                    } catch (e: Exception) {
                        Toast.makeText(context, "Payload not found in clipboard", Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }

    }


    override fun onResume() {
        super.onResume()
        if (AndroidUtil.isPermissionGranted(Manifest.permission.CAMERA, appContext)) {
            mCodeScanner?.startPreview()
        }
    }

    override fun onPause() {
        mCodeScanner?.releaseResources()
        super.onPause()
    }

}
