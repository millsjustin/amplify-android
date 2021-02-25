package com.amplifyframework.auth

// TODO: data class? This looks like a bunch of dumb setters/getters.
interface CredentialStorage {

    fun accessToken(token: String)

    fun idToken(token: String)

    fun refreshToken(token: String)

    fun expiresIn(period: Int)

    fun tokenType(type: String)

    fun accessToken(): String

    fun idToken(): String

    fun refreshToken(): String

    fun tokenType(): String
}
