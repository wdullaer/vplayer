/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer.recommendation

import android.app.NotificationManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.preference.PreferenceManager
import android.support.app.recommendation.ContentRecommendation
import android.util.Log
import com.bumptech.glide.Glide
import com.github.kittinunf.fuel.core.Request
import com.wdullaer.vplayer.R
import com.wdullaer.vplayer.getDetailsIntent
import com.wdullaer.vplayer.getRecommendations

class RecommendationService : JobService() {
    private lateinit var notifManager : NotificationManager
    private var runningRequest : Request? = null

    override fun onCreate() {
        super.onCreate()
        notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartJob(jobParameters : JobParameters): Boolean {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (!sharedPrefs.getBoolean(getString(R.string.pref_enable_recommendations_key), true)) {
            notifManager.cancelAll()
            return false
        }

        runningRequest = getRecommendations { error, playlist ->
            runningRequest = null

            if (error != null) {
                Log.e("RecommendationService", "Failed to load vPlayer recommendations")
                jobFinished(jobParameters, false)
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
                                this.getDetailsIntent(it),
                                0,
                                null
                        )
                        .setContentImage(bitmap)
                        .build()
                        .getNotificationObject(this)

                Log.d("RecommendationService", "Recommending video \"${it.title}\"")
                notifManager.notify(it.id.hashCode(), notification)
            }

            jobFinished(jobParameters, false)
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return runningRequest?.let {
            it.cancel()
            runningRequest = null
            true
        } ?: false
    }
}