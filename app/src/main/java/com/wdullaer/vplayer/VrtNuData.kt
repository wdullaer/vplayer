/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.util.Log
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.lang.IllegalStateException
import java.util.UUID
import kotlin.concurrent.thread

/**
 * Utility functions which fetches data from VRT NU and returns it in an easily renderable shape
 *
 * Currently, this parses the HTML of the website because there is no API.
 *
 * In the future we might move this parsing to a separate service and just fetch json here
 * This will improve the update process (it's easier to update a single API service than push an
 * update to the install base when VRT updates their website and breaks the parsing)
 *
 * Created by wdullaer on 8/10/17.
 */

const val VRT_BASE_PATH = "https://www.vrt.be"
const val VRT_API_PATH = "https://vrtnu-api.vrt.be"
const val HTML_MIME = "text/html"
const val JSON_MIME = "application/json"

const val VRT_TOKEN_PATH = "https://media-services-public.vrt.be/vualto-video-aggregator-web/rest/external/v1/tokens"
val LIVE_PLAYLIST = listOf(
        LiveVideo(title = "Eén", cardImageRes = R.drawable.card_live_een, vualtoUrl = "https://media-services-public.vrt.be/vualto-video-aggregator-web/rest/external/v1/videos/vualto_een_geo"),
        LiveVideo(title = "Canvas", cardImageRes = R.drawable.card_live_canvas, vualtoUrl = "https://media-services-public.vrt.be/vualto-video-aggregator-web/rest/external/v1/videos/vualto_canvas_geo"),
        LiveVideo(title = "Ketnet", cardImageRes = R.drawable.card_live_ketnet, vualtoUrl = "https://media-services-public.vrt.be/vualto-video-aggregator-web/rest/external/v1/videos/vualto_ketnet_geo"),
        LiveVideo(title = "Sporza", cardImageRes = R.drawable.card_live_sporza, vualtoUrl = "https://media-services-public.vrt.be/vualto-video-aggregator-web/rest/external/v1/videos/vualto_sporza_geo")
)

fun getLandingPage(defaultTitle: String, callback : (Exception?, List<Playlist>) -> Unit): Request {
    fun parseLists(doc : Element) : Playlist {
        val title = doc.select("h2").first()?.text()?.trim()?.capitalize()
        val data = doc.select("li").map { parseVideo(it) }
        return Playlist(title ?: defaultTitle, data)
    }

    val endpoint = "$VRT_BASE_PATH/vrtnu"
    Log.i("getLandingpage", "Fetching data from $endpoint")
    return endpoint.httpGet().header("Accept" to HTML_MIME).responseString {_, _, result ->
        when (result) {
            is Result.Success -> {
                try {
                    Jsoup
                            .parse(result.get()).select(".list")
                            .map(::parseLists)
                            .filter { it.data.isNotEmpty() }
                } catch (e : Exception) {
                    Log.e("getLandingpage", "Failed to parse landingpage HTML")
                    Log.e("getLandingpage", e.toString())
                    callback(ParserException(e, PARSE_LANDINGPAGE_ERROR), listOf())
                    null
                }?.let { callback(null, it) }
            }
            is Result.Failure -> {
                Log.e("getLandingpage", "Failed to retrieve data from $endpoint")
                Log.e("getLandingpage", result.error.toString())
                callback(NetworkException(result.error), listOf())
            }
        }
    }
}

fun getCategories(callback : (Exception?, List<Category>) -> Unit) {
    val endpoint = "$VRT_BASE_PATH/vrtnu/categorieen"
    Log.i("getCategories", "Fetching data from $endpoint")
    endpoint.httpGet().header("Accept" to HTML_MIME).responseString {_, _, result ->
        when (result) {
            is Result.Success -> {
                try {
                    Jsoup
                            .parse(result.get())
                            .select("li.vrtlist__item--grid")
                            .map(::parseCategory)
                } catch (e : Exception) {
                    Log.e("getCategories", "Failed to parse categories HTML")
                    Log.e("getCategories", e.toString())
                    callback(ParserException(e, PARSE_CATEGORIES_ERROR), listOf())
                    null
                }?.let { callback(null, it) }
            }
            is Result.Failure -> {
                Log.e("getCategories", "Failed to retrieve data from $endpoint")
                Log.e("getCategories", result.error.toString())
                callback(NetworkException(result.error), listOf())
            }
        }
    }
}

