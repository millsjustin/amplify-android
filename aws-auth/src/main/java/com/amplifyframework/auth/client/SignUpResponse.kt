package com.amplifyframework.auth.client

import org.json.JSONObject

internal data class SignUpResponse(
    internal val userConfirmed: Boolean,
    internal val codeDeliveryDetails: CodeDeliveryDetails,
    internal val userSub: String
) {
    companion object {
        fun from(json: JSONObject): SignUpResponse {
            val codeDeliveryJson = json.getJSONObject("CodeDeliveryDetails")
            return SignUpResponse(
                userConfirmed = json.getBoolean("UserConfirmed"),
                userSub = json.getString("UserSub"),
                codeDeliveryDetails = CodeDeliveryDetails(
                    attributeName = codeDeliveryJson.getString("AttributeName"),
                    deliveryMedium = codeDeliveryJson.getString("DeliveryMedium"),
                    destination = codeDeliveryJson.getString("Destination")
                )
            )
        }
    }
}
