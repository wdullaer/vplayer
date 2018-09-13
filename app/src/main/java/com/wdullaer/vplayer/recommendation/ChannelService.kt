package com.wdullaer.vplayer.recommendation

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.support.media.tv.Channel
import android.support.media.tv.ChannelLogoUtils
import android.support.media.tv.PreviewProgram
import android.support.media.tv.TvContractCompat
import android.util.Log
import com.github.kittinunf.fuel.core.Request
import com.wdullaer.vplayer.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ChannelService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false

        thread {
            val storedChannelId = this.getSharedPreferences(VPLAYER_PREFERENCE_ROOT, Context.MODE_PRIVATE)
                    .getLong(resources.getString(R.string.pref_default_channel_id_key), NO_CHANNEL)
            val registeredChannelId = getRegisteredChannels(this)

            if (registeredChannelId != NO_CHANNEL && registeredChannelId != storedChannelId) {
                Log.i("ChannelService", "Channel does not match stored channel. Deleting existing Channel.")
                this.contentResolver.delete(
                        TvContractCompat.buildChannelUri(registeredChannelId),
                        null,
                        null
                )
            } else if (registeredChannelId == NO_CHANNEL || registeredChannelId != storedChannelId) {
                Log.i("ChannelService", "Registering new Channel")
                val channelValues = Channel.Builder()
                        .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                        .setDisplayName(applicationContext.getString(R.string.app_name))
                        .setAppLinkIntent(Intent(this, MainActivity::class.java))
                        .build()
                        .toContentValues()

                val channelUri = this.contentResolver.insert(
                        TvContractCompat.Channels.CONTENT_URI,
                        channelValues
                )
                val channelId = ContentUris.parseId(channelUri)

                val logo = this.vectorToBitmap(R.drawable.vplayer_logo)
                logo?.let { ChannelLogoUtils.storeChannelLogo(this, channelId, it) }

                TvContractCompat.requestChannelBrowsable(this, channelId)

                this.getSharedPreferences(VPLAYER_PREFERENCE_ROOT, Context.MODE_PRIVATE).edit()
                        .putLong(resources.getString(R.string.pref_default_channel_id_key), channelId)
                        .apply()
            }

            // Schedule job to populate channel with programs
            Log.i("ChannelService", "Scheduling ChannelProgramService")
            val bundle = PersistableBundle()
            bundle.putLong(TvContractCompat.EXTRA_CHANNEL_ID, registeredChannelId)
            val jobInfo = JobInfo.Builder(CHANNEL_PROGRAM_JOB_ID, ComponentName(this, ChannelProgramService::class.java))
                    .setPeriodic(TimeUnit.MINUTES.toMillis(30))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(bundle)
                    .build()
            (this.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).schedule(jobInfo)

            jobFinished(params, false)
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // Potentially stop any long running operations here
        return false
    }

    private fun getRegisteredChannels(context: Context): Long {
        val cursor = context.contentResolver.query(
                TvContractCompat.Channels.CONTENT_URI,
                arrayOf(TvContractCompat.Channels._ID, TvContractCompat.Channels.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
        )
        // TODO: return all channels
        cursor?.use { return if (it.moveToFirst()) Channel.fromCursor(it).id else NO_CHANNEL }
        return NO_CHANNEL
    }
}

class ChannelProgramService : JobService() {
    private var runningRequest: Request? = null

    override fun onStartJob(params: JobParameters): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false

        val channelId = params.extras.getLong(TvContractCompat.EXTRA_CHANNEL_ID, NO_CHANNEL)
        if (channelId == NO_CHANNEL) return false

        thread {
            val cursor = this.contentResolver.query(
                    TvContractCompat.buildChannelUri(channelId),
                    null,
                    null,
                    null,
                    null
            )
            val needsReschedule = cursor?.use {
                if (it.moveToFirst()) {
                    val channel = Channel.fromCursor(it)
                    if (channel.isBrowsable) updateChannelContent(this, channel.id)
                    else deleteChannelContent(this, channel.id)
                } else false
            } ?: false
            jobFinished(params, needsReschedule)
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        runningRequest?.let {
            it.cancel()
            runningRequest = null
            return true
        }
        return false
    }

    private fun deleteChannelContent(context: Context, channelId: Long): Boolean {
        Log.i("ChannelProgramService", "Deleting all programs in main Channel")
        val cursor = context.contentResolver.query(
                TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                null,
                null,
                null,
                null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val program : PreviewProgram = PreviewProgram.fromCursor(it)
                context.contentResolver.delete(
                        TvContractCompat.buildProgramUri(program.id),
                        null,
                        null
                )
            }
        }
        return false
    }

    private fun updateChannelContent(context: Context, channelId: Long): Boolean {
        Log.i("ChannelProgramService", "Updating programs in main Channel")
        val latch = CountDownLatch(1)
        var needsReschedule = false
        runningRequest = getRecommendations { error, playlist ->
            if (error != null) {
                Log.e("RecommendationService", "Failed to load vPlayer recommendations")
                latch.countDown()
                needsReschedule = true
                return@getRecommendations
            }

            val cursor = context.contentResolver.query(
                    TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                    null,
                    null,
                    null,
                    null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val position = it.position
                    val currentProgram = PreviewProgram.fromCursor(it)
                    if (position < playlist.data.size) {
                        context.contentResolver.update(
                                TvContractCompat.buildPreviewProgramUri(currentProgram.id),
                                buildProgram(channelId, playlist.data[position]).toContentValues(),
                                null,
                                null
                        )
                    } else {
                        context.contentResolver.delete(
                                TvContractCompat.buildPreviewProgramUri(currentProgram.id),
                                null,
                                null
                        )
                    }
                }
                for (i in it.count until playlist.data.size) {
                    context.contentResolver.insert(
                            TvContractCompat.PreviewPrograms.CONTENT_URI,
                            buildProgram(channelId, playlist.data[i]).toContentValues()
                    )
                }
            }
            latch.countDown()
        }
        latch.await()
        return needsReschedule
    }

    private fun buildProgram(channelId: Long, video: Video): PreviewProgram {
        Log.d("ChannelProgramService", video.toString())
        return PreviewProgram.Builder()
                .setChannelId(channelId)
                .setType(TvContractCompat.PreviewProgramColumns.TYPE_CLIP)
                .setTitle(video.title)
                .setDescription(video.description)
                .setPosterArtUri(Uri.parse(video.cardImageUrl))
                .setIntent(this.getDetailsIntent(video))
                .setThumbnailUri(Uri.parse(video.cardImageUrl))
                .build()
    }
}

const val NO_CHANNEL = -1L