/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Model for a list of Videos with a title (because associative maps of Any are annoying to work with in Kotlin)
 * Created by wdullaer on 16/10/17.
 */
@Parcelize
data class Playlist (
        val title : String = "Playlist",
        val data : List<Video> = listOf()
) : Parcelable