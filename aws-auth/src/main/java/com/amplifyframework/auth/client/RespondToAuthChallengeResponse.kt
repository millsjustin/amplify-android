package com.amplifyframework.auth.client

internal data class RespondToAuthChallengeResponse(
        internal val authenticationResult: AuthenticationResult,
        internal val challengeName: String)
