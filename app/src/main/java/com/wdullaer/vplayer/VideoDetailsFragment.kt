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
import android.os.Bundle
import android.support.v17.leanback.app.BackgroundManager
import android.support.v17.leanback.app.DetailsFragment
import android.support.v17.leanback.widget.*
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import android.view.ViewGroup
import android.view.LayoutInflater
import android.support.v17.leanback.widget.Presenter
import android.support.v17.leanback.widget.DetailsOverviewRow
import android.widget.ImageView


/**
 * A wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
class VideoDetailsFragment : DetailsFragment() {

    private lateinit var mSelectedVideo: Video
    private lateinit var backgroundManager : BackgroundManager
    private val windowMetrics = DisplayMetrics()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)

        backgroundManager = BackgroundManager.getInstance(activity)
        backgroundManager.attach(activity.window)
        activity.windowManager.defaultDisplay.getMetrics(windowMetrics)

        mSelectedVideo = activity.intent.getSerializableExtra(DetailsActivity.VIDEO) as Video
        val presenterSelector = ClassPresenterSelector()
        val arrayAdapter = ArrayObjectAdapter(presenterSelector)

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
        setOnSearchClickedListener { activity.startSearchActivity() }
        searchAffordanceColor = ContextCompat.getColor(activity, R.color.search_opaque)

        val cookie = activity.getSharedPreferences(AUTHENTICATION_PREFERENCE_ROOT, Context.MODE_PRIVATE)
                .getString(activity.getString(R.string.pref_cookie_key), "")
        enrichVideo(mSelectedVideo, cookie) {
            updateBackground(mSelectedVideo)
            updateRelatedMoviesListRow(arrayAdapter)
            arrayAdapter.notifyArrayItemRangeChanged(0, arrayAdapter.size())
        }
    }

    override fun onResume() {
        super.onResume()
        // Android doesn't seem to remember the background when coming back from another activity
        // So let's explicitly make sure it's showing
        updateBackground(mSelectedVideo)
    }

    private fun updateBackground(video: Video) {
        val width = windowMetrics.widthPixels
        val height = windowMetrics.heightPixels
        val glideOptions = RequestOptions()
                .centerCrop()
                .transform(StackBlurTransform(50, 1))
                .error(R.drawable.default_background)
        Glide.with(this)
                .asBitmap()
                .load(video.backgroundImageUrl)
                .apply(glideOptions)
                .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>(width, height) {
                    override fun onResourceReady(bitmap: Bitmap,
                                                 glideAnimation: Transition<in Bitmap>?) {
                        backgroundManager.setBitmap(bitmap)
                    }
                })
    }

    private fun setupDetailsOverviewRow(arrayAdapter : ArrayObjectAdapter) {
        Log.d(TAG, "doInBackground: " + mSelectedVideo.toString())
        val row = DetailsOverviewRow(mSelectedVideo)
        row.imageDrawable = ContextCompat.getDrawable(activity, R.drawable.default_background)
        val width = activity.convertDpToPixel(DETAIL_THUMB_WIDTH)
        val height = activity.convertDpToPixel(DETAIL_THUMB_HEIGHT)
        val glideOptions = RequestOptions()
                .fitCenter()
                .dontAnimate()
                .error(R.drawable.default_background)
        Glide.with(this)
                .asBitmap()
                .load(mSelectedVideo.cardImageUrl)
                .apply(glideOptions)
                .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>(width, height) {
                    override fun onResourceReady(resource: Bitmap,
                                                 glideAnimation: Transition<in Bitmap>?) {
                        Log.d(TAG, "details overview card image url ready: " + resource)
                        row.setImageBitmap(activity, resource)
                        startEntranceTransition()
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
        val detailsPresenter = object : FullWidthDetailsOverviewRowPresenter(
                DetailsDescriptionPresenter(),
                VideoDetailsOverviewLogoPresenter()
        ) {
            init {
                initialState = STATE_FULL
            }

            var previousState = STATE_FULL

            // This animates the logo when scrolling down in the detailspresenter
            override fun onLayoutLogo(viewHolder: ViewHolder?, oldState: Int, logoChanged: Boolean) {
                if (viewHolder == null) return

                val v = viewHolder.logoViewHolder?.view
                val lp = v?.layoutParams as ViewGroup.MarginLayoutParams
                lp.marginStart = v.resources.getDimensionPixelSize(
                        android.support.v17.leanback.R.dimen.lb_details_v2_logo_margin_start
                )
                lp.topMargin = v.resources.getDimensionPixelSize(
                        android.support.v17.leanback.R.dimen.lb_details_v2_blank_height
                ) - lp.height / 2
                val offset = v.resources.getDimensionPixelSize(
                        android.support.v17.leanback.R.dimen.lb_details_v2_actions_height
                ) + v.resources.getDimensionPixelSize(
                        android.support.v17.leanback.R.dimen.lb_details_v2_description_margin_top
                ) + (lp.height / 2)

                when (viewHolder.state) {
                    STATE_SMALL -> lp.topMargin = 0
                    STATE_HALF -> {
                        if (previousState == STATE_FULL) v.animate().translationYBy(offset.toFloat())
                    }
                    STATE_FULL -> {
                        if (previousState == STATE_HALF) v.animate().translationYBy(-offset.toFloat())
                    }
                    else -> {
                        if (previousState == STATE_HALF) v.animate().translationYBy(-offset.toFloat())
                    }
                }

                previousState = viewHolder.state
                v.layoutParams = lp
            }
        }
        detailsPresenter.backgroundColor =
                ContextCompat.getColor(activity, R.color.selected_background)

        // Hook up transition element.
        val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
        sharedElementHelper.setSharedElementEnterTransition(
                activity, DetailsActivity.SHARED_ELEMENT_NAME)
        detailsPresenter.setListener(sharedElementHelper)
        detailsPresenter.isParticipatingEntranceTransition = false
        prepareEntranceTransition()

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

        // TODO: Convert these into dimen resources
        private const val DETAIL_THUMB_WIDTH = 274
        private const val DETAIL_THUMB_HEIGHT = 154
    }

    // This class ensures that the animation of the thumb works every time
    class VideoDetailsOverviewLogoPresenter : DetailsOverviewLogoPresenter() {
        override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
            val imageView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.lb_fullwidth_details_overview_logo, parent, false) as ImageView

            val width = parent.context.convertDpToPixel(DETAIL_THUMB_WIDTH)
            val height = parent.context.convertDpToPixel(DETAIL_THUMB_HEIGHT)
            imageView.layoutParams = ViewGroup.MarginLayoutParams(width, height)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            return DetailsOverviewLogoPresenter.ViewHolder(imageView)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
            val row = item as DetailsOverviewRow
            val imageView = viewHolder.view as ImageView
            imageView.setImageDrawable(row.imageDrawable)
            if (isBoundToImage(viewHolder as DetailsOverviewLogoPresenter.ViewHolder, row)) {

                viewHolder.parentPresenter.notifyOnBindLogo(viewHolder.parentViewHolder)
            }
        }
    }
}