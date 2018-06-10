/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer.recommendation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/*
 * This class extends BroadcastReceiver and publishes Recommendations when received.
 */
class RecommendationReceiver : BroadcastReceiver() {
    val INITIAL_DELAY = 5000L

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleRecommendationUpdate(context)
        }
    }

    private fun scheduleRecommendationUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val recommendationIntent = Intent(context, RecommendationService::class.java)
        val alarmIntent = PendingIntent.getService(context, 0, recommendationIntent, 0)

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, INITIAL_DELAY,
                AlarmManager.INTERVAL_HALF_HOUR, alarmIntent)
    }
}