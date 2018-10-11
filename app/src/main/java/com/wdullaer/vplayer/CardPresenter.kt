/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.graphics.drawable.Drawable
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlin.properties.Delegates

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an ImageCardView.
 */
class CardPresenter(val size : CardSize = CardSize.LARGE) : Presenter() {
    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()
    private var sDefaultTextColor: Int by Delegates.notNull()
    private var sSelectedTextColor: Int by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        Log.d(TAG, "onCreateViewHolder")

        sDefaultBackgroundColor = ContextCompat.getColor(parent.context, R.color.default_background)
        sSelectedBackgroundColor =
                ContextCompat.getColor(parent.context, R.color.selected_background)
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.drawable.movie)
        sSelectedTextColor = ContextCompat.getColor(parent.context, R.color.selected_text)
        sDefaultTextColor = ContextCompat.getColor(parent.context, R.color.lb_basic_card_title_text_color)

        val cardView = object : ImageCardView(parent.context) {
            override fun setSelected(selected: Boolean) {
                updateCardBackgroundColor(this, selected)
                super.setSelected(selected)
            }
        }

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        updateCardBackgroundColor(cardView, false)
        return Presenter.ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val cardView = viewHolder.view as ImageCardView
        val image : String?
        val title : String
        val content : String

        Log.d(TAG, "onBindViewHolder")
        when (item) {
            is Video -> {
                image = item.cardImageUrl
                title = item.title ?: ""
                content = item.shortDescription ?: ""
            }
            is Category -> {
                image = item.cardImageUrl
                title = item.name
                content = ""
            }
            else -> {
                image = null
                title = ""
                content = ""
            }
        }

        image?.let {
            cardView.titleText = title
            cardView.contentText = content
            val res = viewHolder.view.resources
            if (size == CardSize.LARGE) {
                cardView.setMainImageDimensions(
                        res.getDimensionPixelOffset(R.dimen.image_card_width),
                        res.getDimensionPixelOffset(R.dimen.image_card_height)
                )
            } else {
                cardView.setMainImageDimensions(
                        res.getDimensionPixelOffset(R.dimen.image_card_width_small),
                        res.getDimensionPixelOffset(R.dimen.image_card_height_small)
                )
            }
            val glideOptions = RequestOptions()
                    .centerCrop()
                    .error(mDefaultCardImage)
            Glide.with(viewHolder.view.context)
                    .load(image)
                    .apply(glideOptions)
                    .into(cardView.mainImageView)
        }
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

    companion object {
        private const val TAG = "CardPresenter"
    }
}

enum class CardSize {
    SMALL, LARGE
}
