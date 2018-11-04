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
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackGlue
import android.util.Log
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.drm.*
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import org.jsoup.Jsoup
import org.jsoup.parser.Parser


/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment() {
    companion object {
        private const val LICENSE_URL = "https://widevine-proxy.drm.technology/proxy"
    }

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
        val video = nActivity.intent.getParcelableExtra(DetailsActivity.VIDEO) as Playable
        // Create a simple ExoPlayer
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory()
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        val bandwidthMeter = DefaultBandwidthMeter()

        getDrmSessionManager(video) {
            mPlayer = ExoPlayerFactory.newSimpleInstance(
                    activity,
                    DefaultRenderersFactory(activity),
                    trackSelector,
                    DefaultLoadControl(),
                    it,
                    bandwidthMeter
            )

            mPlayer?.addListener(object : Player.EventListener {
                override fun onPlayerError(error: ExoPlaybackException?) {
                    Log.e("VRTExoPlayer", error.toString())
                }
            })

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
    }

    private fun getDrmSessionManager(video: Playable, callback: (DefaultDrmSessionManager<FrameworkMediaCrypto>?) -> Unit) {
        val drmKey = video.drmKey ?: return callback(null)
        val videoUrl = video.videoUrl ?: return callback(null)
        /*
         * TODO: use the DashMediaSource to extract the default KID
         * (see also https://github.com/google/ExoPlayer/issues/1376)
         * We could probably piggyback off the DashMediaSource loader here, but I don't feel
         * figuring out where I need inject my custom code (it's not even clear to me on first sight
         * whether the raw xml is available somewhere in there).
         * So we are fetching the manifest and parsing out the default_KID
         * (assuming it is the same for all AdaptationSets, which seems true for now)
         */
        videoUrl.httpGet().responseString {_, _, result ->
            when (result) {
                is Result.Failure -> {
                    Log.e("getDrmSessionManager", "Failed to retrieve video manifest")
                    Log.e("getDrmSessionManager", result.error.toString())
                    callback(null)
                }
                is Result.Success -> {
                    val kid = Jsoup.parse(result.get(), "https://www.vrt.be/", Parser.xmlParser())
                            .select("ContentProtection[cenc:default_KID]")
                            .first()
                            .attr("cenc:default_KID")

                    val drmCallback = VualtoHttpMediaDrmCallback(
                            LICENSE_URL,
                            DefaultHttpDataSourceFactory("user-agent"),
                            drmKey,
                            kid
                            //"EF4DAA3C-9033-3621-E411-5FF1CA5BFBDA"
                    )
                    callback(DefaultDrmSessionManager.newWidevineInstance(
                            drmCallback,
                            hashMapOf("Content-Type" to "text/plain;charset=UTF-8")
                    ))
                }
            }
        }
    }

    private fun releasePlayer() {
        if (mPlayer != null) {
            mPlayer?.release()
            mPlayer = null
            mPlayerGlue = null
        }
    }

    private fun play(video: Playable, bandwidthMeter: DefaultBandwidthMeter) {
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