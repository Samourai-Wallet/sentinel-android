package com.samourai.sentinel.ui.collectionDetails

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.samourai.sentinel.R
import com.samourai.sentinel.data.CollectionModel
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.ui.SentinelActivity
import com.samourai.sentinel.ui.utils.AndroidUtil
import kotlinx.android.synthetic.main.activity_collection_details.*
import kotlinx.android.synthetic.main.content_choose_address_type.*
import org.koin.java.KoinJavaComponent

class CollectionDetails : SentinelActivity() {

    private val repository: CollectionRepository by KoinJavaComponent.inject(CollectionRepository::class.java)
    private val viewModel: CollectionDetailViewModel by viewModels()
    private val pubKeyAdapter: PubKeyAdapter = PubKeyAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection_details)
        setSupportActionBar(toolbarCollectionDetails)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        checkIntent()
        setUpPubKeyList()

        viewModel.getCollection().observe(this, Observer {
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
                val snack = Snackbar.make(collectionDetailsRootLayout, it, Snackbar.LENGTH_LONG)
                snack.show()
            }
        })

    }

    private fun checkIntent() {
        if (intent == null) {
            finish()
            return
        }
        if (intent.extras != null && intent.extras.containsKey("collection")) {
            val model = repository.collections.find { it.id == intent.extras?.getString("collection") }
            if (model != null) {
                viewModel.setCollection(model)
                // Check if any new Public key is passed through intent
                // we will set last item as editable so the new Public key  will be shown in edit layout
                if (intent.extras!!.containsKey("pubKey")) {
                    val newPubKey = intent.extras!!.getParcelable<PubKeyModel>("pubKey")
                    viewModel.setPubKeys(model.pubs.apply { add(newPubKey!!) })
                } else {
                    viewModel.setPubKeys(model.pubs)
                }
            }
        } else {
            val collectionModel = CollectionModel()
            collectionModel.balance = 0
            collectionModel.collectionLabel = collectionEdiText.text.toString()
            viewModel.setCollection(collectionModel)
            if (intent.extras!!.containsKey("pubKey")) {
                val newPubKey = intent.extras!!.getParcelable<PubKeyModel>("pubKey")
                viewModel.setPubKeys(arrayListOf(newPubKey!!))
            }
        }

    }

    private fun setUpPubKeyList() {
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        pubKeyRecyclerView.apply {
            layoutManager = linearLayoutManager
            adapter = pubKeyAdapter
        }

        pubKeyAdapter.setOnDeleteListener {
            MaterialAlertDialogBuilder(this)
                    .setTitle(resources.getString(R.string.app_title))
                    .setMessage("Are you sure want to remove this public key ? ")
                    .setBackgroundInsetTop(12)
                    .setNegativeButton(resources.getString(R.string.no)) { dialog, which ->

                    }
                    .setPositiveButton(resources.getString(R.string.yes)) { dialog, which ->
                        viewModel.removePubKey(it)
                    }
                    .show()
        }

        pubKeyAdapter.setOnLabelUpdateListener { i, pubKeyModel ->
            viewModel.updateKey(i, pubKeyModel)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.saveCollectionMenuItem) {
            viewModel.save()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.collection_details_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}