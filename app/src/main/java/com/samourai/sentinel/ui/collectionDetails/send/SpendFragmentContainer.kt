package com.samourai.sentinel.ui.collectionDetails.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.samourai.sentinel.R
import com.samourai.sentinel.data.PubKeyCollection

/**
 * sentinel-android
 */
class SpendFragmentContainer : Fragment() {

    private lateinit var collection: PubKeyCollection;

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sendFragment: SendFragment = SendFragment()
        val transactions = childFragmentManager.beginTransaction()
        sendFragment.setCollection(collection)
        transactions.commit();
    }

    fun setCollection(collection: PubKeyCollection) {
        this.collection = collection
    }
}