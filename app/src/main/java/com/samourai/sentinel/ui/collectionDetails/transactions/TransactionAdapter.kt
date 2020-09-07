package com.samourai.sentinel.ui.collectionDetails.transactions

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samourai.sentinel.R
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.Tx
import com.samourai.sentinel.data.TxRvModel
import kotlinx.coroutines.*
import org.bitcoinj.core.Coin
import org.koin.java.KoinJavaComponent.inject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * sentinel-android
 *
 * @author Sarath
 */
class TransactionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {
    private val MAX_CONFIRM_COUNT = 3
    private val VIEW_ITEM = 1
    private val VIEW_SECTION = 0
    private var txList: ArrayList<Tx> = arrayListOf()
    private val appContext: Context by inject(Context::class.java)
    private var onClickListener: (Tx) -> Unit = {};
    private lateinit var collection: PubKeyCollection;
    private val diffCallBack = object : DiffUtil.ItemCallback<TxRvModel>() {

        override fun areItemsTheSame(oldItem: TxRvModel, newItem: TxRvModel): Boolean {
            if (oldItem.section != null) {
                return oldItem.section == newItem.section
            }
            return oldItem.tx.hash == newItem.tx.hash
        }

        override fun areContentsTheSame(oldItem: TxRvModel, newItem: TxRvModel): Boolean {
            return oldItem == newItem
        }

    }
    private val mDiffer: AsyncListDiffer<TxRvModel> = AsyncListDiffer(this, diffCallBack)

    init {
//        this.setHasStableIds(true)
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txAmount: TextView = view.findViewById(R.id.tvAmount)
        val directionImageView: ImageView = view.findViewById(R.id.transactionDirection)
        val associatedKey: TextView = view.findViewById(R.id.associatedPubKey)
        val txTime: TextView = view.findViewById(R.id.tx_time)
    }

    inner class ViewHolderSection(view: View) : RecyclerView.ViewHolder(view) {
        val section: TextView = view.findViewById(R.id.section_title);

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_SECTION) {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_tx_section, parent, false);
            ViewHolderSection(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_tx, parent, false);

