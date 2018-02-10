/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.os.Bundle
import android.support.v17.leanback.app.SearchFragment
import android.support.v17.leanback.widget.*
import java.util.*
import kotlin.concurrent.schedule

/**
 * Activity that renders a SearchFragment that renders any potential search results
 *
 * Created by wdullaer on 23/10/17.
 */
class SearchActivity : LeanbackActivity() {
    private lateinit var mFragment : SearchFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        mFragment = fragmentManager.findFragmentById(R.id.search_fragment) as SearchFragment
    }
}

class SearchFragment : SearchFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSearchResultProvider(object : SearchResultProvider {
            val mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())
            var videoAdapter = ArrayObjectAdapter(CardPresenter())
            val timer = Timer("SearchTimer")
            var task : TimerTask? = null
            val SEARCH_DELAY = 300L
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query == null || query == "") {
                    mRowsAdapter.clear()
                    return true
                }
                updateSearchResults(query)
                return true
            }

            fun updateSearchResults(query : String) {
                searchVideo(query) {
                    videoAdapter.clear()
                    videoAdapter.addAll(0, it.data)
                    val header = HeaderItem("${activity.getString(R.string.search_results)} ${it.title}")
                    mRowsAdapter.clear()
                    mRowsAdapter.add(ListRow(header, videoAdapter))
                }
            }

            override fun getResultsAdapter(): ObjectAdapter {
                return mRowsAdapter
            }

            override fun onQueryTextChange(newQuery: String?): Boolean {
                if (newQuery == null || newQuery == "") {
                    mRowsAdapter.clear()
                    return true
                }
                task?.cancel()
                task = timer.schedule(SEARCH_DELAY) {
                    updateSearchResults(newQuery)
                }
                timer.purge()
                return true
            }
        })

        setOnItemViewClickedListener { itemViewHolder, item, _, _ ->
            activity.startDetailsActivity(item as Video, (itemViewHolder.view as ImageCardView).mainImageView)
        }
    }
}