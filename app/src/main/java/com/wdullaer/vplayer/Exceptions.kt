/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import androidx.annotation.StringRes

class AuthorizationException : Exception {
    constructor() : super()
    constructor(e : Exception) : super(e)
    constructor(msg : String) : super(msg)
}

class ParserException : Exception {
    val statusCode : Int
    @StringRes val userMessageRes : Int
    constructor(s : Int, @StringRes m : Int = R.string.error_fragment_message) : super() {
        statusCode = s
        userMessageRes = m
    }
    constructor(e : Exception, s : Int, @StringRes m : Int = R.string.error_fragment_message) : super(e) {
        statusCode = s
        userMessageRes = m
    }
    constructor(msg : String, s : Int, @StringRes m : Int = R.string.error_fragment_message) : super(msg) {
        statusCode = s
        userMessageRes = m
    }
}

class NetworkException : Exception {
    constructor() : super()
    constructor(e : Exception) : super(e)
    constructor(msg : String) : super(msg)
}

const val SEARCH_JSON_TO_VIDEO_ERROR = 1000
const val CONTENT_JSON_TO_VIDEO_ERROR = 1001
const val STREAM_JSON_TO_VIDEO_ERROR = 1002
const val ID_JSON_ERROR = 1003
const val VRT_TOKEN_ERROR = 1004
const val PARSE_CATEGORIES_ERROR = 2000
const val PARSE_LANDINGPAGE_ERROR = 2001