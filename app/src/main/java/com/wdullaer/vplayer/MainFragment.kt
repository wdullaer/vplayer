/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import java.util.Timer

import android.os.Bundle
import android.support.v17.leanback.app.BackgroundManager
import android.support.v17.leanback.app.BrowseSupportFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.HeaderItem
import android.support.v17.leanback.widget.ImageCardView
import android.support.v17.leanback.widget.ListRow
import android.support.v17.leanback.widget.ListRowPresenter
import android.support.v17.leanback.widget.Presenter
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlin.properties.Delegates
import kotlin.concurrent.schedule

/**
 * Loads a grid of cards with movies to browse.
 */
class MainFragment : BrowseSupportFragment() {

    private lateinit var mRowsAdapter: ArrayObjectAdapter
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private val mMetrics = DisplayMetrics()
    private var mBackgroundTimer: Timer = Timer()
    private var mBackgroundUri: String? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onActivityCreated(savedInstanceState)

        prepareBackgroundManager(requireActivity())

        setupUIElements(requireContext())

        loadRows(requireActivity())

        setupEventListeners(requireActivity())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString())
        mBackgroundTimer.cancel()
    }

    private fun prepareBackgroundManager(activity: Activity) {
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(activity.window)
        mDefaultBackground = ContextCompat.getDrawable(activity, R.drawable.default_background)
        activity.windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    private fun setupUIElements(context: Context) {
        title = getString(R.string.browse_title)
        // over title
        headersState = BrowseSupportFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // set fastLane (or headers) background color
        brandColor = ContextCompat.getColor(context, R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = ContextCompat.getColor(context, R.color.search_opaque)
    }

    private fun loadRows(activity: FragmentActivity) {
        mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = CardPresenter()

        getLandingPage(resources.getString(R.string.default_playlist_title)) { error, playlists ->
            error?.let {
                when (it) {
                    is ParserException -> activity.showErrorFragment(it)
                    is NetworkException -> Toast.makeText(activity, R.string.video_error_server_inaccessible, Toast.LENGTH_LONG).show()
                    else -> Toast.makeText(activity, R.string.video_error_server_inaccessible, Toast.LENGTH_LONG).show()
                }
            }

            mRowsAdapter.addAll(0, playlists.map {
                val listRowAdapter = ArrayObjectAdapter(cardPresenter)
                listRowAdapter.addAll(0, it.data)
                val header = HeaderItem(it.title)
                ListRow(header, listRowAdapter)
            })
            mRowsAdapter.notifyArrayItemRangeChanged(0, playlists.size)
            this.setSelectedPosition(0, true)
        }

        val gridHeader = HeaderItem(999L, "Preferences")

        val mGridPresenter = GridItemPresenter()
        val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
        gridRowAdapter.add(MenuCard(
                resources.getString(R.string.account_settings),
                R.drawable.ic_person_black_24dp,
                activity::startSettingsActivity
        ))
        mRowsAdapter.add(ListRow(gridHeader, gridRowAdapter))

        getCategories { error, categories ->
            error?.let {
                val resId = when (it) {
                    is ParserException -> R.string.parse_category_error
                    else -> R.string.video_error_server_inaccessible
                }
                Toast.makeText(activity, resId, Toast.LENGTH_LONG).show()
            }
            val categoryHeader = HeaderItem(998L, "Categories")
            val categoryRowAdapter = ArrayObjectAdapter(cardPresenter)
            categoryRowAdapter.addAll(0, categories)
            categoryRowAdapter.notifyArrayItemRangeChanged(0, categories.size)
            if (mRowsAdapter.size() == 0) {
                mRowsAdapter.add(ListRow(categoryHeader, categoryRowAdapter))
            } else {
                mRowsAdapter.add(mRowsAdapter.size() - 1, ListRow(categoryHeader, categoryRowAdapter))
            }
        }

        adapter = mRowsAdapter
    }

    private fun setupEventListeners(activity: Activity) {
        setOnSearchClickedListener { activity.startSearchActivity() }

        setOnItemViewSelectedListener { _, item, _, _ ->
            if (item is Video) {
                mBackgroundUri = item.backgroundImageUrl
                startBackgroundTimer()
            }
        }

        setOnItemViewClickedListener { itemViewHolder, item, _, _ ->
            Log.d(TAG, "Item: $item")
            when (item) {
                is Video -> {
                    activity.startDetailsActivity(item, (itemViewHolder.view as ImageCardView).mainImageView)
                }
                is Category -> {
                    activity.startGridActivity(item)
                }
                is MenuCard -> {
                    item.onClickHandler()
                }
            }
        }
    }

    private fun updateBackground(uri: String?) {
        if (uri == null) {
            return mBackgroundTimer.cancel()
        }
        val width = mMetrics.widthPixels
        val height = mMetrics.heightPixels
        val glideOptions = RequestOptions()
                .centerCrop()
                .error(mDefaultBackground)
        Glide.with(this)
                .asBitmap()
                .load(uri)
                .apply(glideOptions)
                .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>(width, height) {
                    override fun onResourceReady(bitmap: Bitmap,
                                                 glideAnimation: Transition<in Bitmap>?) {
                        mBackgroundManager.setBitmap(bitmap)
                    }
                })
        mBackgroundTimer.cancel()
    }

    private fun startBackgroundTimer() {
        mBackgroundTimer.cancel()
        mBackgroundTimer = Timer()
        mBackgroundTimer.schedule(BACKGROUND_UPDATE_DELAY) {
            updateBackground(mBackgroundUri)
        }
    }

    private inner class GridItemPresenter : Presenter() {
        private var mDefaultCardImage: Drawable? = null
        private var sSelectedBackgroundColor: Int by Delegates.notNull()
        private var sDefaultBackgroundColor: Int by Delegates.notNull()
        private var sDefaultTextColor: Int by Delegates.notNull()
        private var sSelectedTextColor: Int by Delegates.notNull()

        override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
            sDefaultBackgroundColor = ContextCompat.getColor(parent.context, R.color.default_background)
            sSelectedBackgroundColor =
                    ContextCompat.getColor(parent.context, R.color.selected_background)
            mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.drawable.movie)
            sSelectedTextColor = ContextCompat.getColor(parent.context, R.color.selected_text)
            sDefaultTextColor = ContextCompat.getColor(parent.context, R.color.lb_basic_card_title_text_color)

            val view = object : ImageCardView(parent.context) {
                override fun setSelected(selected: Boolean) {
                    updateCardBackgroundColor(this, selected)
                    super.setSelected(selected)
                }
            }

            val width = parent.context.resources.getDimensionPixelSize(R.dimen.icon_card_width)
            val height = parent.context.resources.getDimensionPixelSize(R.dimen.icon_card_height)
            view.layoutParams = ViewGroup.LayoutParams(width, height)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.setMainImageDimensions(width, width)
            view.setMainImageScaleType(ImageView.ScaleType.FIT_CENTER)
            view.setBackgroundColor(ContextCompat.getColor(parent.context, R.color.default_background))
            return Presenter.ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
            val card = viewHolder.view as ImageCardView
            card.titleText = (item as MenuCard).label
            card.mainImage = requireContext().getDrawable(item.imageRes)
        }

        override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
            Log.d(TAG, "onUnbindViewHolder")
            val cardView = viewHolder.view as ImageCardView
            // Remove references to images so that the garbage collector can free up memory
            cardView.badgeImage = null
            cardView.mainImage = null
        }

        private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
            val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
            // Both background colors should be set because the view's background is temporarily visible
            // during animations.
            view.setBackgroundColor(color)
            view.setInfoAreaBackgroundColor(color)

            val textColor = if (selected) sSelectedTextColor else sDefaultTextColor
            view.findViewById<TextView>(R.id.title_text).setTextColor(textColor)
        }
    }

    companion object {
        private const val TAG = "MainFragment"

        private const val BACKGROUND_UPDATE_DELAY = 300L
    }
}

// TODO: render shortDescription in cards

data class MenuCard (
        val label : String,
        val imageRes: Int,
        val onClickHandler: () -> Unit
)