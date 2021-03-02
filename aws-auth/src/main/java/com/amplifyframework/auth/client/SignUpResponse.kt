package com.amplifyframework.auth.client

data class SignUpResponse(
        val userConfirmed: Boolean,
        val codeDeliveryDetails: CodeDeliveryDetails,
        val userSub: String
)