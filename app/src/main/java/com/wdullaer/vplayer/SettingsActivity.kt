/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.app.Activity
import android.os.Bundle
import android.util.Log

/**
 * Activity that allows the user to configure settings
 *
 * Created by wdullaer on 3/11/17.
 */

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("SettingsActivity", "onCreate")
        setContentView(R.layout.activity_settings)
    }
}