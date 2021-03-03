package com.amplifyframework.auth.client

internal data class SignUpResponse(
        internal val userConfirmed: Boolean,
        internal val codeDeliveryDetails: CodeDeliveryDetails,
        internal val userSub: String
)