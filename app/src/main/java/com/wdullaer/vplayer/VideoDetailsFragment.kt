/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v17.leanback.app.DetailsFragment
import android.support.v17.leanback.app.DetailsFragmentBackgroundController
import android.support.v17.leanback.widget.Action
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.ClassPresenterSelector
import android.support.v17.leanback.widget.DetailsOverviewRow
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper
import android.support.v17.leanback.widget.HeaderItem
import android.support.v17.leanback.widget.ImageCardView
import android.support.v17.leanback.widget.ListRow
import android.support.v17.leanback.widget.ListRowPresenter
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition

/**
 * A wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 * TODO: ensure related videos are visible in the initial render
 */
class VideoDetailsFragment : DetailsFragment() {

    private lateinit var mSelectedVideo: Video

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)

        mSelectedVideo = activity.intent.getSerializableExtra(DetailsActivity.VIDEO) as Video
        val presenterSelector = ClassPresenterSelector()
        val arrayAdapter = ArrayObjectAdapter(presenterSelector)

        val cookie = activity.getSharedPreferences(AUTHENTICATION_PREFERENCE_ROOT, Context.MODE_PRIVATE)
                .getString(activity.getString(R.string.pref_cookie_key), "")
        enrichVideo(mSelectedVideo, cookie) {
            val detailsBackground = DetailsFragmentBackgroundController(this)
            initializeBackground(mSelectedVideo, arrayAdapter, detailsBackground)
            updateRelatedMoviesListRow(arrayAdapter)
            arrayAdapter.notifyArrayItemRangeChanged(0, arrayAdapter.size())
        }

        setupDetailsOverviewRow(arrayAdapter)
        setupDetailsOverviewRowPresenter(presenterSelector)
        setupRelatedMovieListRow(arrayAdapter, presenterSelector)

        adapter = arrayAdapter
        setOnItemViewClickedListener { itemViewHolder, item, _, _ ->
            if (item is Video) {
                Log.d(TAG, "Item: " + item.toString())
                activity.startDetailsActivity(
                        item,
                        (itemViewHolder?.view as ImageCardView).mainImageView
                )
            }
        }
    }

    private fun initializeBackground(video: Video, arrayAdapter : ArrayObjectAdapter, background : DetailsFragmentBackgroundController) {
        background.enableParallax()
        val glideOptions = RequestOptions()
                .centerCrop()
                .error(R.drawable.default_background)
        Glide.with(activity)
                .asBitmap()
                .load(video.backgroundImageUrl)
                .apply(glideOptions)
                .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(bitmap: Bitmap,
                                                 glideAnimation: Transition<in Bitmap>?) {
                        background.coverBitmap = bitmap
                        arrayAdapter.notifyArrayItemRangeChanged(0, adapter.size())
                    }
                })
    }

    private fun setupDetailsOverviewRow(arrayAdapter : ArrayObjectAdapter) {
        Log.d(TAG, "doInBackground: " + mSelectedVideo.toString())
        val row = DetailsOverviewRow(mSelectedVideo)
        row.imageDrawable = ContextCompat.getDrawable(activity, R.drawable.default_background)
        val width = convertDpToPixel(activity, DETAIL_THUMB_WIDTH)
        val height = convertDpToPixel(activity, DETAIL_THUMB_HEIGHT)
        val glideOptions = RequestOptions()
                .centerCrop()
                .error(R.drawable.default_background)
        Glide.with(activity)
                .load(mSelectedVideo.cardImageUrl)
                .apply(glideOptions)
                .into<SimpleTarget<Drawable>>(object : SimpleTarget<Drawable>(width, height) {
                    override fun onResourceReady(resource: Drawable,
                                                 glideAnimation: Transition<in Drawable>?) {
                        Log.d(TAG, "details overview card image url ready: " + resource)
                        row.imageDrawable = resource
                        arrayAdapter.notifyArrayItemRangeChanged(0, adapter.size())
                    }
                })

        val actionAdapter = ArrayObjectAdapter()
        actionAdapter.add(Action(
                ACTION_PLAY,
                resources.getString(R.string.play_video_1)
        ))
        row.actionsAdapter = actionAdapter

        arrayAdapter.add(row)
    }

    private fun setupDetailsOverviewRowPresenter(presenterSelector: ClassPresenterSelector) {
        // Set detail background.
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())
        detailsPresenter.backgroundColor =
                ContextCompat.getColor(activity, R.color.selected_background)

        // Hook up transition element.
        val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
        sharedElementHelper.setSharedElementEnterTransition(
                activity, DetailsActivity.SHARED_ELEMENT_NAME)
        detailsPresenter.setListener(sharedElementHelper)
        detailsPresenter.isParticipatingEntranceTransition = true

        detailsPresenter.setOnActionClickedListener {
            // TODO: check that we have a videoUrl before launching playback
            if (it.id == ACTION_PLAY) {
                activity.startPlaybackActivity(mSelectedVideo)
            } else {
                Toast.makeText(activity, it.toString(), Toast.LENGTH_SHORT).show()
            }
        }
        presenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
    }

    private fun setupRelatedMovieListRow(arrayAdapter : ArrayObjectAdapter, presenterSelector : ClassPresenterSelector) {
        updateRelatedMoviesListRow(arrayAdapter)
        presenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
    }

    private fun updateRelatedMoviesListRow(arrayAdapter : ArrayObjectAdapter) {
        if (arrayAdapter.size() > 1) arrayAdapter.removeItems(1, arrayAdapter.size() - 1)

        val cardPresenter = CardPresenter()
        val listRows = mSelectedVideo.relatedVideos.mapIndexed { index, playlist ->
            val header = HeaderItem(index.toLong(), playlist.title)
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            listRowAdapter.addAll(0, playlist.data)
            ListRow(header, listRowAdapter)
        }
        arrayAdapter.addAll(arrayAdapter.size(), listRows)
    }

    companion object {
        private const val TAG = "VideoDetailsFragment"

        private const val ACTION_PLAY = 1L

        private const val DETAIL_THUMB_WIDTH = 487
        private const val DETAIL_THUMB_HEIGHT = 274
    }
}