fun getMoviesByCategory(category : Category, callback : (Exception?, List<Video>) -> Unit) {
    val endpoint = "https://search.vrt.be/suggest?facets[categories]=${category.name.toLowerCase()}"
    Log.i("getMoviesByCategorie", "Fetching data from $endpoint")

    endpoint.httpGet().header("Accept" to JSON_MIME).responseJson { _, _, result ->
        when (result) {
            is Result.Success -> {
                try {
                    result.get()
                            .array()
                            .map { suggestJsonToVideo(it as JSONObject, category.name) }
                            .toList()
                } catch (e : Exception) {
                    Log.e("getMoviesByCategory", "Failed to parse search json")
                    Log.e("getMoviesByCategory", e.toString())
                    callback(ParserException(e, SEARCH_JSON_TO_VIDEO_ERROR), listOf())
                    null
                }?.let { callback(null, it) }
            }
            is Result.Failure -> {
                Log.e("getMoviesByCategory", "Failed to retrieve data from $endpoint")
                Log.e("getMoviesByCategory", result.error.toString())
                callback(NetworkException(result.error), listOf())
            }
        }
    }
}

fun getVideoDetails(video: Video, cookie: String, callback: (Exception?, Video) -> Unit) {
    val detailsUrl = video.detailsUrl ?: return callback(IllegalStateException("Video should have a detailsUrl"), video)

    getContentsLink(detailsUrl).httpGet().responseJson { _, _, result ->
        when (result) {
            is Result.Failure -> {
                Log.e("getVideoDetails", "Failed to retrieve data from $detailsUrl")
                Log.e("getVideoDetails", result.error.toString())
                callback(NetworkException(result.error), video)
            }
            is Result.Success -> {
                try {
                    contentJsonToVideo(result.get().obj())
                } catch (e: Exception) {
                    Log.e("getVideoDetails", "Failed to parse content json")
                    Log.e("getVideoDetails", e.toString())
                    callback(ParserException(e, CONTENT_JSON_TO_VIDEO_ERROR), video)
                    null
                }?.let {
                    getRelatedVideos(it) { error, playlist ->
                        it.relatedVideos = playlist
                        getVideoUrl(it, cookie) { error2, videoUrl, drmKey ->
                            it.videoUrl = videoUrl
                            it.drmKey = drmKey
                            callback(error2 ?: error, it)
                        }
                    }
                }
            }
        }
    }
}

fun getRelatedVideos(video: Video, callback: (Exception?, List<Playlist>) -> Unit) {
    val programName = video.programTitle ?: return callback(IllegalStateException("Cannot fetch related videos when programTitle is null"), listOf())
    "$VRT_API_PATH/search?q=$programName".httpGet().responseJson { _, _, result ->
        when (result) {
            is Result.Failure -> {
                Log.e("getRelatedVideos", "Failed to retrieve data from $VRT_API_PATH")
                Log.e("getRelatedVideos", result.error.toString())
                callback(NetworkException(result.error), listOf())
            }
            is Result.Success -> {
                try {
                    result.get().obj()
                            .getJSONArray("results")
                            .map { contentJsonToVideo(it as JSONObject) }
                            .filterNot { it.publicationId == video.publicationId }
                            .toList()
                } catch (e: Exception) {
                    Log.e("getRelatedVideos", "Failed to parse json from $VRT_API_PATH")
                    Log.e("getRelatedVideos", e.toString())
                    callback(ParserException(e, CONTENT_JSON_TO_VIDEO_ERROR), listOf())
                    null
                }?.let {
                    callback(null, listOf(Playlist(data = it)))
                }
            }
        }
    }
}

