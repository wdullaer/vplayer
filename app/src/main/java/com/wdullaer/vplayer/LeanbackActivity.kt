/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.support.v4.app.FragmentActivity

/**
 * A FragmentActivity that implements common functionality to all Activities in this app
 *
 * Created by wdullaer on 23/10/17.
 */
abstract class LeanbackActivity : FragmentActivity() {
    override fun onSearchRequested(): Boolean {
        startSearchActivity()
        return true
    }
}
