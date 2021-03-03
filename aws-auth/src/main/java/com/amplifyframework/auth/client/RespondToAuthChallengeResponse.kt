package com.amplifyframework.auth.client

import org.json.JSONObject

internal data class RespondToAuthChallengeResponse(
    internal val authenticationResult: AuthenticationResult,
    internal val challengeName: String?
) {
    companion object {
        fun from(json: JSONObject): RespondToAuthChallengeResponse {
            val authResultJson = json.getJSONObject("AuthenticationResult")
            val challengeName = if (json.has("ChallengeName")) {
                json.getString("ChallengeName")
            } else null

            return RespondToAuthChallengeResponse(
                challengeName = challengeName,
                authenticationResult = AuthenticationResult(
                    accessToken = authResultJson.getString("AccessToken"),
                    expiresIn = authResultJson.getInt("ExpiresIn"),
                    idToken = authResultJson.getString("IdToken"),
                    refreshToken = authResultJson.getString("RefreshToken"),
                    tokenType = authResultJson.getString("TokenType")
                )
            )
        }
    }
}
