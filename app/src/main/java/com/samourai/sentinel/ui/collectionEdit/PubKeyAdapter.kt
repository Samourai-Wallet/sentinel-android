package com.samourai.sentinel.ui.collectionEdit

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.*
import com.google.android.material.textfield.TextInputLayout
import com.samourai.sentinel.R
import com.samourai.sentinel.data.PubKeyModel
import com.samourai.sentinel.helpers.toJSON
import com.samourai.sentinel.ui.utils.SlideInItemAnimator
import com.samourai.sentinel.ui.utils.hideKeyboard
import java.util.*


/**
 * sentinel-android
 *
 * @author sarath
 */


class PubKeyListItemAnimator(addRemoveDuration: Long) : SlideInItemAnimator() {

    init {
        addDuration = addRemoveDuration
        removeDuration = addRemoveDuration
    }

    override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
        return true
    }

    override fun recordPreLayoutInformation(
            state: RecyclerView.State,
            viewHolder: RecyclerView.ViewHolder,
            changeFlags: Int,
            payloads: List<Any>
    ): ItemHolderInfo {
        val info = super.recordPreLayoutInformation(
                state, viewHolder, changeFlags, payloads) as PubKeyHolderInfo
        info.doExpand = payloads.contains(EXPAND)
        info.doCollapse = payloads.contains(COLLAPSE)
        return info
    }

    override fun animateChange(
            oldHolder: RecyclerView.ViewHolder,
            newHolder: RecyclerView.ViewHolder,
            preInfo: ItemHolderInfo,
            postInfo: ItemHolderInfo
    ): Boolean {
        if (newHolder is PubKeyAdapter.PubKeyViewHolder && preInfo is PubKeyHolderInfo) {
            if (preInfo.doExpand) {
                newHolder.expand(this)
            } else if (preInfo.doCollapse) {
                newHolder.collapse(this)
            }
        }
        return super.animateChange(oldHolder, newHolder, preInfo, postInfo)
    }

    override fun obtainHolderInfo(): ItemHolderInfo {
        return PubKeyHolderInfo()
    }

    private class PubKeyHolderInfo : ItemHolderInfo() {
        internal var doExpand: Boolean = false
        internal var doCollapse: Boolean = false
    }

    companion object {
        const val EXPAND = 1
        const val COLLAPSE = 2
    }
}


class PubKeyAdapter : RecyclerView.Adapter<PubKeyAdapter.PubKeyViewHolder>() {

    private var editingItem = ""
    private var onDeleteClick: (Int) -> Unit = {}
    private var onEditLabelClick: (Int, PubKeyModel) -> Unit = { _, _ -> run {} }


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

        fun expand(animator: SlideInItemAnimator) {
            val transSet = TransitionSet()
                    .apply {
                        duration = 230
                        ordering = TransitionSet.ORDERING_SEQUENTIAL
                        addTransition(TransitionSet().apply {
                            addTransition(Fade(Fade.OUT))
                            addTransition(ChangeBounds())
                        })
                        addTransition(Fade(Fade.IN))
                                .addListener(object : Transition.TransitionListener {
                                    override fun onTransitionEnd(transition: Transition) {
                                        removeBtn.isEnabled = true
//                                        animator.dispatchChangeFinished(this@PubKeyViewHolder, false)
                                    }

                                    override fun onTransitionResume(transition: Transition) {
                                    }

                                    override fun onTransitionPause(transition: Transition) {
                                    }

                                    override fun onTransitionCancel(transition: Transition) {
                                    }

                                    override fun onTransitionStart(transition: Transition) {
//                                        animator.dispatchChangeStarting(this@PubKeyViewHolder, false)
                                    }

                                })
                    }
            val constraint2 = ConstraintSet()
            constraint2.clone(itemView.context, R.layout.pub_key_item_layout_expand)

            TransitionManager.beginDelayedTransition(layout, transSet)
            constraint2.applyTo(layout)

        }

        fun collapse(animator: SlideInItemAnimator) {

            val transSet = TransitionSet()
                    .apply {
                        duration = 120
                        ordering = TransitionSet.ORDERING_SEQUENTIAL
                        addTransition(TransitionSet().apply {
                            addTransition(Fade(Fade.IN))
                            addTransition(ChangeBounds())
                        })
                        addTransition(Fade(Fade.OUT))
                                .addListener(object : Transition.TransitionListener {
                                    override fun onTransitionEnd(transition: Transition) {
                                        removeBtn.isEnabled = false
                                    }

                                    override fun onTransitionResume(transition: Transition) {
                                    }

                                    override fun onTransitionPause(transition: Transition) {
                                    }

                                    override fun onTransitionCancel(transition: Transition) {
                                    }

                                    override fun onTransitionStart(transition: Transition) {
//                                        animator.dispatchChangeStarting(this@PubKeyViewHolder, false)
                                    }

                                })
                    }
            val constraint2 = ConstraintSet()
            constraint2.clone(itemView.context, R.layout.pub_key_item_layout_collapse)

            TransitionManager.beginDelayedTransition(layout, transSet)
            constraint2.applyTo(layout)

        }

        lateinit var selectedPub: String
        lateinit var pubModel: PubKeyModel
        val layout: ConstraintLayout = view.findViewById(R.id.constraintLayoutRv)
        val label: TextView = view.findViewById(R.id.pubKeyItemLabel)
        val removeBtn: Button = view.findViewById(R.id.removePubKeyBtn)
        val editBtn: Button = view.findViewById(R.id.editPubKey)
        val pubKey: TextView = view.findViewById(R.id.pubKeyString)

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
        holderPubKey.pubKey.text = pubKeyModel.pubKey
        holderPubKey.label.text = pubKeyModel.label
        holderPubKey.removeBtn.setOnClickListener { onDeleteClick(position) }
        holderPubKey.editBtn.setOnClickListener { onEditLabelClick(position,pubKeyModel) }

        holderPubKey.layout.setOnClickListener {

            holderPubKey.itemView.hideKeyboard()

            val pubKey = mDiffer.currentList[holderPubKey.adapterPosition]
            this.editingItem = pubKey.pubKey
            notifyItemChanged(holderPubKey.adapterPosition,
                    PubKeyListItemAnimator.EXPAND)
            mDiffer.currentList.forEach {
                if (editingItem != it.pubKey) {
                    notifyItemChanged(mDiffer.currentList.indexOf(it),
                            PubKeyListItemAnimator.COLLAPSE)
                }
            }
        }

    }


    fun setOnDeleteListener(callback: (Int) -> Unit) {
        this.onDeleteClick = callback
    }


    fun setOnEditClickListener(callback: (Int, PubKeyModel) -> Unit) {
        this.onEditLabelClick = callback
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

    companion object {
        const val LABEL_DURATION = 160L
        const val BTN_DURATION = 230L
    }
}