package com.samourai.sentinel.ui.collectionDetails.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.samourai.sentinel.R
import com.samourai.sentinel.data.PubKeyCollection

class SendFragment : Fragment() {

    private val sendViewModel: SendViewModel by viewModels()
    private lateinit var collection: PubKeyCollection;

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_spend, container, false)
    }


    fun setCollection(collection: PubKeyCollection) {
        this.collection = collection
    }
}