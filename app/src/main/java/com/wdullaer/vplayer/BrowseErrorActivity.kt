/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar

/**
 * BrowseErrorActivity shows how to use ErrorFragment.
 */
class BrowseErrorActivity : LeanbackActivity() {

    private lateinit var mErrorFragment: ErrorFragment
    private lateinit var mSpinnerFragment: SpinnerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testError()
    }

    private fun testError() {
        mErrorFragment = ErrorFragment()
        supportFragmentManager.beginTransaction().add(R.id.main_browse_fragment, mErrorFragment).commit()

        mSpinnerFragment = SpinnerFragment()
        supportFragmentManager.beginTransaction().add(R.id.main_browse_fragment, mSpinnerFragment).commit()

        val handler = Handler()
        handler.postDelayed({
            supportFragmentManager.beginTransaction().remove(mSpinnerFragment).commit()
            mErrorFragment.setErrorContent()
        }, TIMER_DELAY)
    }

    class SpinnerFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val progressBar = ProgressBar(container?.context)
            if (container is FrameLayout) {
                val layoutParams = FrameLayout.LayoutParams(SPINNER_WIDTH, SPINNER_HEIGHT, Gravity.CENTER)
                progressBar.layoutParams = layoutParams
            }
            return progressBar
        }
    }

    companion object {
        private const val TIMER_DELAY = 3000L
        private const val SPINNER_WIDTH = 100
        private const val SPINNER_HEIGHT = 100
    }
}