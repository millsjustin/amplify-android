package com.amplifyframework.auth

import android.util.Log
import com.amplifyframework.auth.client.Cognito
import com.amplifyframework.auth.client.SignUpRequest
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep.CONFIRM_SIGN_UP_STEP
import com.amplifyframework.auth.result.step.AuthSignUpStep.DONE
import com.amplifyframework.core.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class SignUpOperation(
        private val cognito: Cognito,
        private val clientId: String,
        private val clientSecret: String,
        private val username: String,
        private val password: String,
        private val options: AuthSignUpOptions,
        private val onSuccess: Consumer<AuthSignUpResult>,
        private val onError: Consumer<AuthException>
) {
    internal fun start() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                onSuccess.accept(callCognito())
            } catch (error: Throwable) {
                onError.accept(AuthException("Sign up failed.", error, "Try again."))
            }
        }
    }

    private fun callCognito(): AuthSignUpResult {
        val request = SignUpRequest(
            username = username,
            password = password,
            clientId = clientId,
            secretHash = SecretHash.of(username, clientId, clientSecret),
            userAttributes = options.userAttributes.map { Pair(it.key.keyString, it.value) }
        )
        Log.w("SignUp", request.toString())
        val response = cognito.signUp(request)

        // Map into Amplify Auth's code delivery details structure
        val details = response.codeDeliveryDetails
        val destination = details.destination
        val deliveryMedium = AuthCodeDeliveryDetails.DeliveryMedium.fromString(
                details.deliveryMedium
        )
        val attributeName = details.attributeName
        val codeDeliveryDetails = AuthCodeDeliveryDetails(
                destination,
                deliveryMedium,
                attributeName
        )

        // Build result contents
        val signUpStep = if (response.userConfirmed) DONE else CONFIRM_SIGN_UP_STEP
        val nextStep = AuthNextSignUpStep(signUpStep, emptyMap(), codeDeliveryDetails)
        val user = AuthUser(response.userSub, username)
        return AuthSignUpResult(response.userConfirmed, nextStep, user)
    }
}
