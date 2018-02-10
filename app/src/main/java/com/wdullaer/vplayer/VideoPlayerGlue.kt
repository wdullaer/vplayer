/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build
import android.support.v17.leanback.media.PlaybackTransportControlGlue
import android.support.v17.leanback.widget.Action
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.PlaybackControlsRow

import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import java.util.*

import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

/**
 * Manages customizing the actions in the [PlaybackControlsRow]. Adds and manages the
 * following actions to the primary and secondary controls:
 *
 *
 *  * [android.support.v17.leanback.widget.PlaybackControlsRow.PictureInPictureAction]
 *  * [android.support.v17.leanback.widget.PlaybackControlsRow.SkipPreviousAction]
 *  * [android.support.v17.leanback.widget.PlaybackControlsRow.SkipNextAction]
 *  * [android.support.v17.leanback.widget.PlaybackControlsRow.FastForwardAction]
 *  * [android.support.v17.leanback.widget.PlaybackControlsRow.RewindAction]
 *
 *
 * Note that the superclass, [PlaybackTransportControlGlue], manages the playback controls
 * row.
 */
class VideoPlayerGlue(
        context: Context,
        playerAdapter: LeanbackPlayerAdapter,
        private val mActionListener: OnActionClickedListener) : PlaybackTransportControlGlue<LeanbackPlayerAdapter>(context, playerAdapter) {

    private val mPipAction: PlaybackControlsRow.PictureInPictureAction
            = PlaybackControlsRow.PictureInPictureAction(context)
    private val mSkipPreviousAction: PlaybackControlsRow.SkipPreviousAction
            = PlaybackControlsRow.SkipPreviousAction(context)
    private val mSkipNextAction: PlaybackControlsRow.SkipNextAction
            = PlaybackControlsRow.SkipNextAction(context)
    private val mFastForwardAction: PlaybackControlsRow.FastForwardAction
            = PlaybackControlsRow.FastForwardAction(context)
    private val mRewindAction: PlaybackControlsRow.RewindAction
            = PlaybackControlsRow.RewindAction(context)
    private var mForwardMultiplier = 1
    private var mBackwardMultiplier = 1
    private var mResetMultiplierTask : TimerTask? = null

    /** Listens for when skip to next and previous actions have been dispatched.  */
    interface OnActionClickedListener {

        /** Skip to the previous item in the queue.  */
        fun onPrevious()

        /** Skip to the next item in the queue.  */
        fun onNext()

        /** Signals that playback has finished */
        fun onPlaybackFinished()
    }

    override fun onCreatePrimaryActions(adapter: ArrayObjectAdapter) {
        // Order matters, super.onCreatePrimaryActions() will create the play / pause action.
        // Will display as follows:
        // play/pause, previous, rewind, fast forward, next
        //   > /||      |<        <<        >>         >|
        super.onCreatePrimaryActions(adapter)
        adapter.add(mSkipPreviousAction)
        adapter.add(mRewindAction)
        adapter.add(mFastForwardAction)
        adapter.add(mSkipNextAction)
    }

    override fun onCreateSecondaryActions(adapter: ArrayObjectAdapter) {
        super.onCreateSecondaryActions(adapter)
        if (context.supportsPictureInPicture) {
            adapter.add(mPipAction)
        }
    }

    override fun onPlayCompleted() {
        super.onPlayCompleted()
        mActionListener.onPlaybackFinished()
    }

    override fun onActionClicked(action: Action) {
        if (shouldDispatchAction(action)) {
            return dispatchAction(action)
        }
        // Super class handles play/pause and delegates to abstract methods next()/previous().
        super.onActionClicked(action)
    }

    // Should dispatch actions that the super class does not supply callbacks for.
    private fun shouldDispatchAction(action: Action): Boolean {
        return (action === mRewindAction
                || action === mFastForwardAction
                || action === mPipAction)
    }

    private fun dispatchAction(action: Action) {
        // Primary actions are handled manually.
        when (action) {
            mRewindAction -> rewind()
            mFastForwardAction -> fastForward()
            mPipAction -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val params = PictureInPictureParams.Builder().build()
                    (context as Activity).enterPictureInPictureMode(params)
                }
            }
            is PlaybackControlsRow.MultiAction -> {
                action.nextIndex()
                // Notify adapter of action changes to handle secondary actions, such as,
                // thumbs up/down and repeat.
                notifyActionChanged(
                        action,
                        controlsRow.secondaryActionsAdapter as ArrayObjectAdapter
                )
            }
        }
    }

    private fun notifyActionChanged(action: PlaybackControlsRow.MultiAction, adapter: ArrayObjectAdapter) {
        val index = adapter.indexOf(action)
        if (index >= 0) {
            adapter.notifyArrayItemRangeChanged(index, 1)
        }
    }

    override fun next() {
        mActionListener.onNext()
    }

    override fun previous() {
        mActionListener.onPrevious()
    }

    /**
     * Skips backwards 10 seconds. Increases skip time exponentially when called in rapid
     * succession
     */
    private fun rewind() {
        var newPosition = currentPosition - TEN_SECONDS * mBackwardMultiplier

        mBackwardMultiplier *= 2
        mForwardMultiplier = 1
        mResetMultiplierTask?.cancel()
        mResetMultiplierTask = Timer().schedule(RESET_DELAY) { mBackwardMultiplier = 1 }

        newPosition = if (newPosition < 0) 0 else newPosition
        playerAdapter.seekTo(newPosition)
    }

    /**
     * Skips forward 10 seconds. Increases skip time exponentially when called in rapid
     * succession
     */
    private fun fastForward() {
        if (duration > -1) {
            var newPosition = currentPosition + TEN_SECONDS * mForwardMultiplier

            mForwardMultiplier *= 2
            mBackwardMultiplier = 1
            mResetMultiplierTask?.cancel()
            mResetMultiplierTask = Timer().schedule(RESET_DELAY) { mForwardMultiplier = 1 }

            newPosition = if (newPosition > duration) duration else newPosition
            playerAdapter.seekTo(newPosition)
        }
    }

    companion object {
        private val TEN_SECONDS = TimeUnit.SECONDS.toMillis(10)
        private val RESET_DELAY = TimeUnit.SECONDS.toMillis(1)
    }
}