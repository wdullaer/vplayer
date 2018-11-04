package com.wdullaer.vplayer

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LiveVideo(
        override var title: String = "LiveVideo",
        override var description: String = "",
        @DrawableRes var cardImageRes: Int,
        var detailsUrl: String,
        override var videoUrl: String? = null,
        override var drmKey: String? = null
) : Playable {
    override fun toString(): String {
        return "LiveVideo{title=$title, description=$description, cardImageRes=$cardImageRes, detailsUrl=$detailsUrl, videoUrl=$videoUrl}"
    }
}

interface Playable : Parcelable {
    var title: String
    var description: String
    var videoUrl: String?
    var drmKey: String?
    val hasDrm: Boolean
        get() = this.drmKey != null
}
