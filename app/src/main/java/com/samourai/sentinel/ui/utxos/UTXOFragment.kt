package com.samourai.sentinel.ui.utxos;

import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samourai.sentinel.R
import com.samourai.sentinel.data.Utxo
import com.samourai.sentinel.ui.utils.SlideInItemAnimator
import com.samourai.sentinel.util.UtxoMetaUtil
import com.samourai.sentinel.util.ItemDividerDecorator
import com.samourai.sentinel.util.MonetaryUtil
import kotlinx.android.synthetic.main.content_utxos_fragment.*
import timber.log.Timber

class UTXOFragment : Fragment(), ActionMode.Callback {

    private val utxoAdapter: UTXOAdapter = UTXOAdapter()
    private val utxos: ArrayList<Utxo> = arrayListOf()
    private var actionMode: ActionMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_utxos_fragment, null, false)
    }


    override fun onStart() {
        UtxoMetaUtil.read()
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        utxoRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = utxoAdapter
            itemAnimator = SlideInItemAnimator()
            addItemDecoration(ItemDividerDecorator(ContextCompat.getDrawable(requireContext(), R.drawable.divider_tx)))
        }
        utxoAdapter.setLongClickListener {
            if (actionMode == null) {
                utxoAdapter.enableMultiSelect()
                actionMode = (activity as UtxosActivity).startSupportActionMode(this)
            }
        }
        utxoAdapter.setMultiClickListener { utxo ->
            val targetItem = utxos.find { it.txHash == utxo.txHash && it.txOutputN == utxo.txOutputN && it.value == utxo.value }
            targetItem?.let {
                val index = utxos.indexOf(it)
                utxos[index].selected = !utxos[index].selected
                updateList()
            }
        }

    }

    private fun makeSections(utxos: ArrayList<Utxo>): ArrayList<Utxo> {
        //For showing sections
        val active = Utxo(section = "Active")
        val blocked = Utxo(section = "Blocked")
        val activeUtxo: ArrayList<Utxo> = arrayListOf()
        val blockedUtxo: ArrayList<Utxo> = arrayListOf()

        utxos.forEach {
            if (UtxoMetaUtil.has(hash = it.txHash!!, index = it.txOutputN!!.toInt())) {
                blockedUtxo.add(it)
            } else {
                activeUtxo.add(it)
            }
        }

        val list: ArrayList<Utxo> = arrayListOf()
        if (activeUtxo.isNotEmpty()) {
            list.add(active)
            list.addAll(activeUtxo)
        }
        if (blockedUtxo.isNotEmpty()) {
            list.add(blocked)
            list.addAll(blockedUtxo)
        }
        return list;

    }

    public fun setUtxos(items: ArrayList<Utxo>) {
        utxos.apply {
            clear()
            addAll(items)
        }
        updateList()
    }

    private fun updateList() {
        utxoAdapter.submitList(makeSections(utxos))
    }

    fun clearSelection() {
        utxoAdapter.enableMultiSelect(false)
        utxos.forEach {
            it.selected = false
        }
        actionMode?.finish()
    }


    internal class UTXOAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val SECTION = 0
        private val UTXO = 1

        private var multiSelectEnable = false
        private var onLongClick: () -> Unit = {};
        private var onClickListener: (Utxo) -> Unit = {}
        private var multiSelectLister: (Utxo) -> Unit = {}

        fun setLongClickListener(listener: () -> Unit) {
            onLongClick = listener
        }

        fun setClickListener(listener: (Utxo) -> Unit) {
            onClickListener = listener
        }

        fun setMultiClickListener(listener: (Utxo) -> Unit) {
            multiSelectLister = listener
        }


        class UtxoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val amount: TextView = itemView.findViewById(R.id.utxo_item_amount);
            val address: TextView = itemView.findViewById(R.id.utxo_item_address);
            val checkBox: CheckBox = itemView.findViewById(R.id.multiselect_checkbox);
        }


        class UtxoViewHolderSection(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val section: TextView = itemView.findViewById(R.id.section_title)
        }


        private val diffCallBack = object : DiffUtil.ItemCallback<Utxo>() {
            override fun areItemsTheSame(oldItem: Utxo, newItem: Utxo): Boolean {
                return newItem.txHash == oldItem.txHash && newItem.txOutputN == newItem.txOutputN
            }

            override fun areContentsTheSame(oldItem: Utxo, newItem: Utxo): Boolean {
                return newItem.selected == oldItem.selected
            }

        }
        private val mDiffer: AsyncListDiffer<Utxo> = AsyncListDiffer(this, diffCallBack)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            if (viewType == SECTION) {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.utxo_section_layout, parent, false);
                return UtxoViewHolderSection(view)
            }
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.utxo_item_layout, parent, false);
            return UtxoViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val utxo = mDiffer.currentList[position]
            if (holder is UtxoViewHolderSection) {
                holder.section.text = utxo.section
                if(utxo.section == "Blocked"){
                    holder.section.setTextColor(ContextCompat.getColor(holder.section.context,R.color.md_red_A700))
                }
            }
            if (holder is UtxoViewHolder) {
                utxo.value?.let {
                    holder.amount.text = "${MonetaryUtil.getInstance().formatToBtc(it)} BTC"
                }
                holder.address.text = utxo.addr.toString()
                holder.checkBox.visibility = View.GONE
                holder.itemView.setOnLongClickListener {
                    onLongClick.invoke()
                    multiSelectLister.invoke(utxo)
                    true
                }
                holder.itemView.setOnClickListener {
                    if (multiSelectEnable) {
                        multiSelectLister.invoke(utxo)
                    } else {
                        onClickListener.invoke(utxo)
                    }
                }
                holder.checkBox.isChecked = utxo.selected

                if(utxo.selected){
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context,R.color.v3_background_light))
                }else{
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context,R.color.v3_background))
                }
                holder.checkBox.setOnClickListener{
                    multiSelectLister.invoke(utxo)
                }

                if (multiSelectEnable) {
                    holder.checkBox.visibility = View.VISIBLE
                } else {
                    holder.checkBox.visibility = View.GONE
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (mDiffer.currentList[position].section != null) {
                SECTION
            } else {
                UTXO
            }
        }

        override fun getItemCount(): Int {
            return mDiffer.currentList.size;
        }

        fun submitList(items: ArrayList<Utxo>) {
            try {
                val list: ArrayList<Utxo> = arrayListOf()
                // Diff util will perform a shallow compare with updated list,
                // since we're using same list with updated items. we need to make a new copy
                // this will make shallow compare false
                items.forEach { list.add(it.copy()) }
                mDiffer.submitList(list)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        fun enableMultiSelect(enable: Boolean = true) {
            multiSelectEnable = enable
            this.notifyDataSetChanged()
        }

    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode!!.menuInflater.inflate(R.menu.utxo_actions_menu, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onActionItemClicked(p0: ActionMode?, menu: MenuItem?): Boolean {

        menu?.let {
            when (it.itemId) {
                R.id.utxo_details_action_do_not_spend -> {
                    utxos.filter { utxo -> utxo.selected }.forEachIndexed { _, utxo ->
                        UtxoMetaUtil.put(utxo)
                    }
                    utxos.forEach { utxo -> utxo.selected = false }
                    updateList()
                }
                R.id.utxo_details_action_spendable -> {
                    utxos.filter { utxo -> utxo.selected }.forEachIndexed { _, utxo ->
                        UtxoMetaUtil.remove(utxo)
                    }
                    utxos.forEach { utxo -> utxo.selected = false }
                    updateList()
                }
            }
        }

        return true
    }

    override fun onDestroyActionMode(p0: ActionMode?) {
        actionMode = null
        utxos.forEach { it.selected = false }
        updateList()
        utxoAdapter.enableMultiSelect(false)
        updateList()
    }

}