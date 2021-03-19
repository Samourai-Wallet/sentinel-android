package com.samourai.sentinel.ui.collectionEdit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.samourai.sentinel.R
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.util.MonetaryUtil
import java.util.*


/**
 * sentinel-android
 *
 */

class PubKeyAdapter : RecyclerView.Adapter<PubKeyAdapter.PubKeyViewHolder>() {

    private var editingItem = ""
    private var onDeleteClick: (Int) -> Unit = {}
    private var onOptionsClick: (Int, PubKeyModel) -> Unit = { _, _ -> run {} }

    private val diffCallBack = object : DiffUtil.ItemCallback<PubKeyModel>() {

        override fun areItemsTheSame(oldItem: PubKeyModel, newItem: PubKeyModel): Boolean {
            return oldItem.pubKey == newItem.pubKey
        }

        override fun areContentsTheSame(oldItem: PubKeyModel, newItem: PubKeyModel): Boolean {
            if(oldItem.label != newItem.label){
                return false
            }
            if(oldItem.pubKey != newItem.pubKey){
                return false
            }
            return oldItem == newItem
        }

    }
    private val mDiffer: AsyncListDiffer<PubKeyModel> = AsyncListDiffer(this, diffCallBack)


    class PubKeyViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        lateinit var selectedPub: String
        lateinit var pubModel: PubKeyModel
        val layout: ConstraintLayout = view.findViewById(R.id.constraintLayoutRv)
        val moreButton: MaterialButton = view.findViewById(R.id.moreButton)
        val label: TextView = view.findViewById(R.id.pubKeyItemLabel)
        val pubKeyAmount: TextView = view.findViewById(R.id.pubkeyAmount)
        val pubKeyType: TextView = view.findViewById(R.id.pubKeyType)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PubKeyViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.pub_key_item_layout, parent, false)
        val holder = PubKeyViewHolder(view);
        return holder
    }

    override fun getItemCount(): Int {
        return mDiffer.currentList.size
    }

    override fun onBindViewHolder(holderPubKey: PubKeyViewHolder, position: Int) {
        val pubKeyModel = mDiffer.currentList[position]
        holderPubKey.layout.setOnClickListener {
            editingItem = pubKeyModel.pubKey
            this.notifyDataSetChanged()
        }
        holderPubKey.pubModel = pubKeyModel
        holderPubKey.selectedPub = editingItem
        holderPubKey.pubKeyAmount.text = "${MonetaryUtil.getInstance().getBTCDecimalFormat(pubKeyModel.balance)} BTC"
        holderPubKey.label.text = pubKeyModel.label
        holderPubKey.pubKeyType.text = pubKeyModel.type.toString()

        holderPubKey.moreButton.setOnClickListener {
        onOptionsClick.invoke(position,pubKeyModel)
        }

    }

    fun setOnEditClickListener(callback: (Int, PubKeyModel) -> Unit) {
        this.onOptionsClick = callback
    }

    fun setEditingPubKey(pubKey: String) {
        editingItem = pubKey
        this.notifyDataSetChanged()
    }

    fun update(newItems: ArrayList<PubKeyModel>) {
        val list = arrayListOf<PubKeyModel>()
        // Diff util will perform a shallow compare with updated list,
        // since we're using same list with updated items. we need to make a new copy
        // this will make shallow compare false
        newItems.forEach { list.add(it.copy()) }
        if (newItems.size != 0) {
            this.editingItem = newItems[newItems.lastIndex].pubKey
        }
        mDiffer.submitList(list)
    }
}