package com.amplifyframework.auth.client

import org.json.JSONObject

internal data class RespondToAuthChallengeRequest(
        internal val challengeName: String,
        internal val clientId: String,
        internal val challengeResponses: Map<String, String>,
        internal val session: String) {
    fun asJson(): JSONObject = JSONObject()
            .put("Session", session)
            .put("ChallengeName", challengeName)
            .put("ClientId", clientId)
            .put("ChallengeResponses", JSONObject(challengeResponses))
}
