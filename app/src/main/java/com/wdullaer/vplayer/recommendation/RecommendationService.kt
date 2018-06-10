/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer.recommendation

import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.app.recommendation.ContentRecommendation
import android.util.Log
import com.bumptech.glide.Glide
import com.wdullaer.vplayer.DetailsActivity
import com.wdullaer.vplayer.R
import com.wdullaer.vplayer.Video
import com.wdullaer.vplayer.getRecommendations

class RecommendationService : IntentService("RecommendationService") {
    private lateinit var notifManager : NotificationManager

    override fun onCreate() {
        super.onCreate()
        notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onHandleIntent(intent: Intent?) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (!sharedPrefs.getBoolean(getString(R.string.pref_enable_recommendations_key), true)) {
            notifManager.cancelAll()
            return
        }

        getRecommendations { error, playlist ->
            if (error != null) {
                Log.e("RecommendationService", "Failed to load vPlayer recommendations")
                return@getRecommendations
            }

            val cardWidth = resources.getDimensionPixelSize(R.dimen.image_card_width)
            val cardHeight = resources.getDimensionPixelSize(R.dimen.image_card_height)
            val builder = ContentRecommendation.Builder().setBadgeIcon(R.drawable.vplayer_banner)

            playlist.data.forEach {
                val bitmap = Glide.with(application)
                        .asBitmap()
                        .load(it.cardImageUrl)
                        .submit(cardWidth, cardHeight)
                        .get()

                val notification = builder.setIdTag("Video-${it.id}")
                        .setTitle(it.title)
                        .setText(it.shortDescription)
                        .setContentIntentData(
                                ContentRecommendation.INTENT_TYPE_ACTIVITY,
                                getPendingIntent(it),
                                0,
                                null
                        )
                        .setContentImage(bitmap)
                        .build()
                        .getNotificationObject(applicationContext)

                Log.d("RecommendationService", "Recommending video \"${it.title}\"")
                notifManager.notify(it.id.hashCode(), notification)
            }
        }
    }

    private fun getPendingIntent(video: Video) : Intent {
        return Intent(this, DetailsActivity::class.java)
                .putExtra(DetailsActivity.VIDEO, video)
                .putExtra(DetailsActivity.NOTIFICATION, video.id.hashCode())
                .setAction(video.id.toString())
    }
}