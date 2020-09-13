package com.samourai.sentinel.ui.collectionEdit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.samourai.sentinel.R
import com.samourai.sentinel.data.AddressTypes
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.data.repository.TransactionsRepository
import com.samourai.sentinel.service.ImportSegWitService
import com.samourai.sentinel.ui.SentinelActivity
import com.samourai.sentinel.ui.fragments.AddNewPubKeyBottomSheet
import com.samourai.sentinel.ui.home.HomeActivity
import com.samourai.sentinel.ui.utils.AndroidUtil
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.ui.utils.showFloatingSnackBar
import com.samourai.sentinel.ui.views.alertWithInput
import com.samourai.sentinel.ui.views.confirm
import com.samourai.sentinel.util.apiScope
import kotlinx.android.synthetic.main.activity_collection_edit.*
import kotlinx.coroutines.*
import org.koin.java.KoinJavaComponent.inject

class CollectionEditActivity : SentinelActivity() {

    private var needCollectionRefresh = false
    private val repository: CollectionRepository by inject(CollectionRepository::class.java)
    private val transactionsRepository: TransactionsRepository by inject(TransactionsRepository::class.java)
    private val prefs: PrefsUtil by inject(PrefsUtil::class.java)
    private val viewModel: CollectionEditViewModel by viewModels()
    private val pubKeyAdapter: PubKeyAdapter = PubKeyAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection_edit)
        setSupportActionBar(toolbarCollectionDetails)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        checkIntent()

        setUpPubKeyList()

        viewModel.getCollection().observe(this, {
            collectionEdiText.setText(it.collectionLabel)
            collectionEdiText.doOnTextChanged { text, _, _, _ -> it.collectionLabel = text.toString() }
        })
        viewModel.getPubKeys().observe(this, Observer {
            pubKeyAdapter.update(it)
        })

        viewModel.message.observe(this, Observer {
            if (it != null) {
                if (it.isEmpty()) {
                    return@Observer
                }
                AndroidUtil.hideKeyboard(this)
                this@CollectionEditActivity.showFloatingSnackBar(collectionEdiText.parent as ViewGroup,
                        text = "Success", duration = Snackbar.LENGTH_LONG)
            }
        })

        addNewPublicKey.setOnClickListener {
            if (!AndroidUtil.isPermissionGranted(Manifest.permission.CAMERA, applicationContext)) {
                this.askCameraPermission()
            } else {
                showPubKeyBottomSheet()
            }
        }

    }

    private fun checkIntent() {
        if (intent == null) {
            finish()
            return
        }
        if (intent.extras != null && intent.extras!!.containsKey("collection")) {
            val model = intent.extras?.getString("collection")?.let { repository.findById(it) }
            if (model != null) {
                viewModel.setCollection(model)
                // Check if any new Public key is passed through intent
                // we will set last item as editable so the new Public key  will be shown in edit layout
                if (intent.extras!!.containsKey("pubKey")) {
                    val newPubKey = intent.extras!!.getParcelable<PubKeyModel>("pubKey")

                    if (newPubKey?.pubKey?.let { model.getPubKey(it) } != null) {
                        this@CollectionEditActivity.showFloatingSnackBar(collectionDetailsRootLayout,
                                text = "Public key already exists in this collection",
                                duration = Snackbar.LENGTH_LONG)
                        return
                    }
                    viewModel.setPubKeys(model.pubs.apply { add(newPubKey!!) })
                    importWalletIfSegwit(newPubKey)
                } else {
                    viewModel.setPubKeys(model.pubs)
                    if(model.pubs.isNotEmpty()){
                        pubKeyAdapter.setEditingPubKey(model.pubs[model.pubs.lastIndex].pubKey)
                    }
                }
                if (prefs.haptics!!) {
                    addNewPublicKey.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
        } else {
            val collectionModel = PubKeyCollection()
            collectionModel.balance = 0
            collectionModel.collectionLabel = collectionEdiText.text.toString()
            viewModel.setCollection(collectionModel)
            if (intent.extras!!.containsKey("pubKey")) {
                val newPubKey = intent.extras!!.getParcelable<PubKeyModel>("pubKey")
                needCollectionRefresh = true
                viewModel.setPubKeys(arrayListOf(newPubKey!!))
                importWalletIfSegwit(newPubKey)
            }
        }

    }

    private fun importWalletIfSegwit(newPubKey: PubKeyModel?) {
        if (newPubKey != null) {
            if (newPubKey.type == AddressTypes.BIP84 || newPubKey.type == AddressTypes.BIP49) {
                val serviceIntent = Intent(this, ImportSegWitService::class.java)
                serviceIntent.putExtra("segWit", newPubKey.type!!.name)
                serviceIntent.putExtra("pubKey", newPubKey.pubKey)
                ContextCompat.startForegroundService(this, serviceIntent)
            }
        }
    }

    private fun setUpPubKeyList() {

        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL

        pubKeyRecyclerView.itemAnimator = PubKeyListItemAnimator(400)
        pubKeyRecyclerView.apply {
            layoutManager = linearLayoutManager
            adapter = pubKeyAdapter

        }
        addNewPublicKey.setOnClickListener {
            val bottomSheetFragment = AddNewPubKeyBottomSheet()
            if (viewModel.getCollection().value != null)
                bottomSheetFragment.setSelectedCollection(viewModel.getCollection().value!!)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

        pubKeyAdapter.setOnDeleteListener { index ->
            this.confirm(label = "Confirm",
                    message = "Are you sure want to remove this public key ?",
                    positiveText = "Yes",
                    negativeText = "No",
                    onConfirm = { confirmed ->
                        if (confirmed) {
                            val collection = viewModel.getCollection().value ?: return@confirm
                            apiScope.launch(context = Dispatchers.IO) {
                                transactionsRepository.fetchFromLocal(collection.id)
                                transactionsRepository.removeTxsRelatedToPubKey(collection.pubs[index], collection.id)
                                needCollectionRefresh = true
                                withContext(Dispatchers.Main) {
                                    setResult(Activity.RESULT_OK)
                                }
                                viewModel.removePubKey(index)
                            }
                            try {
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
            )

        }

        pubKeyAdapter.setOnEditClickListener { i, pubKeyModel ->
            if (viewModel.getCollection().value?.collectionLabel.isNullOrEmpty()) {
                this@CollectionEditActivity.showFloatingSnackBar(collectionDetailsRootLayout,
                        text = "Please enter a collection label first",
                        duration = Snackbar.LENGTH_LONG)
            } else {
                this.alertWithInput(label = "Edit label", onConfirm = {
                    pubKeyModel.label = it
                    viewModel.updateKey(i, pubKeyModel)
                    pubKeyAdapter.notifyItemChanged(i)
                },maxLen = 30, labelEditText = "Label", value = pubKeyModel.label, buttonLabel = "Save")
            }
        }
    }

    override fun onDestroy() {
        if (needCollectionRefresh) {
            viewModel.getCollection().value?.id?.let {
                apiScope.launch {
                    transactionsRepository.fetchFromServer(it)
                }
            }
        }
        super.onDestroy()
    }


    private fun showPubKeyBottomSheet() {
        val bottomSheetFragment = AddNewPubKeyBottomSheet()
        bottomSheetFragment.setPubKeyListener {
            if (it != null) {
                val items: ArrayList<PubKeyModel> = arrayListOf()
                if (viewModel.getCollection().value?.getPubKey(it.pubKey) != null) {
                    this@CollectionEditActivity.showFloatingSnackBar(collectionDetailsRootLayout,
                            text = "Public key already exists in this collection", duration = Snackbar.LENGTH_LONG)
                } else {
                    //Add all existing public keys
                    viewModel.getPubKeys().value?.let { it1 -> items.addAll(it1) }
                    items.add(it)
                    needCollectionRefresh = true
                    pubKeyAdapter.setEditingPubKey(it.pubKey)
                    viewModel.setPubKeys(items)
                }

            }
        }
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.saveCollectionMenuItem) {
            if (collectionEdiText.text.isNullOrEmpty() || collectionEdiText.text.isBlank()) {
                this@CollectionEditActivity.showFloatingSnackBar(collectionEdiText.parent as ViewGroup,
                        text = "Please enter collection label",
                        duration = Snackbar.LENGTH_SHORT)
            } else {
                viewModel.save()
                this.finish()
            }
        }
        if (item.itemId == R.id.deleteCollection) {
            deleteCollection()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteCollection() {

        this.confirm(label = "Confirm",
                message = "Are you sure want to delete this Collection ?",
                positiveText = "Yes",
                negativeText = "No",
                onConfirm = { confirmed ->
                    if (confirmed) {
                        val collection = viewModel.getCollection().value ?: return@confirm
                        CoroutineScope(Dispatchers.Default).launch {
                            val job = async { viewModel.removeCollection(collection.id) }
                            job.await()
                            withContext(Dispatchers.Main) {
                                job.invokeOnCompletion {
                                    if (it == null) {
                                        Toast.makeText(applicationContext, "Collection removed", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this@CollectionEditActivity, HomeActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        })
                                        finish()
                                    } else {
                                        it.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                }
        )

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.collection_details_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}