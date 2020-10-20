package com.samourai.sentinel.ui.collectionDetails.transactions

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samourai.sentinel.R
import com.samourai.sentinel.data.Tx
import org.apache.commons.lang3.time.DateUtils.isSameDay
import org.bitcoinj.core.Coin
import org.koin.java.KoinJavaComponent.inject
import java.text.SimpleDateFormat
import java.util.*

/**
 * sentinel-android
 *
 * @author Sarath
 */
class TransactionAdapter : PagedListAdapter<Tx, TransactionAdapter.ViewHolder>(DIFF) {

    private val appContext: Context by inject(Context::class.java)
    private var onClickListener: (Tx) -> Unit = {};
    private val simpleDateFormat = SimpleDateFormat("H:mm", Locale.getDefault())
    private val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    init {
        this.setHasStableIds(false)
        simpleDateFormat.timeZone = TimeZone.getDefault()
        fmt.timeZone = TimeZone.getDefault()
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txAmount: TextView = view.findViewById(R.id.tvAmount)
        val directionImageView: ImageView = view.findViewById(R.id.transactionDirection)
        val txTime: TextView = view.findViewById(R.id.tx_time)
        val txContainer: View = view.findViewById(R.id.txContainer)
        private val dividerView: View = view.findViewById(R.id.dividerView)
        val section: TextView = view.findViewById(R.id.section_title)

        fun hideDivider() {
            dividerView.visibility = View.GONE
            section.visibility = View.GONE
        }

        fun showDivider() {
            dividerView.visibility = View.VISIBLE
            section.visibility = View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tx, parent, false);
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItem(position) == null) {
            return
        }
        val tx = getItem(position)!!
        holder.txContainer.setOnClickListener {
            onClickListener.invoke(tx)
        }
        holder.hideDivider()
        holder.txAmount.text = tx.result?.let { getBTCDisplayAmount(it) }
        if (tx.result != null)
            if (tx.result > 0) {
                holder.directionImageView.setImageDrawable(appContext.getDrawable(R.drawable.ic_baseline_incoming_arrow));
                holder.txAmount.setTextColor(ContextCompat.getColor(appContext, R.color.md_green_A400))
            } else {
                holder.directionImageView.setImageDrawable(appContext.getDrawable(R.drawable.ic_baseline_outgoing_arrow));
            }
        val current = Date().apply { time = tx.time * 1000 }

        holder.txTime.text = simpleDateFormat.format(current)

        if (position != 0) {
            val previous = getItem(position - 1)!!
            val datePrev = Date().apply { time = previous.time * 1000 }
            if (isSameDay(datePrev, current)) {
                //No-OP
            } else {
                holder.showDivider()
                if (DateUtils.isToday(tx.time * 1000)) {
                    holder.section.text = "Today"
                } else {
                    holder.section.text = fmt.format(current)
                }
            }
        } else {
            holder.showDivider()
            if (tx.confirmations <= MAX_CONFIRM_COUNT) {
                holder.section.text = "Pending"
                holder.section.setTextColor(ContextCompat.getColor(appContext, R.color.md_amber_400))
            } else if (tx.confirmations >= MAX_CONFIRM_COUNT) {
                if (DateUtils.isToday(tx.time * 1000)) {
                    holder.section.text = "Today"
                } else {
                    if (DateUtils.isToday(tx.time * 1000)) {
                        holder.section.text = "Today"
                    } else {
                        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        holder.section.text = fmt.format(current)
                    }
                }
            }
        }
    }

    fun setOnclickListener(callback: (Tx) -> Unit = {}) {
        onClickListener = callback
    }

    private fun getBTCDisplayAmount(value: Long): String? {
        return Coin.valueOf(value).toPlainString()
    }

    companion object {
        private const val MAX_CONFIRM_COUNT = 3
        val DIFF = object : DiffUtil.ItemCallback<Tx>() {
            override fun areItemsTheSame(oldItem: Tx, newItem: Tx): Boolean {
                return oldItem.hash == newItem.hash
            }

            override fun areContentsTheSame(oldItem: Tx, newItem: Tx): Boolean {
                return oldItem == newItem
            }
        }

    }

}