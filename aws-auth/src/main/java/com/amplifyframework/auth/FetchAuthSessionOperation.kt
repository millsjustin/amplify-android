package com.amplifyframework.auth

import com.amplifyframework.auth.Session.InvalidSession
import com.amplifyframework.auth.Session.ValidSession
import com.amplifyframework.core.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType.REFRESH_TOKEN_AUTH
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest

internal class FetchAuthSessionOperation(
        private val credentialStorage: CredentialStorage,
        private val cognito: CognitoIdentityProviderClient,
        private val clientId: String,
        private val onSuccess: Consumer<AuthSession>,
        private val onError: Consumer<AuthException>) {
    internal fun start() {
        GlobalScope.launch(Dispatchers.IO) {
            if (credentialStorage.isEmpty()) {
                onSuccess.accept(InvalidSession())
            } else if (credentialStorage.isExpired()) {
                refresh()
            }
            try {
                val accessToken = credentialStorage.accessToken()
                val idToken = credentialStorage.idToken()
                onSuccess.accept(ValidSession(accessToken, idToken))
            } catch (error: Throwable) {
                onError.accept(AuthException("Failed to fetch session.", error, "Try again."))
            }
        }
    }

    private fun refresh() {
        val parameters = mapOf("REFRESH_TOKEN" to credentialStorage.refreshToken())
        val request = InitiateAuthRequest.builder()
            .authFlow(REFRESH_TOKEN_AUTH)
            .clientId(clientId)
            .authParameters(parameters)
            .build()
        val response = cognito.initiateAuth(request)
        val authenticationResult = response.authenticationResult()
        credentialStorage.clear()
        credentialStorage.refreshToken(authenticationResult.refreshToken())
        credentialStorage.accessToken(authenticationResult.accessToken())
        credentialStorage.idToken(authenticationResult.idToken())
        credentialStorage.expiresIn(authenticationResult.expiresIn())
        credentialStorage.tokenType(authenticationResult.tokenType())
    }
}
