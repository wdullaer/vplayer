/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Movie class represents video entity with title, description, image thumbs and video url.
 */
@Parcelize
data class Video(
        var id: Long = 0,
        override var title: String = "",
        var shortDescription: String? = null,
        override var description: String = "",
        var backgroundImageUrl: String? = null,
        var cardImageUrl: String? = null,
        override var videoUrl: String? = null,
        override var drmKey: String? = null,
        var detailsUrl: String? = null,
        var brand: String? = null,
        var category: String? = null,
        var relatedVideos : List<Playlist> = listOf(),
        var duration: Long = 0L,
        var publicationId: String = UUID.randomUUID().toString()
) : Playable {
    override fun toString(): String {
        return "Video{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", detailsUrl='" + detailsUrl + '\'' +
                ", backgroundImageUrl='" + backgroundImageUrl + '\'' +
                ", cardImageUrl='" + cardImageUrl + '\'' +
                '}'
    }
}
