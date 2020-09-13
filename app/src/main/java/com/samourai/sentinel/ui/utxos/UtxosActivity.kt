package com.samourai.sentinel.ui.utxos

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.samourai.sentinel.R
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.ui.SentinelActivity
import kotlinx.android.synthetic.main.activity_utxos.*
import org.koin.java.KoinJavaComponent.inject

class UtxosActivity : SentinelActivity() {

    private val repository: CollectionRepository by inject(CollectionRepository::class.java)
    private var collection: PubKeyCollection? = null
    private var pubKeys: ArrayList<PubKeyModel> = arrayListOf()
    private val utxoFragments: MutableMap<String, UTXOFragment> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_utxos)
        setSupportActionBar(findViewById(R.id.toolbar))
        checkIntent()
        setUpToolbar()

        val utxoViewModel: UtxoActivityViewModel by viewModels(factoryProducer = { UtxoActivityViewModel.getFactory(collection!!) })
        setUpPager(utxoViewModel)

        utxoViewModel.getPubKeys().observe(this, {
            pubKeys.clear()
            pubKeys.addAll(it)
            pager.adapter?.notifyDataSetChanged()
            listenChanges(utxoViewModel)
        })

    }

    private fun listenChanges(utxoViewModel: UtxoActivityViewModel) {
        pubKeys.forEach { pubKeyModel ->
            utxoViewModel.getUtxo(pubKeyModel.pubKey).observe(this@UtxosActivity, {
                utxoFragments[pubKeyModel.pubKey]?.setUtxos(it)
            })
        }

    }


    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        toolbar.title = "Unspent outputs"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun checkIntent() {
        if (intent == null) {
            finish()
            return
        }
        if (intent.extras != null && intent.extras!!.containsKey("collection")) {
            val model = intent.extras?.getString("collection")?.let { repository.findById(it) }
            if (model != null) {
                collection = model
            } else {
                finish()
            }
        }

    }

    private fun setUpPager(utxoViewModel: UtxoActivityViewModel) {
        tabLayout.setupWithViewPager(pager)
        pager.adapter = object : FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getCount(): Int {
                return pubKeys.size;
            }

            override fun getItem(position: Int): Fragment {
                if (!utxoFragments.containsKey(pubKeys[position].pubKey)) {
                    utxoFragments[pubKeys[position].pubKey] = UTXOFragment()
                }
                return utxoFragments[pubKeys[position].pubKey]!!
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return pubKeys[position].label
            }
        }
        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                utxoViewModel.getUtxo(pubKeys[position].pubKey).observe(this@UtxosActivity, {
                    utxoFragments[pubKeys[position].pubKey]?.setUtxos(it)
                })
            }

            override fun onPageScrollStateChanged(state: Int) {
                utxoFragments.values.forEach {
                    it.clearSelection()
                }
            }

        })
    }



}
