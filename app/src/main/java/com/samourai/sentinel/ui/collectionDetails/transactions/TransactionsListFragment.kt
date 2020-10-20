package com.samourai.sentinel.ui.collectionDetails.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samourai.sentinel.R
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.data.db.dao.TxDao
import com.samourai.sentinel.ui.fragments.TransactionsDetailsBottomSheet
import com.samourai.sentinel.ui.utils.RecyclerViewItemDividerDecorator
import org.koin.java.KoinJavaComponent

/**
 * sentinel-android
 */

class TransactionsListFragment(
        private val position: Int,
        private val collection: PubKeyCollection,
) : Fragment() {

    private val transactionAdapter: TransactionAdapter = TransactionAdapter()

    class TransactionsViewModel(val pubKeyCollection: PubKeyCollection, val position: Int) : ViewModel() {
        private val txDao: TxDao by KoinJavaComponent.inject(TxDao::class.java)
        val txLiveData: LiveData<PagedList<Tx>>

        init {
            txLiveData = LivePagedListBuilder(
                    txDao.getPaginatedTx(pubKeyCollection.id, pubKeyCollection.pubs[position].pubKey), 12).build()
        }

        class TransactionsViewModelFactory(private val pubKeyCollection: PubKeyCollection, private val position: Int) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
                    return TransactionsViewModel(pubKeyCollection, position) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        companion object {
            fun getFactory(pubKeyCollection: PubKeyCollection, position: Int): TransactionsViewModelFactory {
                return TransactionsViewModelFactory(pubKeyCollection, position)
            }
        }
    }


    private val transactionViewModel: TransactionsViewModel by viewModels(factoryProducer = { TransactionsViewModel.getFactory(collection, position) })


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions_segement, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val transactionsRecycler = view.findViewById<RecyclerView>(R.id.transactionsRecycler)

        transactionsRecycler.layoutManager = LinearLayoutManager(context)
        transactionsRecycler.adapter = transactionAdapter

        val decorator = RecyclerViewItemDividerDecorator(ContextCompat.getDrawable(requireContext(), R.drawable.divider_tx)!!)
        transactionsRecycler.addItemDecoration(decorator)
        transactionAdapter.setOnclickListener {
            val dojoConfigureBottomSheet = TransactionsDetailsBottomSheet(it)
            dojoConfigureBottomSheet.show(childFragmentManager, dojoConfigureBottomSheet.tag)
        }

        transactionViewModel.txLiveData.observe(this.viewLifecycleOwner, {
            transactionAdapter.submitList(it)
        })
    }

}