package com.amplifyframework.auth.client

import org.json.JSONObject
import java.io.BufferedReader
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
                hasChallengeParameters = !challengeParameters.isEmpty()
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
        conn.outputStream.write(input, 0, input.length)

        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var responseLine: String? = null
        while ((responseLine = reader.readLine()) != null) {
            response.append(responseLine.trim())
        }
        return JSONObject(response.toString())
    }
}
