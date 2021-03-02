package com.amplifyframework.auth

import com.amplifyframework.auth.client.Cognito
import com.amplifyframework.auth.client.GlobalSignOutRequest
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SignOutOperation(
        private val client: Cognito,
        private val credentialStorage: CredentialStorage,
        private val onSuccess: Action,
        private val onError: Consumer<AuthException>) {
    fun start() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val accessToken = credentialStorage.accessToken()
                credentialStorage.clear()
                client.globalSignOut(GlobalSignOutRequest(accessToken = accessToken))
                onSuccess.call()
            } catch (error: Throwable) {
                onError.accept(AuthException("Sign out failed.", error, "Try again."))
            }
        }
    }
}
