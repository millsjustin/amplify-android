package com.amplifyframework.auth.client

internal data class AuthenticationResult(
        internal val accessToken: String,
        internal val idToken: String,
        internal val refreshToken: String,
        internal val expiresIn: Int,
        internal val tokenType: String)