fun getVrtToken(cookie: String, callback: (Exception?, String) -> Unit) {
    VRT_TOKEN_PATH.httpPost()
            .header(mapOf("Accept" to JSON_MIME, "Cookie" to cookie, "Content-Type" to JSON_MIME))
            .responseJson { _, _, result ->
                when (result) {
                    is Result.Success -> {
                        try {
                            result.get().obj().getString("vrtPlayerToken")
                        } catch (e: Exception) {
                            callback(ParserException(e, VRT_TOKEN_ERROR), "")
                            null
                        }?.let { callback(null, it) }
                    }
                    is Result.Failure -> {
                        Log.e("getVrtToken", "Failed to retrieve vrtToken")
                        Log.e("getVrtToken", result.error.toString())
                        callback(NetworkException(result.error), "")
                    }
                }
            }
}

fun getVideoUrl(video: Playable, cookie: String, callback: (Exception?, String?, String?) -> Unit) {
    getVrtToken(cookie) { error, token ->
        if (error != null) {
            callback(error, null, null)
        }
        else {
            video.vualtoUrl.httpGet(listOf("vrtPlayerToken" to token)).responseJson { _, _, result ->
                when (result) {
                    is Result.Success -> {
                        val resObj = result.get().obj()
                        try {
                            // TODO: make this a function we can test
                            val drmKey = if (resObj.isNull("drm")) null else resObj.getString("drm")
                            val targetUrl = resObj
                                    .getJSONArray("targetUrls")
                                    .map { it as JSONObject }
                                    .filter { it.getString("type") == "mpeg_dash" }
                                    .first().getString("url")
                            callback(null, targetUrl, drmKey)
                        } catch (e: Exception) {
                            callback(ParserException(e, STREAM_JSON_TO_VIDEO_ERROR), null, null)
                        }
                    }
                    is Result.Failure -> {
                        Log.e("getVideoUrl", "Failed to retrieve video target URL")
                        Log.e("getVideoUrl", result.error.toString())
                        callback(NetworkException(result.error), null, null)
                    }
                }
            }
        }
    }
}

fun getRecommendations(callback: (Exception?, Playlist) -> Unit): Request {
    // The VRTNU landingpage kind of acts as a recommendation service
    // We'll return the first 2 lists of whatever is on the landingpage
    return getLandingPage("Recommendations") { error, playlists ->
        // The http library runs its callbacks on the UI thread, but recommendations should be
        // fetched on a background thread. So we have to fork a new thread before doing anything else
        if (playlists.size < 3) callback(error, Playlist())
        else thread {
            val result = playlists.subList(0, 2)
                    .reduce { acc, playlist ->  Playlist(acc.title, acc.data.plus(playlist.data.map(::getVideoDetailsSync))) }
            callback(error, result)
        }
    }
}

fun enrichVideo(video : Video, cookie : String, callback: (Exception?) -> Unit) {
    getVideoDetails(video, cookie) { error, result ->
        result.copyInto(video)
        callback(error)
    }
}

fun searchVideo (query : String, callback : (Exception?, Playlist) -> Unit) {
    if (query == "") {
        callback(null, Playlist())
    }
    val endpoint = "$VRT_API_PATH/suggest?i=video&q=$query"
    endpoint.httpGet().header("Accept" to JSON_MIME).responseJson { _, _, result ->
        when (result) {
            is Result.Success -> {
                try {
                    result.get()
                            .array()
                            .map { it as JSONObject }
                            .map { suggestJsonToVideo(it) }
                            .toList()
                } catch (e : Exception) {
                    Log.e("searchVideo", "Failed to parse search json")
                    Log.e("searchVideo", e.toString())
                    callback(ParserException(e, SEARCH_JSON_TO_VIDEO_ERROR), Playlist())
                    null
                }?.let { callback(null, Playlist(query, it)) }
            }
            is Result.Failure -> {
                Log.e("searchVideo", "Failed to retrieve data from $endpoint")
                Log.e("searchVideo", result.error.toString())
                callback(NetworkException(result.error), Playlist())
            }
        }
    }
}

