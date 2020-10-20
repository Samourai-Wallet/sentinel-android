package com.samourai.sentinel.ui.collectionDetails

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.samourai.sentinel.R
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.ui.SentinelActivity
import com.samourai.sentinel.ui.collectionDetails.receive.ReceiveFragment
import com.samourai.sentinel.ui.collectionDetails.send.SendFragment
import com.samourai.sentinel.ui.collectionDetails.send.SpendFragmentContainer
import com.samourai.sentinel.ui.collectionDetails.transactions.TransactionsFragment
import kotlinx.android.synthetic.main.activity_collection_details.*
import org.koin.java.KoinJavaComponent.inject


class CollectionDetailsActivity : SentinelActivity() {


    private lateinit var pagerAdapter: PagerAdapter;
    private val receiveFragment: ReceiveFragment = ReceiveFragment()
    private val sendFragment: SpendFragmentContainer = SpendFragmentContainer()
    private val transactionsFragment: TransactionsFragment = TransactionsFragment()
    private var collection: PubKeyCollection? = null
    private val repository: CollectionRepository by inject(CollectionRepository::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection_details)
        pagerAdapter = PagerAdapter(this)
        fragmentHostContainerPager.adapter = pagerAdapter
        fragmentHostContainerPager.isUserInputEnabled = false

        checkIntent()
        val receiveViewModel: CollectionDetailsViewModel by viewModels(factoryProducer = { CollectionDetailsViewModel.getFactory(collection!!) })
        linearLayout.setPadding(0, 0, 0, getNavHeight().toInt())
        receiveViewModel.getCollections().observe(this, Observer {
            intent.extras?.getString("collection")?.let { it1 ->
                receiveViewModel.getRepository().findById(it1)?.let {
                    collection = it
                }
            }
            if (collection != null) {
                receiveFragment.setCollection(collection!!)
                sendFragment.setCollection(collection!!)
                transactionsFragment.initViewModel(collection!!)
            } else {
                finish()
            }
        })

        receiveFragment.setBalance(receiveViewModel.getBalance())
        transactionsFragment.setBalance(receiveViewModel.getBalance())

        receiveViewModel.getFiatBalance().observe(this, Observer {
            receiveFragment.setBalanceFiat(receiveViewModel.getFiatBalance())
            transactionsFragment.setBalanceFiat(receiveViewModel.getFiatBalance())
        })

        bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_nav_receive -> {
                    fragmentHostContainerPager.setCurrentItem(0, true)
                }
                R.id.bottom_nav_send -> {
                    fragmentHostContainerPager.setCurrentItem(2, true)
                }
                R.id.bottom_nav_transaction -> {
                    fragmentHostContainerPager.setCurrentItem(1, true)
                }
            }
            true
        }

        fragmentHostContainerPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                invalidateOptionsMenu()
                when (position) {
                    0 -> {
                        bottomNav.selectedItemId = R.id.bottom_nav_receive
                    }
                    1 -> {
                        bottomNav.selectedItemId = R.id.bottom_nav_transaction
                    }
                    else -> {
                        bottomNav.selectedItemId = R.id.bottom_nav_send
                    }
                }
            }

        })

        fragmentHostContainerPager.visibility = View.INVISIBLE
        Handler().postDelayed({
            fragmentHostContainerPager.setCurrentItem(1, false)
            fragmentHostContainerPager.visibility = View.VISIBLE
        }, 1)
    }


    private fun checkIntent() {
        if (intent.extras != null) {
            if (intent.extras!!.containsKey("collection")) {
                repository.findById(intent.extras!!.getString("collection")!!)?.let {
                    collection = it
                }
                if (collection != null) {
                    receiveFragment.setCollection(collection!!)
                    sendFragment.setCollection(collection!!)
                    transactionsFragment.initViewModel(collection!!)
                } else {
                    finish()
                }
            } else {
                finish()
                return
            }
        } else {
            finish()
            return
        }

    }

    private inner class PagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    receiveFragment
                }
                1 -> {
                    transactionsFragment
                }
                else -> {
                    sendFragment
                }
            }
        }
    }


}

