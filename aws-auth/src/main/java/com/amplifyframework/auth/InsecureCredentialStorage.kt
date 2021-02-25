package com.amplifyframework.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

// TODO: write one that uses encryption, instead.
class InsecureCredentialStorage(context: Context): CredentialStorage {
    private val label = InsecureCredentialStorage::class.simpleName
    private val prefs: SharedPreferences = context.getSharedPreferences(label, Context.MODE_PRIVATE)

    override fun accessToken(token: String) {
        store(Key.ACCESS_TOKEN, token)
    }

    override fun accessToken(): String {
        return getString(Key.ACCESS_TOKEN)
    }

    override fun idToken(token: String) {
        store(Key.ID_TOKEN, token)
    }

    override fun idToken(): String {
        return getString(Key.ID_TOKEN)
    }

    override fun refreshToken(token: String) {
        store(Key.REFRESH_TOKEN, token)
    }

    override fun refreshToken(): String {
        return getString(Key.REFRESH_TOKEN)
    }

    override fun expiresIn(period: Int) {
        store(Key.EXPIRES_IN, period)
    }

    override fun tokenType(type: String) {
        return store(Key.TOKEN_TYPE, type)
    }

    override fun tokenType(): String {
        return getString(Key.TOKEN_TYPE)
    }

    @SuppressLint("ApplySharedPref")
    private fun store(key: Key, value: String) {
        prefs.edit()
            .putString(key.name, value)
            .commit()
    }

    @SuppressLint("ApplySharedPref")
    private fun store(key: Key, value: Int) {
        prefs.edit()
            .putInt(key.name, value)
            .commit()
    }

    private fun getString(key: Key): String {
        return prefs.getString(key.name, null)!!
    }

    enum class Key {
        ACCESS_TOKEN,
        ID_TOKEN,
        REFRESH_TOKEN,
        EXPIRES_IN,
        TOKEN_TYPE
    }
}
