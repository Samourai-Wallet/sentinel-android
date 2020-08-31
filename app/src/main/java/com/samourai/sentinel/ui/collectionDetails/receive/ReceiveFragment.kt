package com.samourai.sentinel.ui.collectionDetails.receive

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.client.android.Contents
import com.google.zxing.client.android.encode.QRCodeEncoder
import com.samourai.sentinel.R
import com.samourai.sentinel.core.SentinelState
import com.samourai.sentinel.core.hd.HD_Account
import com.samourai.sentinel.core.segwit.P2SH_P2WPKH
import com.samourai.sentinel.data.AddressTypes
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.ui.SentinelActivity
import com.samourai.sentinel.ui.collectionDetails.transactions.TransactionsViewModel
import com.samourai.sentinel.ui.views.confirm
import com.samourai.sentinel.util.MonetaryUtil
import com.samourai.wallet.segwit.SegwitAddress
import kotlinx.android.synthetic.main.grid.view.*
import org.koin.java.KoinJavaComponent
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.ParseException

class ReceiveFragment : Fragment() {

    private lateinit var collection: PubKeyCollection;
    private lateinit var toolbar: Toolbar
    private lateinit var receiveQR: ImageView
    private lateinit var receiveAddressText: TextView
    private lateinit var collectionBalanceBtc: TextView
    private var pubKeyIndex = 0
    private lateinit var pubKeyDropDown: AutoCompleteTextView
    private val monetaryUtil: MonetaryUtil by KoinJavaComponent.inject(MonetaryUtil::class.java)
    private val receiveViewModel: ReceiveViewModel by viewModels()