// searching for the pubId seems to always return exactly one hit: the video with that ID
// Given that these are UUIDs it should be relatively safe to assume that this will always be the case
fun getVideoByPubId (id : String, callback : (Exception?, Video) -> Unit) {
    if (id == "") callback(null, Video())
    val query = "$VRT_API_PATH/search?q=$id"
    query.httpGet().header("Accept" to JSON_MIME).responseJson {_, _, result ->
        when (result) {
            is Result.Success -> {
                val matches = result.get()
                        .obj()
                        .getJSONArray("results")
                if (matches.length() == 0) {
                    Log.w("getvideoByPubId", "Could not find video with id=$id")
                    callback(ParserException("Could not find video with id=$id", 1001), Video())
                } else {
                    callback(null, contentJsonToVideo(matches.getJSONObject(0)))
                }
            }
            is Result.Failure -> {
                Log.e("getVideoByPubId", "Failed to retrieve data from $query")
                Log.e("getVideoByPubId", result.error.toString())
                callback(NetworkException(result.error), Video())
            }
        }
    }
}

fun searchVideoSync (query : String) : Playlist {
    if (query == "") return Playlist()

    val endpoint = "$VRT_API_PATH/suggest?i=video&q=$query"
    val result = endpoint.httpGet().header("Accept" to JSON_MIME).responseJson().third

    return when (result) {
        is Result.Success -> {
            val videos = result.get()
                    .array()
                    .map {it as JSONObject}
                    .map { suggestJsonToVideo(it) }
                    .map { getVideoDetailsSync(it) }
                    .toList()
            Playlist(query, videos)
        }
        is Result.Failure -> Playlist()
    }
}

fun getVideoDetailsSync (video : Video) : Video {
    val detailsUrl = video.detailsUrl ?: return video
    val content = getContentsLink(detailsUrl)
            .httpGet()
            .header("Accept" to JSON_MIME)
            .responseJson()
            .third

    return when (content) {
        is Result.Success -> {
            try {
                    contentJsonToVideo(content.get().obj())
                } catch (e : Exception) {
                    Log.e("getVideoDetailsSync", e.toString())
                    null
                }?.copyInto(video)
            video
        }
        is Result.Failure -> video
    }
}

fun enrichLiveVideo (video : LiveVideo, cookie : String, callback : (error : Exception?, result : LiveVideo) -> Unit) {
    getVideoUrl(video, cookie) { error, targetUrl, drmKey ->
        if (error != null) {
            callback(error, video)
        } else {
            video.videoUrl = targetUrl
            video.drmKey = drmKey
            callback(null, video)
        }
    }
}

private fun parseCategory (doc : Element) : Category {
    return Category(
            name = doc.select("h2.tile__title").first().text(),
            cardImageUrl = parseSrcSet(doc.select("div.tile__image").first().select("img").first().attr("srcset")),
            link = toAbsoluteUrl(doc.select("a.tile--category").first().attr("href"))
    )
}

private fun parseVideo (doc : Element, category : String = "") : Video {
    fun getDescription (input : Element) : String {
        var description = input.select("div.tile__description").first()?.children()?.first()?.text()
        description = description ?: input.select("div.tile__description").first()?.text()
        description = description ?: input.select("div.tile__subtitle").first()?.text()
        return description ?: ""
    }

    fun getCardImageUrl (input : Element) : String? {
        var imageUrl = parseSrcSet(input.select("div.tile__image").first()?.select("img")?.first()?.attr("srcset"))
        imageUrl = imageUrl ?: getBackgroundImage(input.select("div.tile__image").first()?.attr("style"))
        imageUrl = imageUrl ?: input.select("div.tile__image").first()?.attr("data-responsive-image")?.ensurePrefix("https:")
        return imageUrl
    }

    return Video(
            id = UUID.randomUUID().mostSignificantBits,
            title = doc.select("h3.tile__title").first().text(),
            description = getDescription(doc),
            shortDescription = getDescription(doc),
            cardImageUrl = getCardImageUrl(doc),
            category = category,
            detailsUrl = toAbsoluteUrl(doc.select("a.tile")?.first()?.attr("href"))
    )
}

