package com.samourai.sentinel.ui.collectionDetails.transactions

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.samourai.sentinel.R
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.data.db.dao.TxDao
import com.samourai.sentinel.ui.SentinelActivity
import com.samourai.sentinel.ui.collectionEdit.CollectionEditActivity
import com.samourai.sentinel.ui.fragments.TransactionsDetailsBottomSheet
import com.samourai.sentinel.ui.utils.RecyclerViewItemDividerDecorator
import com.samourai.sentinel.ui.utils.showFloatingSnackBar
import com.samourai.sentinel.ui.utxos.UtxosActivity
import com.samourai.sentinel.util.MonetaryUtil
import kotlinx.android.synthetic.main.fragment_transactions.*
import org.bitcoinj.core.Coin
import org.koin.java.KoinJavaComponent.inject


class TransactionsFragment : Fragment() {

    private lateinit var fiatBalanceLiveData: LiveData<String>
    private lateinit var balanceLiveData: LiveData<Long>
    private val transactionsViewModel: TransactionsViewModel by viewModels()
    private lateinit var collection: PubKeyCollection;
    private val monetaryUtil: MonetaryUtil by inject(MonetaryUtil::class.java)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()

        setUpToolBar()

    }

    private fun initViewModel() {
        transactionsViewModel.setCollection(collection)

        txViewPager.adapter = CollectionPubKeysViewpager(this.activity, collection)
        txViewPager.offscreenPageLimit = 5
        TabLayoutMediator(tabLayout, txViewPager) { tab, position ->
            tab.text = collection.pubs[position].label
        }.attach()

        balanceLiveData.observe(viewLifecycleOwner, {
            collectionBalanceBtc.text = "${getBTCDisplayAmount(it)} BTC"
        })
        fiatBalanceLiveData.observe(viewLifecycleOwner, {
            if (isAdded) {
                collectionBalanceFiat.text = it
            }
        })
        transactionsViewModel.getMessage().observe(
                this.viewLifecycleOwner,
                {
                    if (it != "null")
                        (requireActivity() as AppCompatActivity)
                                .showFloatingSnackBar(collectionBalanceBtc, "Error : $it")
                }
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun setUpToolBar() {
        (activity as SentinelActivity).setSupportActionBar(toolbarCollectionDetails)
        (activity as SentinelActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbarCollectionDetails.title = collection.collectionLabel
        collectionBalanceBtc.text = monetaryUtil.formatToBtc(collection.balance)
    }

    fun initViewModel(collection: PubKeyCollection) {
        // check if the new instance added / removed pub keys
        // if the size changed we need to fetch transactions
        this.collection = collection
        if (isAdded) {
            initViewModel()
            setUpToolBar()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.collection_detail_transaction_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.collection_details_transaction_edit_option) {
            startActivityForResult(Intent(context, CollectionEditActivity::class.java).apply {
                putExtra("collection", collection.id)
            }, EDIT_REQUEST_ID)
        }
        if (item.itemId == R.id.collection_details_transaction_utxos) {
            startActivityForResult(Intent(context, UtxosActivity::class.java).apply {
                putExtra("collection", collection.id)
            }, EDIT_REQUEST_ID)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun getBTCDisplayAmount(value: Long): String? {
        return Coin.valueOf(value).toPlainString()
    }

    fun setBalance(balance: LiveData<Long>) {
        this.balanceLiveData = balance
    }

    fun setBalanceFiat(fiatBalance: LiveData<String>) {
        this.fiatBalanceLiveData = fiatBalance

    }

    companion object {
        const val EDIT_REQUEST_ID = 11;
    }

    private class CollectionPubKeysViewpager(
            fa: FragmentActivity?,
            private val collection: PubKeyCollection,

            ) : FragmentStateAdapter(fa!!) {
        override fun createFragment(position: Int): Fragment {
            return TransactionsListFragment(position, collection)
        }

        override fun getItemCount(): Int {
            return collection.pubs.size
        }
    }


}

