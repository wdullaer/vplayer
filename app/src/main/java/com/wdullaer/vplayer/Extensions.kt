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
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityOptionsCompat
import android.view.View
import com.google.android.exoplayer2.util.Util
import org.json.JSONArray

/**
 * Defines useful extension functions and properties that would otherwise go into a Utils class
 *
 * Created by wdullaer on 12/10/17.
 */

/**
 * Returns if picture-in-picture (PIP) is supported by the system.
 */
val Context.supportsPictureInPicture: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && this.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

/**
 * A useragent string for use in an ExoPlayer instance
 */
val Context.exoPlayerUserAgent : String
    get() = Util.getUserAgent(this, "VideoPlayerGlue")

/**
 * Start the DetailsActivity for a given video with an animation
 */
fun Activity.startDetailsActivity(video : Video, transitionView : View) {
    val intent = Intent(this, DetailsActivity::class.java)
    intent.putExtra(DetailsActivity.VIDEO, video)

    val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            transitionView,
            DetailsActivity.SHARED_ELEMENT_NAME
    ).toBundle()
    this.startActivity(intent, bundle)
}

/**
 * Start the DetailsActivity for a given video without an animation
 */
fun Activity.startDetailsActivity(video : Video) {
    val intent = Intent(this, DetailsActivity::class.java)
    intent.putExtra(DetailsActivity.VIDEO, video)

    val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle()
    this.startActivity(intent, bundle)
}

/**
 * Start the VerticalGridActivity showing all videos in the given Category
 */
fun Activity.startGridActivity(category : Category) {
    val intent = Intent(this, com.wdullaer.vplayer.VerticalGridActivity::class.java)
    intent.putExtra(INTENT_CATEGORY, category)

    val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle()
    this.startActivity(intent, bundle)
}

/**
 * Start the ErrorActivity displaying some information to the user about what went wrong
 * TODO: pass in an Exception or error object
 */
fun Activity.startErrorActivity() {
    val intent = Intent(this, BrowseErrorActivity::class.java)
    val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle()
    startActivity(intent, bundle)
}

/**
 * Start the PlaybackActivity for the given Video
 */
fun Activity.startPlaybackActivity(video: Video) {
    val intent = Intent(this, PlaybackActivity::class.java)
    intent.putExtra(DetailsActivity.VIDEO, video)
    startActivity(intent)
}

/**
 * Start the SettingsActivity
 */
fun Activity.startSettingsActivity() {
    val intent = Intent(this, SettingsActivity::class.java)
    this.startActivity(intent)
}

/**
 * Start the AuthenticationActivity
 */
fun Activity.startAuthenticationActivity() {
    val intent = Intent(this, AuthenticationActivity::class.java)
    this.startActivity(intent)
}

/**
 * Start the SearchActivity
 */
fun Activity.startSearchActivity() {
    val intent = Intent(this, SearchActivity::class.java)
    this.startActivity(intent)
}

/**
 * JSONArray does not inherit from java collections
 * This adds the filter function that we would get if it did
 */
fun JSONArray.filter(predicate : (Any) -> Boolean) : List<Any> {
    return (0 until this.length())
            .filter { predicate(this.get(it)) }
            .map { this.get(it) }
}

/**
 * JSONArray does not inherit from java collections
 * This adds the map function
 */
fun <T> JSONArray.map(transform : (Any) -> T) : List<T> {
    return (0 until this.length())
            .map { transform(this.get(it)) }
}

fun Context.convertDpToPixel(dp: Int) : Int {
    val density = this.applicationContext.resources.displayMetrics.density
    return Math.round(dp.toFloat() * density)
}

const val INTENT_CATEGORY = "category"
