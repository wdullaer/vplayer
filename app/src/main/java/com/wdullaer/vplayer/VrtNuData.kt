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
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.UUID

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
const val HTML_MIME = "text/html"
const val JSON_MIME = "application/json"

fun getLandingPage(defaultTitle: String, callback : (List<Playlist>) -> Unit) {
    fun parseLists(doc : Element) : Playlist {
        val title = doc.select("h2.vrtlist__title").first()?.text()?.trim()?.capitalize()
        val data = doc.select("li.vrtlist__item").map { parseVideo(it) }
        return Playlist(title ?: defaultTitle, data)
    }

    val endpoint = "$VRT_BASE_PATH/vrtnu"
    Log.i("getLandingpage", "Fetching data from $endpoint")
    endpoint.httpGet().header("Accept" to HTML_MIME).responseString {_, _, result ->
        when (result) {
            is Result.Success -> {
                callback(Jsoup.parse(result.get()).select(".list").map(::parseLists))
            }
            is Result.Failure -> {
                Log.e("getLandingpage", "Failed to retrieve data from $endpoint")
                Log.e("getLandingpage", result.error.toString())
                callback(listOf())
            }
        }
    }
}

fun getCategories(callback : (List<Category>) -> Unit) {
    val endpoint = "$VRT_BASE_PATH/vrtnu/categorieen"
    Log.i("getCategories", "Fetching data from $endpoint")
    endpoint.httpGet().header("Accept" to HTML_MIME).responseString {_, _, result ->
        when (result) {
            is Result.Success -> {
                callback(Jsoup
                        .parse(result.get())
                        .select("li.vrtlist__item")
                        .map(::parseCategory)
                )
            }
            is Result.Failure -> {
                Log.e("getCategories", "Failed to retrieve data from $endpoint")
                Log.e("getCategories", result.error.toString())
                callback(listOf())
            }
        }
    }
}

fun getMoviesByCategory(category : Category, callback : (List<Video>) -> Unit) {
    val endpoint = "https://search.vrt.be/suggest?facets[categories]=${category.name.toLowerCase()}"
    Log.i("getMoviesByCategorie", "Fetching data from $endpoint")

    endpoint.httpGet().header("Accept" to JSON_MIME).responseJson { _, _, result ->
        when (result) {
            is Result.Success -> {
                callback(result.get().array()
                        .map { jsonToVideo(it as JSONObject, category.name) }
                )
            }
            is Result.Failure -> {
                Log.e("getMoviesByCategory", "Failed to retrieve data from $endpoint")
                Log.e("getMoviesByCategory", result.error.toString())
                callback(listOf())
            }
        }
    }
}

fun getVideoDetails(video : Video, cookie : String = "", callback : (Video) -> Unit) {
    fun fetchVideoUrls(clientid : String, videoid : String) {
        val endpoint = "https://mediazone.vrt.be/api/v1/$clientid/assets/$videoid"
        endpoint.httpGet().header("Accept" to JSON_MIME, "Cookie" to cookie).responseJson {_, _, result ->
            when (result) {
                is Result.Success -> {
                    val json = result.get().obj()
                    // TODO: validate payload before parsing
                    // TODO: parse related videos
                    val posterImage = toAbsoluteUrl(json.optString("posterImageUrl"))
                    callback(Video(
                            id = video.id,
                            title = json.getString("title"),
                            shortDescription = video.shortDescription,
                            description = json.getString("description"),
                            backgroundImageUrl = if (posterImage != "") posterImage else video.backgroundImageUrl,
                            cardImageUrl =  video.cardImageUrl,
                            detailsUrl = video.detailsUrl,
                            category = video.category,
                            relatedVideos = video.relatedVideos,
                            videoUrl = json.getJSONArray("targetUrls")
                                    .filter {(it as JSONObject).getString("type") == "MPEG_DASH"}
                                    .map { it as JSONObject }
                                    .first()
                                    .getString("url")
                    ))
                }
                // TODO: pass the error on so we can render a nice message to the user
                is Result.Failure -> {
                    Log.e("getVideoDetails", "Failed to retrieve data from $endpoint")
                    Log.e("getVideoDetails", result.error.toString())
                    callback(video)
                }
            }

        }
    }

    fun fetchVideoIds(detailsUrl : String) {
        // TODO: this request requires authentication. Add it
        getVideosLink(detailsUrl).httpGet().header("Accept" to JSON_MIME, "Cookie" to cookie).responseJson { _, _, result ->
            when (result) {
                is Result.Success -> {
                    val json = result.get().obj()
                    // TODO: validate payload before parsing
                    val key = json.keys().next()
                    val clientid = json.getJSONObject(key).getString("clientid")
                    val videoid = json.getJSONObject(key).getString("videoid")
                    fetchVideoUrls(clientid, videoid)
                }
            // TODO: pass the error on so we can render a nice message to the user
                is Result.Failure -> {
                    Log.e("getVideoDetails", "Failed to retrieve data from ${getVideosLink(detailsUrl)}")
                    Log.e("getVideoDetails", result.error.toString())
                    callback(video)
                }
            }
        }
    }

    fun fetchRelatedVideos(detailsUrl : String) {
        detailsUrl.httpGet().header("Accept" to HTML_MIME).responseString {_, _, result ->
            // If VRT provides an episode list, show these as related
//          val episodes = html.select("div.episodeslist")
//          if (!episodes.isEmpty()) relatedVideos.addAll(episodes.map(::parsePlaylist))
//
//          // If VRT provides a list of related videos add that as well
//          val related = html.select("div.list-inherited")
//          if (!related.isEmpty()) relatedVideos.add(parsePlaylist(related.first()))
            // TODO use https://search.vrt.be/suggest and https://search.vrt.be/search to
            // populate the related videos and other seasons playlists rather than parsing the
            // website
            video.relatedVideos = Jsoup.parse(result.get())
                    .select(".main-content")
                    .select("div.vrtlist")
                    .map(::parsePlaylist)
            fetchVideoIds(detailsUrl)
        }
    }

    // TODO: pass the error on so we can render a nice message to the user
    val detailsUrl = video.detailsUrl ?: return callback(video)
    Log.i("getVideoDetails", "Fetching data from $detailsUrl")
    getContentsLink(detailsUrl).httpGet().header("Accept" to JSON_MIME).responseJson {_, _, result ->
        when (result) {
            is Result.Success -> {
                val info = result.get().obj()
                video.title = info.getString("title")
                video.shortDescription = info.getString("shortDescription")
                video.description = sanitizeText(info.getString("description"))
                video.backgroundImageUrl = if (info.getString("assetImage") == "") {
                    toAbsoluteUrl(info.getString("programImageUrl"))
                } else {
                    toAbsoluteUrl(info.getString("assetImage"))
                }
                val newDetailsUrl = toAbsoluteUrl(info.getString("url"))
                video.detailsUrl = newDetailsUrl
                fetchRelatedVideos(newDetailsUrl)
            }
            is Result.Failure -> {
                Log.e("getVideoDetails", "Failed to retrieve data from $detailsUrl")
                Log.e("getVideoDetails", result.error.toString())
                callback(video)
            }
        }
    }
}

