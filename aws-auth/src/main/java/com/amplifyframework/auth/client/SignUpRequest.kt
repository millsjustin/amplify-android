package com.amplifyframework.auth.client

import org.json.JSONObject

internal data class SignUpRequest(
        internal val username: String,
        internal val password: String,
        internal val clientId: String,
        internal val secretHash: String) {
    fun asJson(): JSONObject = JSONObject()
            .put("Username", username)
            .put("Password", password)
            .put("ClientId", clientId)
            .put("SecretHash", secretHash)
}