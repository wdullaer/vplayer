/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.os.Bundle
import android.os.Handler
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.ImageCardView
import android.support.v17.leanback.widget.VerticalGridPresenter
import android.util.Log
import android.widget.Toast

/**
 * Fragment that shows a grid of videos that can be scrolled vertically
 *
 * Created by wdullaer on 23/10/17.
 */
class VerticalGridFragment : android.support.v17.leanback.app.VerticalGridSupportFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("VerticalGridFragment", "onCreate")

        val category = requireActivity().intent.getSerializableExtra(INTENT_CATEGORY) as Category

        title = category.name

        val arrayAdapter = ArrayObjectAdapter(CardPresenter(CardSize.SMALL))
        getMoviesByCategory(category) { error, videos ->
            error?.let {
                // TODO: distinguish between network and parsing errors
                Toast.makeText(requireContext(), R.string.video_error_server_inaccessible, Toast.LENGTH_LONG).show()
            }
            arrayAdapter.addAll(0, videos)
            arrayAdapter.notifyArrayItemRangeChanged(0, arrayAdapter.size())
        }
        adapter = arrayAdapter

        if (savedInstanceState == null) prepareEntranceTransition()

        gridPresenter  = VerticalGridPresenter()
        gridPresenter.numberOfColumns = 5

        Handler().postDelayed({ startEntranceTransition() }, 500L)

        setOnSearchClickedListener { requireActivity().startSettingsActivity() }
        // todo: Maybe make this a generic thing we can call from multiple activities?
        setOnItemViewClickedListener { itemViewHolder, item, _, _ ->
            if (item !is Video) return@setOnItemViewClickedListener
            requireActivity().startDetailsActivity(item, (itemViewHolder.view as ImageCardView).mainImageView)
        }
    }
}