    private lateinit var qrFile: String

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_receive, container, false)
        toolbar = root.findViewById(R.id.toolbarReceive)
        receiveQR = root.findViewById(R.id.receiveQR)
        receiveAddressText = root.findViewById(R.id.receiveAddressText)
        pubKeyDropDown = root.findViewById(R.id.pubKeySelector)
        collectionBalanceBtc = root.findViewById(R.id.collectionBalanceBtc)

        qrFile = "${requireContext().cacheDir.path}${File.separator}qr.png";


        setUpSpinner()

        generateQR()

        setUpToolBar()

        receiveAddressText.setOnClickListener {

            val cm = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val clipData = ClipData
                    .newPlainText("Address", (it as TextView).text)
            if (cm != null) {
                cm.setPrimaryClip(clipData)
                Toast.makeText(context, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
            }

        }

        return root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun share() {

        ( this.activity as (AppCompatActivity) ).confirm(
                positiveText = "Share as QR code",
                negativeText = "Copy Address to clipboard",
                label = "Share options",
                onConfirm = {
                    if(it){
                        shareQR()
                    }else{
                        val cm = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                        val clipData = ClipData
                                .newPlainText("Address", receiveAddressText.text)
                        if (cm != null) {
                            cm.setPrimaryClip(clipData)
                            Toast.makeText(context, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        )
    }

    private fun shareQR() {
        val file = File(qrFile)
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }
        file!!.setReadable(true, false)

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
        } catch (fnfe: FileNotFoundException) {
        }

        var clip: ClipData? = null
        clip = ClipData.newPlainText("Receive address", receiveAddressText.text)
        (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)

        if (fos != null) {
            val bitmap = (receiveQR.drawable as BitmapDrawable).bitmap
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos)
            try {
                fos.close()
            } catch (ioe: IOException) {
            }
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.type = "image/png"
            if (Build.VERSION.SDK_INT >= 24) {
                //From API 24 sending FIle on intent ,require custom file provider
                intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                        requireContext(),
                        requireContext()
                                .packageName + ".provider", file))
            } else {
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
            }
            startActivity(Intent.createChooser(intent, requireContext().getText(R.string.send_payment_code)))
        }

    }

    private fun generateQR() {
        val display = activity?.windowManager?.defaultDisplay
        val size = Point()
        display?.getSize(size)
        val imgWidth = size.x - 200
        val addr = getAddress()
        receiveAddressText.text = addr
        try {
//            val amount = NumberFormat.getInstance(Locale.US).parse(edAmountBTC.getText().toString())
            receiveQR.setImageBitmap(generateQRCode(addr, imgWidth))
        } catch (nfe: NumberFormatException) {
            receiveQR.setImageBitmap(generateQRCode(addr, imgWidth))
        } catch (pe: ParseException) {
            receiveQR.setImageBitmap(generateQRCode(addr, imgWidth))
        }
    }


    private fun generateQRCode(uri: String, imgWidth: Int): Bitmap? {
        var bitmap: Bitmap? = null
        val qrCodeEncoder = QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth)
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap()
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        return bitmap
    }


    fun getAddress(): String {
        if (collection.pubs.size == 0) {
            return ""
        }

        if (collection.pubs[pubKeyIndex].type == AddressTypes.ADDRESS) {
            return collection.pubs[pubKeyIndex].pubKey
        }
        val pubKey = collection.pubs[pubKeyIndex]
        val accountIndex = collection.pubs[pubKeyIndex].account_index

        return when (collection.pubs[pubKeyIndex].type) {
            AddressTypes.BIP44 -> {
                val account = HD_Account(SentinelState.getNetworkParam(), pubKey.pubKey, "", 0)
                account.getChain(0).addrIdx = accountIndex
                val hdAddress = account.getChain(0).getAddressAt(accountIndex)
                hdAddress.addressString
            }
            AddressTypes.BIP49 -> {
                val account = HD_Account(SentinelState.getNetworkParam(), pubKey.pubKey, "", 0)
                account.getChain(0).addrIdx = accountIndex
                val address = account.getChain(0).getAddressAt(accountIndex)
                val ecKey = address.ecKey
                val p2shP2wpkH = P2SH_P2WPKH(ecKey.pubKey, SentinelState.getNetworkParam())
                p2shP2wpkH.addressAsString
            }
            AddressTypes.BIP84 -> {
                val account = HD_Account(SentinelState.getNetworkParam(), pubKey.pubKey, "", 0)
                account.getChain(0).addrIdx = accountIndex
                val address = account.getChain(0).getAddressAt(accountIndex)
                val ecKey = address.ecKey
                val segwitAddress = SegwitAddress(ecKey.pubKey, SentinelState.getNetworkParam())
                segwitAddress.bech32AsString
            }
            AddressTypes.ADDRESS -> {
                collection.pubs[pubKeyIndex].pubKey
            }
            else -> ""
        }
    }

    private fun setUpToolBar() {
        (activity as SentinelActivity).setSupportActionBar(toolbar)
        (activity as SentinelActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.title = collection.collectionLabel
        collectionBalanceBtc.text = "${monetaryUtil.formatToBtc(collection.balance)} BTC"
    }


    /**
     * This method will be called immediately after creating fragment
     * this ensure  onCreateView method
     */
    fun setCollection(collection: PubKeyCollection) {
        this.collection = collection
        if (isAdded) {
            setUpSpinner()
            setUpToolBar()
        }
    }


    private fun setUpSpinner() {
        val items = collection.pubs.map { it.label }.toMutableList()
        if (items.size != 0) {
            val adapter: ArrayAdapter<String> = ArrayAdapter(requireContext(),
                    R.layout.dropdown_menu_popup_item, items)
            pubKeyDropDown.inputType = InputType.TYPE_NULL
            pubKeyDropDown.setAdapter(adapter)
            pubKeyDropDown.setText(items.first(), false)
            pubKeyDropDown.onItemClickListener = AdapterView.OnItemClickListener { _, _, index, _ ->
                pubKeyIndex = index
                generateQR()
            }

        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.collection_detail_receive_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.collection_details_share_qr) {
            share()
        }

        return super.onOptionsItemSelected(item)
    }


}