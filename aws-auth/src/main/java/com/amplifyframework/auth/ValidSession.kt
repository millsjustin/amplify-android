package com.amplifyframework.auth

data class ValidSession(
        val accessToken: String,
        val idToken: String
) : AuthSession(true)