fun enrichVideo(video : Video, cookie : String, callback: () -> Unit) {
    getVideoDetails(video, cookie) {
        video.id = it.id
        video.title = it.title
        video.shortDescription = it.shortDescription
        video.description = it.description
        video.backgroundImageUrl = it.backgroundImageUrl
        video.cardImageUrl = it.cardImageUrl
        video.detailsUrl = it.cardImageUrl
        video.category = it.category
        video.videoUrl = it.videoUrl
        video.relatedVideos = it.relatedVideos
        callback()
    }
}

fun searchVideo (query : String, callback : (Playlist) -> Unit) {
    if (query == "") {
        callback(Playlist())
    }
    val endpoint = "https://search.vrt.be/suggest?i=video&q=$query"
    endpoint.httpGet().header("Accept" to JSON_MIME).responseJson { _, _, result ->
        when (result) {
            is Result.Success -> {
                val videos = result.get()
                        .array()
                        .map { it as JSONObject }
                        .map { jsonToVideo(it) }
                callback(Playlist(query, videos))
            }
            is Result.Failure -> {
                Log.e("searchVideo", "Failed to retrieve data from $endpoint")
                Log.e("searchVideo", result.error.toString())
                callback(Playlist())
            }
        }
    }
}

private fun parseCategory (doc : Element) : Category {
    return Category(
            name = doc.select("h3.tile__title").first().text(),
            cardImageUrl = parseSrcSet(doc.select("div.tile__image").first().select("img").first().attr("srcset")),
            link = toAbsoluteUrl(doc.select("a.tile--category").first().attr("href"))
    )
}

private fun parseVideo (doc : Element, category : String = "") : Video {
    fun getDescription (input : Element) : String? {
        var description = input.select("div.tile__description").first()?.children()?.first()?.text()
        description = description ?: input.select("div.tile__description").first()?.text()
        description = description ?: input.select("div.tile__subtitle").first()?.text()
        return description
    }
    return Video(
            id = UUID.randomUUID().mostSignificantBits,
            title = doc.select("h3.tile__title").first().text(),
            description = getDescription(doc),
            cardImageUrl = parseSrcSet(doc.select("div.tile__image").first().select("img").first().attr("srcset")),
            category = category,
            detailsUrl = toAbsoluteUrl(doc.select("a.tile")?.first()?.attr("href"))
    )
}

private fun jsonToVideo (json : JSONObject, category: String = "") : Video {
    return Video(
            id = UUID.randomUUID().mostSignificantBits,
            title = json.getString("title"),
            description = sanitizeText(json.getString("description")),
            shortDescription = sanitizeText(json.getString("description")),
            detailsUrl = toAbsoluteUrl(json.getString("targetUrl")),
            cardImageUrl = toAbsoluteUrl(json.getString("thumbnail")),
            brand = json.getJSONArray("brands").optString(0),
            category = category
    )
}

private fun parsePlaylist (doc : Element) : Playlist {
    fun getTitle (input : Element) : String {
        // See if there is an actual title provided
        return input.select("h2.vrtlist__title").first()?.text()?.trim()
        // If there is an episode list with one season
        ?: input.select("div.tabs__tab--active span").first()?.text()?.trim()
        // If there is an episode list with multiple seasons
        ?: input.select("div.tabs__tab--active select.dropdown__element option")
                .firstOrNull { it.hasAttr("selected") }?.text()?.trim()
        // Return default value
        ?: "Playlist"
    }
    return Playlist(
            title = getTitle(doc),
            data = doc.select("li.vrtlist__item").map { parseVideo(it) }
    )
}

private fun parseSrcSet(srcSet : String) : String {
    val imageUrl = srcSet
            .split(',')
            .first { it.endsWith("2x") }
            .dropLast(2)
            .trim()
    return toAbsoluteUrl(imageUrl)
}

private fun getVideosLink(link : String) : String {
    return (if (link.last() == '/') link.dropLast(1) else link).plus(".mssecurevideo.json")
}

private fun getContentsLink(link : String) : String {
    return (if (link.last() == '/') link.dropLast(1) else link).plus(".content.json")
}

private fun sanitizeText(text : String) : String {
    var output = text.trim()
    if (output.startsWith("<p>")) output = output.drop(3).dropLast(4)
    return output
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
