package com.amplifyframework.auth.client

import org.json.JSONObject

data class InitiateAuthRequest(val authFlow: String, val clientId: String, val authParameters: Map<String, String>) {
    fun asJson(): JSONObject  = JSONObject()
}
