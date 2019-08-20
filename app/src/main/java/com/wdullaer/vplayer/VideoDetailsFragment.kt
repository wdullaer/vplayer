/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.*
import androidx.core.content.ContextCompat
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.Transition
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.DetailsOverviewRow
import android.widget.ImageView
import com.wdullaer.vplayer.glide.CustomTarget


/**
 * A wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
class VideoDetailsFragment : DetailsSupportFragment() {

    private var selectedVideo: Video = Video()
    private lateinit var backgroundManager : BackgroundManager
    private val windowMetrics = DisplayMetrics()

    private val NO_NOTIFICATION = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)

        val activity = requireActivity()

        backgroundManager = BackgroundManager.getInstance(activity)
        backgroundManager.attach(activity.window)
        activity.windowManager.defaultDisplay.getMetrics(windowMetrics)

        removeNotification(activity)

        setOnItemViewClickedListener { itemViewHolder, item, _, _ ->
            if (item is Video) {
                Log.d(TAG, "Item: $item")
                activity.startDetailsActivity(
                        item,
                        (itemViewHolder?.view as ImageCardView).mainImageView
                )
            }
        }
        setOnSearchClickedListener { activity.startSearchActivity() }
        searchAffordanceColor = ContextCompat.getColor(activity, R.color.search_opaque)

        // We were started by the global search
        if (Intent.ACTION_VIEW.equals(activity.intent.action, true)) {
            getVideoByPubId(activity.intent.data?.lastPathSegment ?: "") { err, video ->
                err?.let {
                    // TODO: distinguish between network and parsing error
                    Toast.makeText(activity, R.string.video_error_server_inaccessible, Toast.LENGTH_LONG).show()
                }
                selectedVideo = video
                init(activity, true)
            }
        // We were started from within the application
        } else {
            Log.i("DetailsFragment", activity.intent.extras?.toString())
            selectedVideo = activity.intent.getParcelableExtra(DetailsActivity.VIDEO) as Video
            init(activity, false)
        }
    }

    /**
     * Fragment initialisation that can only happen once we have some basic information about
     * the Video that needs to be shown
     */
    private fun init(activity : Activity, autoPlay : Boolean) {
        val presenterSelector = ClassPresenterSelector()
        val arrayAdapter = ArrayObjectAdapter(presenterSelector)

        // TODO: make these calls more functional (pass in the video)
        // it will make it easier to see which state needs to be present for them to do their job
        setupDetailsOverviewRow(activity, arrayAdapter)
        setupDetailsOverviewRowPresenter(activity, presenterSelector)
        setupRelatedMovieListRow(arrayAdapter, presenterSelector)

        adapter = arrayAdapter

        val cookie = activity.getSharedPreferences(VPLAYER_PREFERENCE_ROOT, Context.MODE_PRIVATE)
                .getString(activity.getString(R.string.pref_cookie_key), "") ?: ""
        // This is explicitly side effecty to avoid flickering (but we should test if we can get
        // away with replacing the video)
        enrichVideo(selectedVideo, cookie) {
            it?.let {
                // TODO: distinguish between network, authorization and parsing error
                Toast.makeText(activity, R.string.video_error_server_inaccessible, Toast.LENGTH_LONG).show()
            }
            updateBackground(selectedVideo)
            updateRelatedMoviesListRow(arrayAdapter)
            arrayAdapter.notifyArrayItemRangeChanged(0, arrayAdapter.size())
            if (autoPlay && it != null) activity.startPlaybackActivity(selectedVideo)
        }
    }

    override fun onResume() {
        super.onResume()
        // Android doesn't seem to remember the background when coming back from another activity
        // So let's explicitly make sure it's showing
        updateBackground(selectedVideo)
    }

    private fun removeNotification(activity: Activity) {
        val notification = activity.intent.getIntExtra(DetailsActivity.NOTIFICATION, NO_NOTIFICATION)
        if (notification != NO_NOTIFICATION) {
            (activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(notification)
        }
    }

    private fun updateBackground(video: Video) {
        Log.i("VideoDetailsFragment", "Using background ${video.backgroundImageUrl}")
        if (video.backgroundImageUrl == null) return

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
                .into(object : CustomTarget<Bitmap>(width, height) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        backgroundManager.setBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        if (backgroundManager.isAttached) {
                            backgroundManager.clearDrawable()
                        }
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        errorDrawable?.let { backgroundManager.drawable = it }
                    }
                })
    }

    private fun setupDetailsOverviewRow(context: Context, arrayAdapter : ArrayObjectAdapter) {
        Log.d(TAG, "doInBackground: $selectedVideo")
        val row = DetailsOverviewRow(selectedVideo)
        row.imageDrawable = ContextCompat.getDrawable(context, R.drawable.default_background)
        val width = context.resources.getDimensionPixelSize(R.dimen.details_thumb_width)
        val height = context.resources.getDimensionPixelSize(R.dimen.details_thumb_heigth)
        val glideOptions = RequestOptions()
                .fitCenter()
                .dontAnimate()
                .error(R.drawable.default_background)
                .placeholder(R.drawable.default_background)
        Glide.with(this)
                .asBitmap()
                .load(selectedVideo.cardImageUrl)
                .apply(glideOptions)
                .into(object : CustomTarget<Bitmap>(width, height) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        Log.d(TAG, "details overview card image url ready: $resource")
                        row.setImageBitmap(activity, resource)
                        startEntranceTransition()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        placeholder?.let { row.imageDrawable = it }
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        errorDrawable?.let { row.imageDrawable = it }
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

    private fun setupDetailsOverviewRowPresenter(activity: Activity, presenterSelector: ClassPresenterSelector) {
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
                viewHolder?.logoViewHolder?.view?.let {
                    val lp = it.layoutParams as ViewGroup.MarginLayoutParams
                    lp.marginStart = it.resources.getDimensionPixelSize(
                            androidx.leanback.R.dimen.lb_details_v2_logo_margin_start
                    )
                    lp.topMargin = it.resources.getDimensionPixelSize(
                            androidx.leanback.R.dimen.lb_details_v2_blank_height
                    ) - lp.height / 2
                    val offset = it.resources.getDimensionPixelSize(
                            androidx.leanback.R.dimen.lb_details_v2_actions_height
                    ) + it.resources.getDimensionPixelSize(
                            androidx.leanback.R.dimen.lb_details_v2_description_margin_top
                    ) + (lp.height / 2)

                    when (viewHolder.state) {
                        STATE_SMALL -> lp.topMargin = 0
                        STATE_HALF -> {
                            if (previousState == STATE_FULL) it.animate().translationYBy(offset.toFloat())
                        }
                        STATE_FULL -> {
                            if (previousState == STATE_HALF) it.animate().translationYBy(-offset.toFloat())
                        }
                        else -> {
                            if (previousState == STATE_HALF) it.animate().translationYBy(-offset.toFloat())
                        }
                    }

                    previousState = viewHolder.state
                    it.layoutParams = lp
                }
            }
        }
        detailsPresenter.backgroundColor =
                ContextCompat.getColor(activity, R.color.selected_background)

        // Hook up transition element.
        val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
        sharedElementHelper.setSharedElementEnterTransition(
                activity,
                DetailsActivity.SHARED_ELEMENT_NAME
        )
        detailsPresenter.setListener(sharedElementHelper)
        detailsPresenter.isParticipatingEntranceTransition = false
        prepareEntranceTransition()

        detailsPresenter.setOnActionClickedListener {
            // TODO: check that we have a videoUrl before launching playback
            if (it.id == ACTION_PLAY) {
                activity.startPlaybackActivity(selectedVideo)
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
        val listRows = selectedVideo.relatedVideos.mapIndexed { index, playlist ->
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
    }

    // This class ensures that the animation of the thumb works every time
    class VideoDetailsOverviewLogoPresenter : DetailsOverviewLogoPresenter() {
        override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
            val imageView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.lb_fullwidth_details_overview_logo, parent, false) as ImageView

            val width = parent.context.resources.getDimensionPixelSize(R.dimen.details_thumb_width)
            val height = parent.context.resources.getDimensionPixelSize(R.dimen.details_thumb_heigth)
            imageView.layoutParams = ViewGroup.MarginLayoutParams(width, height)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            return ViewHolder(imageView)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
            val row = item as DetailsOverviewRow
            val imageView = viewHolder.view as ImageView
            imageView.setImageDrawable(row.imageDrawable)
            if (isBoundToImage(viewHolder as ViewHolder, row)) {

                viewHolder.parentPresenter.notifyOnBindLogo(viewHolder.parentViewHolder)
            }
        }
    }
}