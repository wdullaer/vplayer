/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.View
import java.io.PrintWriter
import java.io.StringWriter

/**
 * This class demonstrates how to extend [androidx.leanback.app.ErrorFragment].
 */
class ErrorFragment : androidx.leanback.app.ErrorSupportFragment() {
    var error : ParserException? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        error = savedInstanceState?.getSerializable("error") as ParserException
    }

    internal fun setErrorContent() {
        imageDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.lb_ic_sad_cloud)
        setDefaultBackground(TRANSLUCENT)

        error?.let {
            title = resources.getString(it.userMessageRes)
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            it.printStackTrace(pw)
            message = sw.toString()
            pw.close()
            sw.close()
        }
        title = title ?: resources.getString(R.string.app_name)
        message = message ?: resources.getString(R.string.error_fragment_message)

        buttonText = resources.getString(R.string.dismiss_error)
        buttonClickListener = View.OnClickListener {
            fragmentManager?.beginTransaction()?.remove(this@ErrorFragment)?.commit()
        }


    }

    companion object {
        private const val TRANSLUCENT = true
    }
}