package com.samourai.sentinel.ui.collectionDetails.transactions

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.samourai.sentinel.R
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.repository.TransactionsRepository
import com.samourai.sentinel.ui.SentinelActivity
import com.samourai.sentinel.ui.collectionEdit.CollectionEditActivity
import com.samourai.sentinel.ui.fragments.TransactionsDetailsBottomSheet
import com.samourai.sentinel.ui.utils.RecyclerViewItemDividerDecorator
import com.samourai.sentinel.ui.utils.SlideInItemAnimator
import com.samourai.sentinel.ui.utils.showFloatingSnackBar
import com.samourai.sentinel.ui.utxos.UtxosActivity
import com.samourai.sentinel.util.MonetaryUtil
import kotlinx.android.synthetic.main.fragment_transactions.*
import org.bitcoinj.core.Coin
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber


class TransactionsFragment : Fragment() {

    private lateinit var fiatBalanceLiveData: LiveData<String>
    private lateinit var balanceLiveData: LiveData<Long>
    private val transactionsViewModel: TransactionsViewModel by viewModels()
    private lateinit var collection: PubKeyCollection;
    private val transactionsRepository: TransactionsRepository by KoinJavaComponent.inject(TransactionsRepository::class.java)
    private val transactionAdapter: TransactionAdapter = TransactionAdapter()
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

        transactionAdapter.setOnclickListener {
            val dojoConfigureBottomSheet = TransactionsDetailsBottomSheet(it)
            dojoConfigureBottomSheet.show(childFragmentManager, dojoConfigureBottomSheet.tag)
        }

        transactionsRecycler.layoutManager = LinearLayoutManager(context)
        transactionsRecycler.adapter = transactionAdapter
        val decorator = RecyclerViewItemDividerDecorator(ContextCompat.getDrawable(requireContext(), R.drawable.divider_tx)!!);
        transactionsRecycler.addItemDecoration(decorator)
        transactionsRecycler.itemAnimator = SlideInItemAnimator(slideFromEdge = Gravity.TOP)
        fabGoUp.hide()
        fabGoUp.setOnClickListener {
            transactionsNestedScrollView.post {
                transactionsNestedScrollView.fling(0)
                transactionsNestedScrollView.smoothScrollTo(0, 0)
             }

        }
        transactionsNestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > (requireActivity() as SentinelActivity).screenRectDp.height()) {
                fabGoUp.show()
            } else {
                fabGoUp.hide()
            }
        })
    }

    private fun initViewModel() {
        transactionsViewModel.setCollection(collection)
        transactionAdapter.setCollection(collection)

        transactionsViewModel.getTransactions().observe(this.viewLifecycleOwner, Observer {
            transactionAdapter.updateTx(it)
            if (pubKeySelector.listSelection > 0) {
                setAdapterFilter(pubKeySelector.listSelection)
            } else {
                setAdapterFilter(0)
            }
        })

        balanceLiveData.observe(viewLifecycleOwner, {
            collectionBalanceBtc.text = "${getBTCDisplayAmount(it)} BTC"
        })
        fiatBalanceLiveData.observe(viewLifecycleOwner, {
            if (isAdded) {
                collectionBalanceFiat.text = it
            }
        })
        transactionsViewModel.getLoadingState().observe(this.viewLifecycleOwner, {
            transactionsSwipeContainer.isRefreshing = it
        })
        transactionsViewModel.getMessage().observe(
                this.viewLifecycleOwner,
                {
                    if (it != "null")
                        (requireActivity() as AppCompatActivity)
                                .showFloatingSnackBar(transactionsSwipeContainer, "Error : $it")
                }
        )

        transactionsSwipeContainer.setOnRefreshListener {
            transactionsViewModel.fetch()
        }


        setUpSpinner()
    }

    private fun setUpSpinner() {
        val items = collection.pubs.map { it.label }.toMutableList()
        items.add(0, "All")
        val adapter: ArrayAdapter<String> = ArrayAdapter(requireContext(),
                R.layout.dropdown_menu_popup_item, items)
        pubKeySelector.inputType = InputType.TYPE_NULL
        pubKeySelector.setAdapter(adapter)
        pubKeySelector.setText("All", false)
        pubKeySelector.onItemClickListener = AdapterView.OnItemClickListener { _, _, index, _ ->
            setAdapterFilter(index)
        }
    }


    /**
     * Filter string is a pubkey or word "All"
     * when the user sets a new filter adapter will filter out the dataset
     * and shows in the list
     */
    private fun setAdapterFilter(index: Int) {
        if (index == 0) {
            transactionAdapter.filter.filter("")
        } else {
            transactionAdapter.filter.filter(collection.pubs[index - 1].pubKey)
        }
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
            setUpSpinner()
            setUpToolBar()
            transactionsRecycler.adapter?.notifyDataSetChanged()
        }
    }

    override fun onDetach() {
        transactionsRepository.clear()
        super.onDetach()
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


}