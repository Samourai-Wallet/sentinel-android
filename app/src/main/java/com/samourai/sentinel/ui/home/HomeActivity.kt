package com.samourai.sentinel.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.samourai.sentinel.R
import com.samourai.sentinel.ui.SentinelActivity
import com.samourai.sentinel.ui.adapters.CollectionsAdapter
import com.samourai.sentinel.ui.collectionDetails.CollectionDetails
import com.samourai.sentinel.ui.dojo.NetworkActivity
import com.samourai.sentinel.ui.fragments.AddNewPubKeyBottomSheet
import com.samourai.sentinel.ui.utils.RecyclerViewItemDividerDecorator
import kotlinx.android.synthetic.main.activity_home.*


class HomeActivity : SentinelActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private val collectionsAdapter = CollectionsAdapter();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(findViewById(R.id.toolbarCollectionDetails))

        val model: HomeViewModel by viewModels()

        setUpCollectionList()

        model.collections.observe(this, Observer { collectionsAdapter.update(it) })

        fab.setOnClickListener {
            val bottomSheetFragment = AddNewPubKeyBottomSheet()
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

    }


    private fun setUpCollectionList() {

        collectionsAdapter.setOnClickListener {
            startActivity(Intent(applicationContext, CollectionDetails::class.java).apply {
                putExtra("collection", it.id)
            })
        }

        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        val decorator = RecyclerViewItemDividerDecorator(ContextCompat.getDrawable(applicationContext, R.drawable.divider_home)!!);
        collectionRecyclerView.apply {
            adapter = collectionsAdapter
            layoutManager = linearLayoutManager
            setHasFixedSize(true)
            addItemDecoration(decorator)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let { setNetWorkMenu(it) }
        return super.onPrepareOptionsMenu(menu)
    }


    private fun setNetWorkMenu(menu: Menu) {
        val alertMenuItem: MenuItem = menu.findItem(R.id.activity_home_menu_network)
        val rootView = alertMenuItem.actionView
        val redCircle = rootView.findViewById<View>(R.id.home_menu_network_shape) as FrameLayout
        val shape = ContextCompat.getDrawable(applicationContext, R.drawable.circle_shape);
        shape?.setTint(ContextCompat.getColor(applicationContext, R.color.red))
        redCircle.background = shape
        redCircle.visibility = View.VISIBLE
        rootView.setOnClickListener {
            startActivity(Intent(this, NetworkActivity::class.java))
        }
    }

}

