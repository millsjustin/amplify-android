package com.amplifyframework.auth

import com.amplifyframework.core.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class FetchAuthSessionOperation(
        private val credentialStorage: CredentialStorage,
        private val onSuccess: Consumer<AuthSession>,
        private val onError: Consumer<AuthException>) {
    internal fun start() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val accessToken = credentialStorage.accessToken()
                val idToken = credentialStorage.idToken()
                onSuccess.accept(ValidSession(accessToken, idToken))
            } catch (error: Throwable) {
                onError.accept(AuthException("Failed to fetch session.", error, "Try again."))
            }
        }
    }
}