            ViewHolder(view)
        }
    }

    override fun getItemCount(): Int = mDiffer.currentList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val txRvModel = mDiffer.currentList[position]
        if (holder is ViewHolderSection) {
            holder.section.text = txRvModel.section
            if (txRvModel.section == "pending") {
                holder.section.setTextColor(ContextCompat.getColor(appContext, R.color.md_amber_400))
            }
        }
        if (holder is ViewHolder) {
            holder.itemView.setOnClickListener {
                onClickListener.invoke(txRvModel.tx)
            }
            holder.txAmount.text = txRvModel.tx.result?.let { getBTCDisplayAmount(it) }
            val associatedKey = this.collection.getPubKey(txRvModel.tx.associatedPubKey)
            if (associatedKey != null) {
                holder.associatedKey.text = associatedKey.label
            }
            if (txRvModel.tx.result != null)
                if (txRvModel.tx.result > 0) {
                    holder.directionImageView.setImageDrawable(appContext.getDrawable(R.drawable.ic_baseline_incoming_arrow));
                } else {
                    holder.directionImageView.setImageDrawable(appContext.getDrawable(R.drawable.ic_baseline_outgoing_arrow));
                }
        }
    }

    fun setOnclickListener(callback: (Tx) -> Unit = {}) {
        onClickListener = callback
    }

    override fun getItemViewType(position: Int): Int {
        return if (mDiffer.currentList[position].section != null) {
            VIEW_SECTION
        } else {
            VIEW_ITEM
        }
    }

    fun updateTx(list: ArrayList<Tx>) {
        txList = list
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(filterItem: CharSequence?): FilterResults {

                val filtered: ArrayList<Tx>
                val filterString = filterItem.toString()

                filtered = if (filterString.isEmpty()) {
                    txList
                } else {
                    ArrayList(txList.filter { it.isBelongsToPubKey(filterString.toLowerCase()) })
                }
                val filterResults = FilterResults()
                filterResults.values = filtered
                return filterResults
            }

            override fun publishResults(filterItem: CharSequence?, results: FilterResults?) {
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        val items = makeSections(results?.values as List<Tx>)
                        withContext(Dispatchers.Main) {
                            mDiffer.submitList(items)
                        }
                    } catch (e: Exception) {
                        throw CancellationException(e.message)
                    }
                }
            }
        }
    }

    fun makeSections(txs: List<Tx>): MutableList<TxRvModel> {

        val mutableTxList = txs.toMutableList()

        mutableTxList.sortWith { t1, t2 -> t1.time.compareTo(t2.time) }
        mutableTxList.distinctBy { it.hash }

        val sectionDates = ArrayList<Long>()
        val sectioned: MutableList<TxRvModel> = java.util.ArrayList()
        // for pending state
        var contains_pending = false
        //if there is only pending tx today we don't want to add today's section
        var show_todays_tx = false

        repeat(mutableTxList.size) { i ->
            val tx: Tx = mutableTxList[i]
            if (tx.confirmations <= MAX_CONFIRM_COUNT) {
                contains_pending = true
            }
            if (tx.confirmations >= MAX_CONFIRM_COUNT && DateUtils.isToday(tx.time * 1000)) {
                show_todays_tx = true
            }
        }

        for (tx in mutableTxList) {
            val date = Date()
            date.time = tx.time * 1000
            val calendarDM = Calendar.getInstance()
            calendarDM.timeZone = TimeZone.getDefault()
            calendarDM.time = date
            calendarDM[Calendar.HOUR_OF_DAY] = 0
            calendarDM[Calendar.MINUTE] = 0
            calendarDM[Calendar.SECOND] = 0
            calendarDM[Calendar.MILLISECOND] = 0
            if (!sectionDates.contains(calendarDM.time.time)) {
                if (DateUtils.isToday(calendarDM.time.time)) {
                    if (show_todays_tx) sectionDates.add(calendarDM.time.time)
                } else {
                    sectionDates.add(calendarDM.time.time)
                }
            }
        }

        sectionDates.sortWith { first: Long?, second: Long? -> second?.let { first?.compareTo(it) }!! }


        if (contains_pending) sectionDates.add(-1L)

        for (key in sectionDates) {
            val section = TxRvModel(section = null, time = 0)
            if (key != -1L) {
                val fmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                fmt.timeZone = TimeZone.getDefault()
                section.section = fmt.format(key)
            } else {
                section.section = "pending"
            }
            section.time = key
            for (tx in mutableTxList) {
                val date = Date()
                date.time = tx.time * 1000
                val fmt = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
                fmt.timeZone = TimeZone.getDefault()
                if (key == -1L) {
                    if (tx.confirmations < MAX_CONFIRM_COUNT) {
                        sectioned.add(TxRvModel(tx = tx, time = 0, section = null))
                    }
                } else if (fmt.format(key) == fmt.format(date)) {
                    if (tx.confirmations >= MAX_CONFIRM_COUNT) {
                        sectioned.add(TxRvModel(tx = tx, time = 0, section = null))
                    }
                }
            }
            sectioned.add(section)
        }

        sectioned.reverse()

        val list = ArrayList<TxRvModel>();

        sectioned.forEach {
            val index = sectioned.indexOf(it)
            if (it.section != null) {
                if (index + 1 < sectioned.lastIndex) {
                    if (sectioned[index + 1].section == null) {
                        list.add(it)
                    }
                }
            } else {
                list.add(it)
            }
        }

        return list
    }

    fun setCollection(collection: PubKeyCollection) {
        this.collection = collection
    }


    private fun getBTCDisplayAmount(value: Long): String? {
        return Coin.valueOf(value).toPlainString()
    }
}