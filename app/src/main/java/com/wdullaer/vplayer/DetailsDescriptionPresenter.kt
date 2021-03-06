/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(
            viewHolder: ViewHolder,
            item: Any) {
        val movie = item as Video

        viewHolder.title.text = movie.programTitle ?: movie.title
        viewHolder.subtitle.text = movie.shortDescription
        viewHolder.body.text = movie.description
    }
}