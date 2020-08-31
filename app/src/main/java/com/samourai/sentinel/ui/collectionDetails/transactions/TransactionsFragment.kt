package com.samourai.sentinel.ui.collectionDetails.transactions

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.samourai.sentinel.R
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.repository.TransactionsRepository
import com.samourai.sentinel.ui.SentinelActivity
import com.samourai.sentinel.ui.collectionEdit.CollectionEditActivity
import com.samourai.sentinel.ui.utils.RecyclerViewItemDividerDecorator
import com.samourai.sentinel.ui.utils.SlideInItemAnimator
import com.samourai.sentinel.ui.utils.showFloatingSnackBar
import com.samourai.sentinel.util.MonetaryUtil
import org.koin.java.KoinJavaComponent


class TransactionsFragment : Fragment() {

    private val transactionsViewModel: TransactionsViewModel by viewModels()
    private lateinit var toolbar: Toolbar;
    private lateinit var collection: PubKeyCollection;
    private val transactionsRepository: TransactionsRepository by KoinJavaComponent.inject(TransactionsRepository::class.java)
    private val transactionAdapter: TransactionAdapter = TransactionAdapter()
    private lateinit var transactionRecyclerView: RecyclerView
    private lateinit var pubKeyDropDown: AutoCompleteTextView
    private lateinit var collectionBalanceBtc: TextView
    private lateinit var collectionBalanceFiat: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val monetaryUtil: MonetaryUtil by KoinJavaComponent.inject(MonetaryUtil::class.java)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_transactions, container, false)
        toolbar = root.findViewById(R.id.toolbarCollectionDetails)
        transactionRecyclerView = root.findViewById(R.id.transactionsRecycler)
        pubKeyDropDown = root.findViewById(R.id.pubKeySelector)
        collectionBalanceFiat = root.findViewById(R.id.collectionBalanceFiat)
        collectionBalanceBtc = root.findViewById(R.id.collectionBalanceBtc)
        swipeRefreshLayout = root.findViewById(R.id.transactionsSwipeContainer)
        collectionBalanceBtc = root.findViewById(R.id.collectionBalanceBtc)


        initViewModel()

        setUpToolBar()


        transactionRecyclerView.layoutManager = LinearLayoutManager(context)
        transactionRecyclerView.adapter = transactionAdapter
        val decorator = RecyclerViewItemDividerDecorator(ContextCompat.getDrawable(requireContext(), R.drawable.divider_tx)!!);
        transactionRecyclerView.addItemDecoration(decorator)
        transactionRecyclerView.itemAnimator = SlideInItemAnimator(slideFromEdge = Gravity.TOP)

        return root
    }

    private fun initViewModel() {
        transactionsViewModel.setCollection(collection)
        transactionAdapter.setCollection(collection)

        transactionsViewModel.getTransactions().observe(this.viewLifecycleOwner, Observer {
            transactionAdapter.updateTx(it)
            if (pubKeyDropDown.listSelection > 0) {
                setAdapterFilter(pubKeyDropDown.listSelection)
            } else {
                setAdapterFilter(0)
            }
        })

        transactionsViewModel.getBalance().observe(this.viewLifecycleOwner, Observer {
            if (it != null) {
                collectionBalanceBtc.text = it
            }
        })

        transactionsViewModel.getLoadingState().observe(this.viewLifecycleOwner, {
            swipeRefreshLayout.isRefreshing = it
        })
        transactionsViewModel.getMessage().observe(
                this.viewLifecycleOwner,
                {
                    if (it != "null")
                        (requireActivity() as AppCompatActivity)
                                .showFloatingSnackBar(swipeRefreshLayout, "Error : $it")
                }
        )

        swipeRefreshLayout.setOnRefreshListener {
            transactionsViewModel.fetch()
        }


        setUpSpinner()
    }

    private fun setUpSpinner() {
        val items = collection.pubs.map { it.label }.toMutableList()
        items.add(0, "All")
        val adapter: ArrayAdapter<String> = ArrayAdapter(requireContext(),
                R.layout.dropdown_menu_popup_item, items)
        pubKeyDropDown.inputType = InputType.TYPE_NULL
        pubKeyDropDown.setAdapter(adapter)
        pubKeyDropDown.setText("All", false)
        pubKeyDropDown.onItemClickListener = AdapterView.OnItemClickListener { _, _, index, _ ->
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
        (activity as SentinelActivity).setSupportActionBar(toolbar)
        (activity as SentinelActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.title = collection.collectionLabel
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
            transactionRecyclerView.adapter?.notifyDataSetChanged()
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

        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EDIT_REQUEST_ID = 11;
    }


}