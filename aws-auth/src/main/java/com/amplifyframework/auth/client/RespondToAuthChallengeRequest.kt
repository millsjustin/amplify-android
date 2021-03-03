package com.amplifyframework.auth.client

import org.json.JSONObject

internal data class RespondToAuthChallengeRequest(
    internal val challengeName: String,
    internal val clientId: String,
    internal val challengeResponses: Map<String, String>?,
    internal val session: String?
) {
    fun asJson(): JSONObject {
        val json = JSONObject()
            .put("ChallengeName", challengeName)
            .put("ClientId", clientId)

        if (session != null) {
            json.put("Session", session)
        }
        if (challengeResponses != null) {
            json.put("ChallengeResponses", JSONObject(challengeResponses))
        }

        return json
    }
}
