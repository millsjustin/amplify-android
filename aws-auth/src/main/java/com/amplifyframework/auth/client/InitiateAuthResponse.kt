package com.amplifyframework.auth.client

internal data class InitiateAuthResponse(
        internal val challengeParameters: Map<String, String>,
        internal val authenticationResult: AuthenticationResult,
        internal val hasChallengeParameters: Boolean,
        internal val challengeName: String,
        internal val session: String)
