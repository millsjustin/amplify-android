package com.amplifyframework.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit
import kotlin.math.max

// TODO: write one that uses encryption, instead.
class InsecureCredentialStorage(context: Context): CredentialStorage {
    private val label = InsecureCredentialStorage::class.simpleName
    private val prefs: SharedPreferences = context.getSharedPreferences(label, Context.MODE_PRIVATE)

    @SuppressLint("ApplySharedPref")
    override fun clear() {
        prefs.edit().clear().commit()
    }

    override fun isEmpty(): Boolean {
        return !prefs.contains(Key.EXPIRATION_EPOCH.name)
    }

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
        store(Key.EXPIRATION_EPOCH, now() + period)
    }

    // TODO: feels like this logic probably belongs a level higher in abstraction.
    override fun isExpired(): Boolean {
        val gracePeriod = TimeUnit.MINUTES.toSeconds(5)
        val expirationEpoch = prefs.getLong(Key.EXPIRATION_EPOCH.name, 0)
        val safelyBeforeExpiration = max(0, expirationEpoch - gracePeriod)
        return now() > safelyBeforeExpiration
    }

    private fun now(): Long {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
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
    private fun store(key: Key, value: Long) {
        prefs.edit()
            .putLong(key.name, value)
            .commit()
    }

    private fun getString(key: Key): String {
        return prefs.getString(key.name, null)!!
    }

    enum class Key {
        ACCESS_TOKEN,
        ID_TOKEN,
        REFRESH_TOKEN,
        EXPIRATION_EPOCH,
        TOKEN_TYPE
    }
}
