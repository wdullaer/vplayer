/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.os.Bundle
import androidx.preference.PreferenceFragment
import androidx.leanback.preference.LeanbackPreferenceFragment
import androidx.leanback.preference.LeanbackSettingsFragment
import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen

/**
 * Fragment that renders the settings from an xml file
 *
 * Created by wdullaer on 3/11/17.
 */
class SettingsFragment : LeanbackSettingsFragment(), DialogPreference.TargetFragment {
    private lateinit var mPreferenceFragment : PreferenceFragment

    override fun onPreferenceStartInitialScreen() {
        mPreferenceFragment = buildPreferenceFragment(R.xml.settings, null)
        startPreferenceFragment(mPreferenceFragment)
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragment?, pref: Preference?): Boolean {
        return false
    }

    override fun onPreferenceStartScreen(caller: PreferenceFragment?, pref: PreferenceScreen?): Boolean {
        mPreferenceFragment = buildPreferenceFragment(R.xml.settings, pref?.key)
        startPreferenceFragment(mPreferenceFragment)
        return true
    }

    override fun findPreference(charSequence: CharSequence) : Preference {
        return mPreferenceFragment.findPreference(charSequence)
    }

    private fun buildPreferenceFragment(resId : Int, root : String?) : PreferenceFragment {
        val fragment = VRTPrefsFragment()
        val args = Bundle()
        args.putInt(PREFERENCE_RESOURCE_ID, resId)
        args.putString(VPLAYER_PREFERENCE_ROOT, root)
        fragment.arguments = args
        return fragment
    }
}

class VRTPrefsFragment : LeanbackPreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val root = arguments.getString(VPLAYER_PREFERENCE_ROOT)
        val prefResId = arguments.getInt(PREFERENCE_RESOURCE_ID)

        if (root == null) {
            addPreferencesFromResource(prefResId)
        } else {
            setPreferencesFromResource(prefResId, root)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key.equals(getString(R.string.pref_login_key))) {
            activity.startAuthenticationActivity()
        }
        return super.onPreferenceTreeClick(preference)
    }
}

const val PREFERENCE_RESOURCE_ID = "preference_resource"
const val VPLAYER_PREFERENCE_ROOT = "vplayer"
