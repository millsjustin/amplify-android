package com.amplifyframework.auth

sealed class Session(signedIn: Boolean) : AuthSession(signedIn) {
    class InvalidSession : Session(false)

    data class ValidSession(
        val accessToken: String,
        val idToken: String
    ) : Session(true)
}
