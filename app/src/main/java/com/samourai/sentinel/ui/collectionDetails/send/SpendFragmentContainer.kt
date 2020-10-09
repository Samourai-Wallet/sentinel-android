package com.samourai.sentinel.ui.collectionDetails.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.samourai.sentinel.data.PubKeyCollection

/**
 * sentinel-android
 */
class SpendFragmentContainer : Fragment() {

    private lateinit var collection: PubKeyCollection;
    lateinit var frame: FrameLayout

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        frame = FrameLayout(requireContext())
        frame.id = View.generateViewId();
//        frame = FrameLayout(c
//        ontainer?.context!!)
        return frame
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sendFragment: SendFragment = SendFragment()
        val transactions = childFragmentManager.beginTransaction()
        transactions.add(frame.id, sendFragment);
        sendFragment.setCollection(collection)
        transactions.commit();
    }

    fun setCollection(collection: PubKeyCollection) {
        this.collection = collection
    }
}