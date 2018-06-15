/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.content.Context
import android.os.Bundle
import android.support.v17.leanback.app.GuidedStepSupportFragment
import android.support.v17.leanback.widget.GuidanceStylist
import android.support.v17.leanback.widget.GuidedAction
import android.support.v4.app.FragmentActivity
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
class AuthenticationActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, FirstStepFragment(), android.R.id.content)
        }
    }
}

class FirstStepFragment : GuidedStepSupportFragment() {
    private val LOGIN = 2L
    private val USERNAME = 3L
    private val PASSWORD = 4L
    private val LOGOUT = 5L

    /*override fun onProvideTheme(): Int {
        return R.style.theme_first_step
    }*/

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val title = getString(R.string.authentication_screen_title)
        val description = getString(R.string.authentication_screen_description)
        val icon = requireContext().getDrawable(R.drawable.vplayer_banner)
        return GuidanceStylist.Guidance(title, description, "", icon)
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val context = requireContext()
        val defaultUsername = context.getSharedPreferences(AUTHENTICATION_PREFERENCE_ROOT, Context.MODE_PRIVATE)
                .getString(context.getString(R.string.pref_username_key), "")
        val defaultPassword = context.getSharedPreferences(AUTHENTICATION_PREFERENCE_ROOT, Context.MODE_PRIVATE)
                .getString(context.getString(R.string.pref_password_key), "")
        val cookie = context.getSharedPreferences(AUTHENTICATION_PREFERENCE_ROOT, Context.MODE_PRIVATE)
                .getString(context.getString(R.string.pref_cookie_key), "")
        Log.i("AuthenticationActivity", defaultUsername)
        Log.i("AuthenticationActivity", defaultPassword.length.toString())

        val username = GuidedAction
                .Builder(context)
                .id(USERNAME)
                .title(context.getString(R.string.pref_username_title))
                .descriptionEditable(true)
                .editDescription(defaultUsername)
                .build()
        val password = GuidedAction
                .Builder(context)
                .id(PASSWORD)
                .title(context.getString(R.string.pref_password_title))
                .descriptionEditable(true)
                .editDescription(defaultPassword)
                .descriptionEditInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD.or(InputType.TYPE_CLASS_TEXT))
                .build()
        val loginTextResId = if (cookie == "") R.string.pref_action_login else R.string.pref_action_refresh
        val login = GuidedAction
                .Builder(context)
                .id(LOGIN)
                .title(context.getString(loginTextResId))
                .build()
        actions.add(username)
        actions.add(password)
        actions.add(login)

        if (cookie != "") {
            val logout = GuidedAction
                    .Builder(context)
                    .id(LOGOUT)
                    .title(context.getString(R.string.pref_action_logout))
                    .build()
            actions.add(logout)
        }
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

                val context = requireContext()
                context.getSharedPreferences(AUTHENTICATION_PREFERENCE_ROOT, Context.MODE_PRIVATE).edit()
                        .putString(context.getString(R.string.pref_username_key), username)
                        .putString(context.getString(R.string.pref_password_key), password)
                        .apply()

                Log.i("AuthenticationActivity", "Refreshing Cookie")

                // Test the credentials by obtaining a cookie
                refreshVrtCookie(username, password) {error, cookie ->
                    Log.i("Authentication", "Handling cookie callback")
                    if (error == null && cookie != null) {
                            context.getSharedPreferences(AUTHENTICATION_PREFERENCE_ROOT, Context.MODE_PRIVATE).edit()
                                    .putString(context.getString(R.string.pref_cookie_key), cookie)
                                    .apply()

                            Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_login_successful),
                                    Toast.LENGTH_SHORT
                            ).show()
                    } else {
                        Toast.makeText(
                                context,
                                context.getString(R.string.video_error_unknown_error),
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                    requireActivity().finishAfterTransition()
                }
            }
            LOGOUT -> {
                Log.i("AuthenticationActivity", "Removing credentials")
                val context = requireContext()
                context.getSharedPreferences(AUTHENTICATION_PREFERENCE_ROOT, Context.MODE_PRIVATE).edit()
                        .remove(context.getString(R.string.pref_username_key))
                        .remove(context.getString(R.string.pref_password_key))
                        .remove(context.getString(R.string.pref_cookie_key))
                        .apply()
                Toast.makeText(
                        context,
                        context.getString(R.string.toast_logout_successful),
                        Toast.LENGTH_SHORT
                ).show()
                requireActivity().recreate()
            }
        }
    }
}
