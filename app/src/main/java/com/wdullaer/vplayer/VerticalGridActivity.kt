/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.os.Bundle
import android.util.Log

/**
 * LeanbackActivity that shows a Vertical Grid of Items
 *
 * Created by wdullaer on 23/10/17.
 */

class VerticalGridActivity : LeanbackActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("VerticalGridActivity", "onCreate")
        setContentView(R.layout.activity_vertical_grid)
        // todo: see what grid background is in the sample app
        // window.setBackgroundDrawableResource(R.drawable.default_background)
    }
}