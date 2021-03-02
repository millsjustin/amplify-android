package com.amplifyframework.auth.client

import org.json.JSONObject

internal data class ConfirmSignUpRequest(
        internal val secretHash: String,
        internal val clientId: String,
        internal val username: String,
        internal val confirmationCode: String) {
    fun asJson(): JSONObject = JSONObject()
            .put("SecretHash", secretHash)
            .put("ClientId", clientId)
            .put("Username", username)
            .put("ConfirmationCode", confirmationCode)
}