package com.amplifyframework.auth

import android.util.Base64
import android.util.Log
import com.amplifyframework.auth.AuthCodeDeliveryDetails.DeliveryMedium.EMAIL
import com.amplifyframework.auth.client.AuthenticationResult
import com.amplifyframework.auth.client.Cognito
import com.amplifyframework.auth.client.InitiateAuthRequest
import com.amplifyframework.auth.client.InitiateAuthResponse
import com.amplifyframework.auth.client.RespondToAuthChallengeRequest
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthSignInStep.DONE
import com.amplifyframework.core.Consumer
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class SignInOperation(
    private val cognito: Cognito,
    private val credentialStorage: CredentialStorage,
    private val clientId: String,
    private val clientSecret: String,
    private val poolId: String,
    private val username: String,
    private val password: String,
    private val options: AuthSignInOptions,
    private val onSuccess: Consumer<AuthSignInResult>,
    private val onError: Consumer<AuthException>
) {
    private val helper = AuthenticationHelper(poolId)

    internal fun start() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                onSuccess.accept(callCognito())
            } catch (error: Throwable) {
                onError.accept(AuthException("Sign in failed.", error, "Try again."))
            }
        }
    }

    private fun callCognito(): AuthSignInResult {
        @Suppress("UsePropertyAccessSyntax") // getA() is NOT "a"!!!!!!
        val response = cognito.initiateAuth(
            InitiateAuthRequest(
                clientId = clientId,
                authFlow = "USER_SRP_AUTH",
                authParameters = mapOf(
                    "USERNAME" to username,
                    "SRP_A" to helper.getA().toString(16),
                    "SECRET_HASH" to SecretHash.of(username, clientId, clientSecret)
                )
            )
        )
        Log.w("InitiateAuth", response.toString())

        if (!response.hasChallengeParameters) {
            if (response.authenticationResult != null) {
                storeCredentials(response.authenticationResult)
            }
            val details = AuthCodeDeliveryDetails("TODO: what is this field, actually?", EMAIL)
            val nextStep = AuthNextSignInStep(DONE, emptyMap(), details)
            return AuthSignInResult(true, nextStep)
        }

        when (response.challengeName) {
            "PASSWORD_VERIFIER" -> {
                verifyPassword(password, response)
                val details = AuthCodeDeliveryDetails("what is this", EMAIL)
                val nextStep = AuthNextSignInStep(DONE, emptyMap(), details)
                return AuthSignInResult(true, nextStep)
            }
            else -> {
                throw AuthException(
                    "Unknown challenge = ${response.challengeName}", "Implement it!"
                )
            }
        }
    }

    private fun verifyPassword(password: String, initAuthResponse: InitiateAuthResponse) {
        Log.i("SignIn", "verifying password from $initAuthResponse")

        val challengeParameters = initAuthResponse.challengeParameters!!
        val salt = BigInteger(challengeParameters["SALT"]!!, 16)
        val secretBlock = challengeParameters["SECRET_BLOCK"]!!
        val userIdForSrp = challengeParameters["USER_ID_FOR_SRP"]!!
        val username = challengeParameters["USERNAME"]!!
        val srpB = BigInteger(challengeParameters["SRP_B"]!!, 16)
        val timestamp = computeTimestamp()

        val key = helper.getPasswordAuthenticationKey(userIdForSrp, password, srpB, salt)
        val claimSignature = claimSignature(userIdForSrp, key, timestamp, secretBlock)

        val request = RespondToAuthChallengeRequest(
            challengeName = initAuthResponse.challengeName!!,
            clientId = clientId,
            challengeResponses = mapOf(
                "SECRET_HASH" to SecretHash.of(username, clientId, clientSecret),
                "PASSWORD_CLAIM_SIGNATURE" to claimSignature,
                "PASSWORD_CLAIM_SECRET_BLOCK" to secretBlock,
                "TIMESTAMP" to timestamp,
                "USERNAME" to username
            ),
            session = initAuthResponse.session
        )
        val responseToAuthChallenge = cognito.respondToAuthChallenge(request)
        val authResult = responseToAuthChallenge.authenticationResult
        storeCredentials(authResult)
    }

    // calculateSignature(hkdf, userPoolId, ChallengeParameters.USER_ID_FOR_SRP, ChallengeParameters.SECRET_BLOCK, dateNow)
    private fun claimSignature(
        userIdForSrp: String,
        key: ByteArray,
        timestamp: String,
        secretBlock: String
    ): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        val keySpec = SecretKeySpec(key, algorithm)
        mac.init(keySpec)
        mac.update(poolId.split("_")[1].toByteArray())
        mac.update(userIdForSrp.toByteArray())
        mac.update(Base64.decode(secretBlock, Base64.NO_WRAP))

        val hmac = mac.doFinal(timestamp.toByteArray())
        return Base64.encodeToString(hmac, Base64.NO_WRAP)
    }

    private fun computeTimestamp(): String {
        val simpleDateFormat = SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US)
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return simpleDateFormat.format(Date())
    }

    private fun storeCredentials(authResult: AuthenticationResult) {
        Log.i("SignIn", "handling auth result = $authResult")
        credentialStorage.accessToken(authResult.accessToken)
        credentialStorage.idToken(authResult.idToken)
        credentialStorage.refreshToken(authResult.refreshToken)
        credentialStorage.expiresIn(authResult.expiresIn)
        credentialStorage.tokenType(authResult.tokenType)
    }
}
