package com.amplifyframework.auth.client

internal data class CodeDeliveryDetails(
    internal val destination: String,
    internal val attributeName: String,
    internal val deliveryMedium: String
)
