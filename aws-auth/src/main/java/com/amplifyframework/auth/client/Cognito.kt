package com.amplifyframework.auth.client

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


internal class Cognito(private val endpoint: String = "https://cognito-identity.us-east-2.amazonaws.com") {
    internal fun confirmSignUp(request: ConfirmSignUpRequest) {
        post(request.asJson())
    }

    internal fun signUp(request: SignUpRequest): SignUpResponse {
        val response = post(request.asJson())
        val codeDeliveryJson = response.getJSONObject("CodeDeliveryDetails")
        return SignUpResponse(
                userConfirmed = response.getBoolean("UserConfirmed"),
                userSub = response.getString("UserSub"),
                codeDeliveryDetails = CodeDeliveryDetails(
                        attributeName = codeDeliveryJson.getString("AttributeName"),
                        deliveryMedium = codeDeliveryJson.getString("DeliveryMedium"),
                        destination = codeDeliveryJson.getString("Destination")
                )
        )
    }

    internal fun initiateAuth(request: InitiateAuthRequest): InitiateAuthResponse {
        val response = post(request.asJson())

        val challengeParamJson = response.getJSONObject("ChallengeParameters")
        val challengeParameters = mutableMapOf<String, String>()
        for (key in challengeParameters.keys) {
            challengeParameters[key] = challengeParamJson[key] as String
        }

        val authResultJson = response.getJSONObject("AuthenticationResult")


        return InitiateAuthResponse(
                challengeName = response.getString("ChallengeName"),
                challengeParameters = challengeParameters,
                session = response.getString("Session"),
                authenticationResult = AuthenticationResult(
                        accessToken = authResultJson.getString("AccessToken"),
                        expiresIn = authResultJson.getInt("ExpiresIn"),
                        idToken = authResultJson.getString("IdToken"),
                        refreshToken = authResultJson.getString("RefreshToken"),
                        tokenType = authResultJson.getString("TokenType")
                ),
                hasChallengeParameters = challengeParameters.isNotEmpty()
        )
    }

    internal fun globalSignOut(accessToken: String) {
        post(JSONObject().put("AccessToken", accessToken))
    }

    internal fun respondToAuthChallenge(request: RespondToAuthChallengeRequest): RespondToAuthChallengeResponse {
        val response = post(request.asJson())
        val authResultJson = response.getJSONObject("AuthenticationResult")
        return RespondToAuthChallengeResponse(
                challengeName = response.getString("ChallengeName"),
                authenticationResult = AuthenticationResult(
                        accessToken = authResultJson.getString("AccessToken"),
                        expiresIn = authResultJson.getInt("ExpiresIn"),
                        idToken = authResultJson.getString("IdToken"),
                        refreshToken = authResultJson.getString("RefreshToken"),
                        tokenType = authResultJson.getString("TokenType")
                )
        )
    }

    private fun post(json: JSONObject): JSONObject {
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; utf-8")
        conn.setRequestProperty("Accept", "application/json")

        // sign it!

        conn.doOutput = true

        val input = json.toString().toByteArray()
        conn.outputStream.write(input, 0, input.size)

        if (conn.responseCode < 200 || conn.responseCode > 399) {
            throw ResponseError(conn.responseCode, readStream(conn.errorStream))
        } else {
            val response = JSONObject(readStream(conn.inputStream))
            Log.i("Cognito", response.toString())
            return response
        }
    }

    private fun readStream(stream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(stream))
        val response = StringBuilder()
        var responseLine: String? = reader.readLine()
        while (responseLine != null) {
            response.append(responseLine.trim())
            responseLine = reader.readLine()
        }
        return response.toString()
    }

    class ResponseError(code: Int, message: String) : Exception(message)

    private fun sign() {
        val method = 'POST'
        val service = ''
    }
}
