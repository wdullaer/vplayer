/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v17.leanback.app.GuidedStepFragment
import android.support.v17.leanback.widget.GuidanceStylist
import android.support.v17.leanback.widget.GuidedAction
import android.text.InputType
import android.util.Log
import android.widget.Toast

/**
 * A GuidedStepActivity (fancy name for an old skool wizard) that allows the user to input
 * their credentials for VRT NU
 *
 * TODO: extend this with google and facebook logins
 *
 * Created by wdullaer on 5/11/17.
 */
class AuthenticationActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            GuidedStepFragment.addAsRoot(this, FirstStepFragment(), android.R.id.content)
        }
    }
}

class FirstStepFragment : GuidedStepFragment() {
    private val LOGIN = 2L
    private val USERNAME = 3L
    private val PASSWORD = 4L

    /*override fun onProvideTheme(): Int {
        return R.style.theme_first_step
    }*/

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val title = getString(R.string.authentication_screen_title)
        val description = getString(R.string.authentication_screen_description)
        val icon = activity.getDrawable(R.drawable.app_icon_your_company)
        return GuidanceStylist.Guidance(title, description, "", icon)
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val defaultUsername = activity.getSharedPreferences(AUTHENTICATION_PREFERENCE_ROOT, Context.MODE_PRIVATE)
                .getString(activity.getString(R.string.pref_username_key), "")
        val defaultPassword = activity.getSharedPreferences(AUTHENTICATION_PREFERENCE_ROOT, Context.MODE_PRIVATE)
                .getString(activity.getString(R.string.pref_password_key), "")
        Log.i("AuthenticationActivity", defaultUsername)
        Log.i("AuthenticationActivity", defaultPassword.length.toString())

        val username = GuidedAction
                .Builder(activity)
                .id(USERNAME)
                .title(activity.getString(R.string.pref_username_title))
                .descriptionEditable(true)
                .editDescription(defaultUsername)
                .build()
        val password = GuidedAction
                .Builder(activity)
                .id(PASSWORD)
                .title(activity.getString(R.string.pref_password_title))
                .descriptionEditable(true)
                .editDescription(defaultPassword)
                .descriptionEditInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD.or(InputType.TYPE_CLASS_TEXT))
                .build()
        val login = GuidedAction
                .Builder(activity)
                .id(LOGIN)
                .title(activity.getString(R.string.pref_action_login))
                .build()

        actions.add(username)
        actions.add(password)
        actions.add(login)
    }

    override fun onGuidedActionClicked(action: GuidedAction?) {
        if (action == null) return

        when (action.id) {
            LOGIN -> {
                val username = actions.first { it.id == USERNAME }
                        .editDescription
                        .toString()
                val password = actions.first { it.id == PASSWORD }
                        .editDescription
                        .toString()

                Log.i("AuthenticationActivity", "Saving Credentials")

                activity.getSharedPreferences(AUTHENTICATION_PREFERENCE_ROOT, Context.MODE_PRIVATE).edit()
                        .putString(activity.getString(R.string.pref_username_key), username)
                        .putString(activity.getString(R.string.pref_password_key), password)
                        .apply()

                Log.i("AuthenticationActivity", "Refreshing Cookie")

                // Test the credentials by obtaining a cookie
                refreshVrtCookie(username, password) {error, cookie ->
                    Log.i("Authentication", "Handling cookie callback")
                    if (error == null && cookie != null) {
                            activity.getSharedPreferences(AUTHENTICATION_PREFERENCE_ROOT, Context.MODE_PRIVATE).edit()
                                    .putString(activity.getString(R.string.pref_cookie_key), cookie)
                                    .apply()

                            Toast.makeText(
                                    activity,
                                    activity.getString(R.string.toast_login_successful),
                                    Toast.LENGTH_SHORT
                            ).show()
                    } else {
                        Toast.makeText(
                                activity,
                                activity.getString(R.string.video_error_unknown_error),
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                    activity.finishAfterTransition()
                }
            }
        }
    }
}
