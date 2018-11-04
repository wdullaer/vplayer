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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
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

fun FragmentActivity.showErrorFragment(e : ParserException) {
    val fragmentArgs = Bundle()
    fragmentArgs.putSerializable("error", e)
    val errorFragment = ErrorFragment()
    errorFragment.arguments = fragmentArgs
    supportFragmentManager.beginTransaction().add(R.id.main_browse_fragment, errorFragment).commit()
}

/**
 * Start the PlaybackActivity for the given Video
 */
fun Activity.startPlaybackActivity(video: Playable) {
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
 * Get an Intent to start the DetailsActivity showing a particular Video
 */
fun Context.getDetailsIntent(video: Video): Intent {
    return Intent(this, DetailsActivity::class.java)
            .putExtra(DetailsActivity.VIDEO, video)
            .putExtra(DetailsActivity.NOTIFICATION, video.id.hashCode())
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("content://com.wdullaer.vplayer/video/" + video.publicationId))
}

/**
 * JSONArray does not inherit from java collections
 * This adds the filter function that we would get if it did
 */
fun JSONArray.filter(predicate : (Any) -> Boolean) : Sequence<Any> {
    return generateSequence(0) { it + 1 }
            .take(this.length())
            .filter { predicate(this.get(it)) }
            .map { this.get(it) }
}

/**
 * JSONArray does not inherit from java collections
 * This adds the map function
 */
fun <T> JSONArray.map(transform : (Any) -> T) : Sequence<T> {
    return generateSequence(0) { it + 1 }
            .take(this.length())
            .map { transform(this.get(it)) }
}

fun String.ensurePrefix(prefix: String): String {
    if (this.startsWith(prefix)) return this
    return "$prefix$this"
}

fun Context.convertDpToPixel(dp: Int) : Int {
    val density = this.applicationContext.resources.displayMetrics.density
    return Math.round(dp.toFloat() * density)
}

fun Context.vectorToBitmap(resId: Int): Bitmap? {
    val drawable = (ContextCompat.getDrawable(this, resId) ?: return null) as? VectorDrawable
            ?: throw IllegalArgumentException("ResId should refer to a VectorDrawable")
    val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

const val INTENT_CATEGORY = "category"