private fun suggestJsonToVideo (json : JSONObject, category: String = "") : Video {
    return Video(
            id = UUID.randomUUID().mostSignificantBits,
            title = json.getString("title"),
            description = sanitizeText(json.getString("description")),
            shortDescription = sanitizeText(json.getString("description")),
            detailsUrl = toAbsoluteUrl(json.getString("targetUrl")),
            cardImageUrl = toAbsoluteUrl(json.getString("thumbnail")),
            category = category
    )
}

/**
 * Function to parse the results of the video content.json and the results of the search API
 * The json payloads are not exactly the same, but similar enough that we can roll it in one function
 */
private fun contentJsonToVideo (json : JSONObject) : Video {
    val backgroundImageUrl = when {
        json.optString("programImageUrl", "") != "" -> json.getString("programImageUrl")
        else -> null
    }
    val cardImageUrl = when {
        json.optString("assetImage", "") != "" -> json.getString("assetImage")
        json.optString("videoThumbnailUrl", "") != "" -> json.getString("videoThumbnailUrl")
        else -> null
    }
    val duration = if (json.getLong("duration") < 3600) {
        json.getLong("duration") * 60 * 1000 // Durations are given in minutes in the search API
    } else {
        json.getLong("duration") // Durations are given in ms in the content.json
    }
    return Video(
            id = json.getLong("whatsonId"),
            title = json.getString("title"),
            programTitle = json.optString("program", null) ?: json.getString("programTitle"),
            description = sanitizeText(json.getString("description")),
            shortDescription =  json.getString("shortDescription"),
            detailsUrl = toAbsoluteUrl(json.getString("url")),
            cardImageUrl = toAbsoluteUrl(cardImageUrl),
            category = json.optJSONArray("categories")?.optString(0),
            backgroundImageUrl = toAbsoluteUrl(backgroundImageUrl),
            duration = duration,
            publicationId = json.getString("publicationId"),
            videoId = json.getString("videoId")
    )
}

private fun parseSrcSet(srcSet : String?) : String? {
    if (srcSet == null) return null
    val imageUrl = srcSet
            .split(',')
            .first { it.endsWith("2x") }
            .dropLast(2)
            .trim()
    return toAbsoluteUrl(imageUrl)
}

private fun getContentsLink(link : String) : String {
    return (if (link.last() == '/') link.dropLast(1) else link).plus(".content.json")
}

private fun sanitizeText(text : String) : String {
    val body = Jsoup.parseBodyFragment(text)
            .body()
    return if (body.select("p").isEmpty()) body.text()
    else body.select("p").joinToString("\n") { it.text() }
}

private fun toAbsoluteUrl(url : String?) : String {
    return when {
        url == null -> ""
        url.startsWith("http://") -> url
        url.startsWith("https://") -> url
        url.startsWith("//") -> "https:$url"
        url.startsWith("/") -> "$VRT_BASE_PATH$url"
        else -> {
            Log.w("VrtNuData", "$url does not appear to be a url")
            url
        }
    }
}

/**
 * Simple function which extracts the value of `background-image` in a css style attribute
 * Returns null if the input is null or no `background-image` property is found.
 * It assumes that the value is wrapped in `url()` and will remove those
 *
 * @param css A string containing a css style attribute
 * @returns The value of the `background-image` attribute (stripped of `url()`) or null if no such
 *          attribute could be found
 */
private fun getBackgroundImage(css : String?) : String? {
    Log.i("getBackgroundImage", css)
    return css?.split(";")
            ?.asSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("background-image") }
            ?.removePrefix("background-image:")
            ?.trim()
            ?.drop(5)
            ?.dropLast(2)
}
