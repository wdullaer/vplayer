/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import java.io.Serializable

/**
 * Model of a VRT Nu Category
 *
 * Created by wdullaer on 8/10/17.
 */

data class Category(
        var name : String = "",
        var cardImageUrl : String? = null,
        var link : String? = null
) : Serializable