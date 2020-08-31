package com.samourai.sentinel.ui.fragments

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.samourai.sentinel.R
import com.samourai.sentinel.ui.views.codeScanner.CodeScanner
import com.samourai.sentinel.ui.views.codeScanner.CodeScannerView
import com.samourai.sentinel.ui.views.codeScanner.DecodeCallback
import com.samourai.sentinel.data.AddressTypes
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.ui.adapters.CollectionsAdapter
import com.samourai.sentinel.ui.collectionEdit.CollectionEditActivity
import com.samourai.sentinel.ui.utils.AndroidUtil
import com.samourai.sentinel.ui.utils.RecyclerViewItemDividerDecorator
import com.samourai.sentinel.ui.views.GenericBottomSheet
import com.samourai.sentinel.util.FormatsUtil
import kotlinx.android.synthetic.main.content_choose_address_type.*
import kotlinx.android.synthetic.main.content_collection_select.*
import kotlinx.android.synthetic.main.fragment_bottomsheet_view_pager.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent


class AddNewPubKeyBottomSheet : GenericBottomSheet() {

    private val scanPubKeyFragment = ScanPubKeyFragment()
    private var newPubKeyListener: ((pubKey: PubKeyModel?) -> Unit)? = null
    private val selectAddressTypeFragment = SelectAddressTypeFragment()
    private val chooseCollectionFragment = ChooseCollectionFragment()
    private var pubKeyString = ""
    private var pubKeyModel: PubKeyModel? = null
    private var selectedPubKeyCollection: PubKeyCollection? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet, container)
        view.findViewById<TextView>(R.id.dialogTitle).text = "Add new public key"
        val fragmentLayout = inflater.inflate(R.layout.fragment_bottomsheet_view_pager, container)
        val content = view.findViewById<FrameLayout>(R.id.contentContainer)
        content.addView(fragmentLayout)
        return view
    }

    fun setSelectedCollection(pubKeyCollection: PubKeyCollection) {
        selectedPubKeyCollection = pubKeyCollection
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        setUpViewPager()

        scanPubKeyFragment.setOnScanListener {
            validate(it)
            pubKeyString = it
        }

        selectAddressTypeFragment.setOnSelectListener {
            validateXPUB(it)
        }

        chooseCollectionFragment.setOnSelectListener {
            startActivity(Intent(context, CollectionEditActivity::class.java).apply {
                this.putExtra("pubKey", pubKeyModel)
                if (it != null) {
                    this.putExtra("collection", it.id)
                }
            })
            this.dismiss()
        }

    }

    private fun validateXPUB(addressTypes: AddressTypes) {

        pubKeyModel = if ((addressTypes == AddressTypes.BIP49 || addressTypes == AddressTypes.BIP84) && (pubKeyString.startsWith("xpub") || pubKeyString.startsWith("tpub"))) {
            PubKeyModel(pubKey = pubKeyString, balance = 0, account_index = 0, change_index = 1, label = "Untitled", type = addressTypes)
        } else if (pubKeyString.startsWith("ypub") || pubKeyString.startsWith("upub")) {
            PubKeyModel(pubKey = pubKeyString, balance = 0, account_index = 0, change_index = 1, label = "Untitled", type = addressTypes)
        } else if (pubKeyString.startsWith("zpub") || pubKeyString.startsWith("vpub")) {
            PubKeyModel(pubKey = pubKeyString, balance = 0, account_index = 0, change_index = 1, label = "Untitled", type = addressTypes)
        } else {
            PubKeyModel(pubKey = pubKeyString, balance = 0, account_index = 0, change_index = 1, label = "Untitled", type = AddressTypes.BIP44)
        }

        if (newPubKeyListener != null) {
            this.newPubKeyListener?.let { it(pubKeyModel) }
            this.dismiss()
            return
        }
        if (selectedPubKeyCollection != null) {
            startActivity(Intent(context, CollectionEditActivity::class.java).apply {
                this.putExtra("pubKey", pubKeyModel)
                this.putExtra("collection", selectedPubKeyCollection?.id)
            })
            this.dismiss()
        } else {
            pager.setCurrentItem(2, true)
        }

    }

    private fun validate(code: String) {

        var payload = code

        if (code.startsWith("BITCOIN:")) {
            payload = code.substring(8)

        }
        if (code.startsWith("bitcoin:")) {
            payload = code.substring(8)
        }
        if (code.startsWith("bitcointestnet:")) {
            payload = code.substring(15)
        }
        if (code.contains("?")) {
            payload = code.substring(0, code.indexOf("?"))
        }
        if (code.contains("?")) {
            payload = code.substring(0, code.indexOf("?"))
        }

        var type = AddressTypes.ADDRESS

        if (code.startsWith("xpub") || code.startsWith("tpub")) {
             type = AddressTypes.BIP44
        } else if (code.startsWith("ypub") || code.startsWith("upub")) {
            type = AddressTypes.BIP49
        } else if (code.startsWith("zpub") || code.startsWith("vpub")) {
            type = AddressTypes.BIP84
        }


        when {
            FormatsUtil.getInstance().isValidBitcoinAddress(payload.trim()) -> {
                if (newPubKeyListener != null) {
                    val pubKey = PubKeyModel(pubKey = payload, type = AddressTypes.ADDRESS, label = "Untitled")
                    newPubKeyListener?.let { it(pubKey) }
                    this.dismiss()
                    return
                } else {
                    pubKeyModel = PubKeyModel(pubKey = payload, type = AddressTypes.ADDRESS, label = "Untitled")
                    // Skip type selection screen since payload is an address
                    pager.setCurrentItem(2, true)
                }
            }
            FormatsUtil.getInstance().isValidXpub(code) -> {
                //show option to choose xpub type
                pager.setCurrentItem(1, true)
                pager.post {
                    selectAddressTypeFragment.setType(type)
                }
            }
            else -> {
                Toast.makeText(context, "Invalid public key or payload", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setUpViewPager() {
        val item = arrayListOf<Fragment>()
        item.add(scanPubKeyFragment)
        item.add(selectAddressTypeFragment)
        item.add(chooseCollectionFragment)
        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return item.size;
            }

            override fun createFragment(position: Int): Fragment {
                return item[position];
            }

        }
        pager.isUserInputEnabled = false
    }

    fun setPubKeyListener(listener: (pubKey: PubKeyModel?) -> Unit) {
        newPubKeyListener = listener;
    }
}


class ScanPubKeyFragment : Fragment() {

    private var mCodeScanner: CodeScanner? = null
    private val appContext: Context by KoinJavaComponent.inject(Context::class.java);
    private var onScan: (scanData: String) -> Unit = {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_scan_layout, container, false);
    }

    fun setOnScanListener(callback: (scanData: String) -> Unit) {
        this.onScan = callback
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mCodeScanner = CodeScanner(appContext, view.findViewById(R.id.scannerViewXpub))
        view.findViewById<TextView>(R.id.scanInstructions).text = getString(R.string.pub_key_scan_instruction)
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
                    Toast.makeText(context, "PubKey not found in clipboard", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    try {
                        val item = clipboard.primaryClip?.getItemAt(0)
                        onScan(item?.text.toString())
                    } catch (e: Exception) {
                        Toast.makeText(context, "PubKey not found in clipboard", Toast.LENGTH_SHORT).show()
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

class SelectAddressTypeFragment : Fragment() {
    private var onSelect: (type: AddressTypes) -> Unit = {}
    var addressType: AddressTypes = AddressTypes.BIP44


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_choose_address_type, container, false);
    }

    fun setOnSelectListener(callback: (type: AddressTypes) -> Unit = {}) {
        this.onSelect = callback;
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        radioGroup.setOnCheckedChangeListener { radioGroup, i ->
            when (i) {
                0 -> {
                    addressType = AddressTypes.BIP44
                }
                1 -> {
                    addressType = AddressTypes.BIP49
                }
                2 -> {
                    addressType = AddressTypes.BIP84
                }
            }
        }
        nextBtn.setOnClickListener {
            onSelect(addressType)
        }
        when (addressType) {
            AddressTypes.BIP44 -> {
                buttonBIP44.isChecked = true
            }
            AddressTypes.BIP49 -> {
                buttonBIP49.isChecked = true
            }
            AddressTypes.BIP84 -> {
                buttonBIP84.isChecked = true
            }
            AddressTypes.ADDRESS -> {
                //No-op
            }
        }
    }

    fun setType(type: AddressTypes?) {
        if (type != null) {
            addressType = type

        }
    }

}

class ChooseCollectionFragment : Fragment() {

    private val repository: CollectionRepository by KoinJavaComponent.inject(CollectionRepository::class.java)
    private val collectionsAdapter = CollectionsAdapter()
    private var onSelect: (PubKeyCollection?) -> Unit = {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_collection_select, container, false);
    }

    fun setOnSelectListener(callback: (PubKeyCollection?) -> Unit = {}) {
        this.onSelect = callback
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setUpCollectionSelectList()
        createNewCollection.setOnClickListener {
            this.onSelect(null)
        }
    }

    private fun setUpCollectionSelectList() {

        repository.collectionsLiveData.observe(viewLifecycleOwner, Observer {
            collectionsAdapter.update(it);
        })
        collectionsAdapter.setLayoutType(CollectionsAdapter.Companion.LayoutType.STACKED)
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        val decorator = RecyclerViewItemDividerDecorator(ContextCompat.getDrawable(requireContext(), R.drawable.divider_grey)!!);
        collectionSelectRecyclerView.apply {
            adapter = collectionsAdapter
            layoutManager = linearLayoutManager
            setHasFixedSize(true)
            addItemDecoration(decorator)
        }

        collectionsAdapter.setOnClickListener {
            this.onSelect(it)
        }
    }


}