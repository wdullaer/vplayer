/*
 * Copyright (C) 2018 Wouter Dullaert
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.wdullaer.vplayer

import android.util.Log
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import org.json.JSONObject

const val API_KEY = "3_qhEcPa5JGFROVwu5SWKqJ4mVOIkwlFNMSKwzPDAh8QZOtHqu6L4nD5Q7lk0eXOOG"

fun refreshVrtCookie (username : String, password : String, callback : (Exception?, String?) -> Unit) {
    // https://developers.gigya.com/display/GD/accounts.login+REST
    // Ideally this should be in a separate function
    val tokenParameters = listOf(
            "loginID" to username,
            "password" to password,
            "APIKey" to API_KEY,
            "sessionExpiration" to "-1",
            "loginMode" to "standard",
            "targetEnv" to "jssdk",
            "includeSSOToken" to "true",
            "authMode" to "cookie",
            "httpStatusCodes" to "true"
    )
    "https://accounts.vrt.be/accounts.login".httpPost(tokenParameters)
            .header("Accept" to JSON_MIME)
            .responseJson { _, _, result ->
                when (result) {
                    is Result.Success -> {
                        val tokenResult = result.get().obj()
                        if (tokenResult.getInt("errorCode") != 0) {
                            Log.e("VRTCookie", "Failed to refresh Cookie")
                            Log.e("VRTCookie", tokenResult.getString("errorDetails"))
                            callback(Exception(tokenResult.getString("errorDetails")), null)
                            return@responseJson
                        }
                        val cookiePayload = JSONObject(mapOf(
                                "uid" to tokenResult.getString("UID"),
                                "uidsig" to tokenResult.getString("UIDSignature"),
                                "ts" to tokenResult.getString("signatureTimestamp"),
                                "email" to username
                        ))
                        "https://token.vrt.be/"
                                .httpPost()
                                .header(mapOf("content-type" to JSON_MIME,
                                        "Referer" to "https://www.vrt.be/vrtnu",
                                        "Origin" to "https://www.vrt.be"))
                                .body(cookiePayload.toString().toByteArray())
                                .response { request, response, result2 ->
                                    when (result2) {
                                        is Result.Success -> {
                                            val output = createCookieString(response.headers["Set-Cookie"].orEmpty())
                                            callback(null, output)
                                        }
                                        is Result.Failure -> {
                                            Log.i("VRTCookie", request.cUrlString())
                                            Log.e("VRTCookie", "Failed to refresh VRT Cookie (could not obtain cookies)")
                                            Log.e("VRTCookie", result2.error.toString())
                                            callback(result2.error, null)
                                        }
                                    }
                                }
                    }
                    is Result.Failure -> {
                        Log.e("VRTCookie", "Failed to refresh VRT Cookie (could not obtain token)")
                        Log.e("VRTCookie", result.error.toString())
                        callback(result.error, null)
                    }
                }
            }
}

private fun createCookieString(cookies : List<String>) : String {
    return cookies.asSequence().map {cookie ->
        cookie
                .split(";")
                .asSequence()
                .map { it.trim() }
                .filterNot { it.startsWith("Secure") }
                .filterNot { it.startsWith("Expires") }
                .filterNot { it.startsWith("HttpOnly") }
                .filterNot { it.startsWith("Domain") }
                .toList()
    }.reduce { acc, list -> acc + list }.joinToString(";")
}