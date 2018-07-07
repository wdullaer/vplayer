/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import android.util.Log
import java.util.Calendar

/**
 * We are not using the content provider to do any data manipulation or fetching if we can help it.
 * I find it an overly complicated "abstraction" over something which is essentially simple:
 * fetch some data async and return it in a callback. Especially when your data is not coming out
 * of a SQL database.
 * Most of my data fetching functions just take a nodejs style callback with a Playlist of Videos.
 *
 * That said, certain android integrations require that the data be provided through a content
 * provider. On Android TV this is the global search functionality. (for an idea of how much more
 * complicated this is, compare the query function here, with the query function in the
 * SearchActivity)
 * I'm implementing the bare minimum here to get this working, but it is recommended to just call
 * VRTNuData classes where possible and just skip this content provider.
 */
class VRTContentProvider : ContentProvider() {
    private val contentAuthority = "com.wdullaer.vplayer"
    // private val BASE_URI = Uri.parse("content://$contentAuthority")
    private val searchSuggest = 0
    private val cursorColumns = listOf(
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1, // Name
                SearchManager.SUGGEST_COLUMN_TEXT_2, // Description
                SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE, // Card Image
                SearchManager.SUGGEST_COLUMN_CONTENT_TYPE, // Mime type of data
                SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
                SearchManager.SUGGEST_COLUMN_DURATION
    ).toTypedArray()

    private lateinit var uriMatcher : UriMatcher

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        return when (uriMatcher.match(uri)) {
            searchSuggest -> {
                val query = selectionArgs?.first() ?: ""
                val results = try {
                    searchVideoSync(query)
                } catch (e : Exception) {
                    Log.e("searchVideoSync", "Failed to parse search json")
                    Log.e("searchVideoSync", e.toString())
                    Playlist()
                }
                val year = Calendar.getInstance().get(Calendar.YEAR)
                val cursor = MatrixCursor(cursorColumns, results.data.size)
                results.data.forEach {
                    cursor.newRow()
                            .add(BaseColumns._ID, it.id)
                            .add(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, it.publicationId)
                            .add(SearchManager.SUGGEST_COLUMN_TEXT_1, it.title)
                            .add(SearchManager.SUGGEST_COLUMN_TEXT_2, it.description)
                            .add(SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE, it.cardImageUrl)
                            .add(SearchManager.SUGGEST_COLUMN_CONTENT_TYPE, "application/dash+xml")
                            // VRT does not provide the production year in its APIs, but it is mandatory for google
                            // We are setting a default of the current year
                            .add(SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR, year)
                            .add(SearchManager.SUGGEST_COLUMN_DURATION, it.duration)
                }
                cursor
            }
            else -> throw UnsupportedOperationException("Unknown URI $uri")
        }
    }

    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            searchSuggest -> SearchManager.SUGGEST_MIME_TYPE
            else -> throw UnsupportedOperationException("Unknown URI $uri")
        }
    }
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        return Uri.EMPTY
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun onCreate(): Boolean {
        uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        uriMatcher.addURI(contentAuthority, "search/${SearchManager.SUGGEST_URI_PATH_QUERY}", searchSuggest)
        uriMatcher.addURI(contentAuthority, "search/${SearchManager.SUGGEST_URI_PATH_QUERY}/*", searchSuggest)
        return true
    }
}