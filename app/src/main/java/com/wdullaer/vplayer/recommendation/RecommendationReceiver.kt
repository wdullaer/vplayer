/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer.recommendation

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.tvprovider.media.tv.TvContractCompat
import java.util.concurrent.TimeUnit

/*
 * This class extends BroadcastReceiver and publishes Recommendations when received.
 */
class RecommendationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleRecommendationUpdate(context, scheduler)
            scheduleChannelUpdate(context, scheduler)
        } else if (intent.action == TvContractCompat.ACTION_INITIALIZE_PROGRAMS) {
            scheduleChannelUpdate(context, scheduler)
        }
    }
}

fun scheduleRecommendationUpdate(context: Context, scheduler: JobScheduler) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return
    val jobInfo = JobInfo.Builder(RECOMMENDATION_JOB_ID, ComponentName(context, RecommendationService::class.java))
            .setPeriodic(TimeUnit.MINUTES.toMillis(30))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .build()
    scheduler.schedule(jobInfo)
}

fun scheduleChannelUpdate(context: Context, scheduler: JobScheduler) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val jobInfo = JobInfo.Builder(CHANNEL_JOB_ID, ComponentName(context, ChannelService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .build()
    scheduler.schedule(jobInfo)
}

const val RECOMMENDATION_JOB_ID = 1001
const val CHANNEL_JOB_ID = 1002
const val CHANNEL_PROGRAM_JOB_ID = 1003