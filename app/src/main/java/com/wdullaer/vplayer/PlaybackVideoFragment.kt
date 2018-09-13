/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.net.Uri
import android.os.Build
import android.support.v17.leanback.app.VideoSupportFragment
import android.support.v17.leanback.app.VideoSupportFragmentGlueHost
import android.support.v17.leanback.media.PlaybackGlue
import android.util.Log
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory


/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment() {

    private var mPlayerGlue : VideoPlayerGlue? = null
    private var mPlayer : SimpleExoPlayer? = null
    private val updateDelay = 16

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            mPlayer ?: initializePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            releasePlayer()
        }
    }

    override fun onPause() {
        super.onPause()

        // Do not pause if we are running in Picture in Picture
        val isInPipMode = mPlayerGlue?.isPlaying ?: false
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && activity?.supportsPictureInPicture ?: false
                && activity?.isInPictureInPictureMode ?: false
        if (!isInPipMode) {
            mPlayerGlue?.pause()
        }

        // Release resources here for older versions of android
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            releasePlayer()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

        mPlayerGlue?.isControlsOverlayAutoHideEnabled = !isInPictureInPictureMode

        if (isInPictureInPictureMode) {
            isControlsOverlayAutoHideEnabled = true
            hideControlsOverlay(false)
        }
    }

    private fun initializePlayer() {
        // No point showing a player if we don't have an activity to show it on
        val nActivity = activity ?: return
        // Parse the intent for the target video
        val video = nActivity.intent.getParcelableExtra(DetailsActivity.VIDEO) as Video
        // Create a simple ExoPlayer
        val bandwidthMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        mPlayer = ExoPlayerFactory.newSimpleInstance(activity, trackSelector)

        // Create a player adapter (TODO: figure out what this actually does)
        val playerAdapter = LeanbackPlayerAdapter(activity, mPlayer, updateDelay)
        val playlistActionListener = object : VideoPlayerGlue.OnActionClickedListener {
            override fun onPrevious() {
                Log.d("PlaybackVideoFragment", "Playlist not implemented")
            }

            override fun onNext() {
                Log.d("PlaybackVideoFragment", "Playlist not implemented")
            }

            override fun onPlaybackFinished() {
                Log.d("PlaybackVideoFragment", "Play next in playlist or jump back to video details")
                nActivity.finish()
            }
        }

        // Create the glue
        mPlayerGlue = VideoPlayerGlue(nActivity, playerAdapter, playlistActionListener)
        mPlayerGlue?.host = VideoSupportFragmentGlueHost(this)
        mPlayerGlue?.addPlayerCallback(object : PlaybackGlue.PlayerCallback() {
            override fun onPreparedStateChanged(glue : PlaybackGlue) {
                super.onPreparedStateChanged(glue)
                if (glue.isPrepared) {
                    glue.removePlayerCallback(this)
                    glue.play()
                }
            }
        })

        play(video, bandwidthMeter)
    }

    private fun releasePlayer() {
        if (mPlayer != null) {
            mPlayer?.release()
            mPlayer = null
            mPlayerGlue = null
        }
    }

    private fun play(video: Video, bandwidthMeter: DefaultBandwidthMeter) {
        val nActivity = activity ?: return
        mPlayerGlue?.title = video.title
        mPlayerGlue?.subtitle = video.description

        val dataSourceFactory = DefaultHttpDataSourceFactory(
                nActivity.exoPlayerUserAgent,
                bandwidthMeter
        )
        val dashMediaSource = DashMediaSource.Factory(
                DefaultDashChunkSource.Factory(dataSourceFactory),
                dataSourceFactory
        ).createMediaSource(Uri.parse(video.videoUrl))

        mPlayer?.prepare(dashMediaSource)

        mPlayerGlue?.play()
    }
}