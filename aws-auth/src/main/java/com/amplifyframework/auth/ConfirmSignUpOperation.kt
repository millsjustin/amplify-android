package com.amplifyframework.auth

import android.util.Log
import com.amplifyframework.auth.client.Cognito
import com.amplifyframework.auth.client.ConfirmSignUpRequest
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep.DONE
import com.amplifyframework.core.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class ConfirmSignUpOperation(
    private val cognito: Cognito,
    private val clientId: String,
    private val clientSecret: String,
    private val username: String,
    private val confirmationCode: String,
    private val onSuccess: Consumer<AuthSignUpResult>,
    private val onError: Consumer<AuthException>
) {
    internal fun start() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                onSuccess.accept(callCognito())
            } catch (error: Throwable) {
                onError.accept(AuthException("Confirm sign up failed.", error, "Try again."))
            }
        }
    }

    private fun callCognito(): AuthSignUpResult {
        // This returns an empty response body with a 200 on success.
        val request = ConfirmSignUpRequest(
            clientId = clientId,
            secretHash = SecretHash.of(username, clientId, clientSecret),
            username = username,
            confirmationCode = confirmationCode
        )
        Log.w("ConfirmSignUp", request.toString())
        cognito.confirmSignUp(request)

        val nextStep = AuthNextSignUpStep(DONE, emptyMap<String, String>(), null)
        val userId = "TODO. How do I include this without keeping some global state?"
        val user = AuthUser(userId, username)
        return AuthSignUpResult(true, nextStep, user)
    }
}
