package com.amplifyframework.auth

class InsecureInMemCredStore: CredentialStorage {
    private var accessToken: String? = null
    private var idToken: String? = null
    private var refreshToken: String? = null
    private var expiresIn: Int? = null
    private var tokenType: String? = null

    override fun accessToken(token: String) {
        this.accessToken = token
    }

    override fun accessToken(): String = accessToken!!

    override fun idToken(token: String) {
        this.idToken = token
    }

    override fun idToken() = idToken!!

    override fun refreshToken(token: String) {
        this.refreshToken = token
    }

    override fun refreshToken() = refreshToken!!

    override fun expiresIn(period: Int) {
        this.expiresIn = period
    }

    override fun tokenType(type: String) {
        this.tokenType = type
    }

    override fun tokenType() = tokenType!!
}
