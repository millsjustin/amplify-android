package com.amplifyframework.auth.client

class InitiateAuthResponse(val challengeParameters: Map<String, String>, val authenticationResult: AuthenticationResult, val hasChallengeParameters: Boolean, val challengeName: String, val session: Any)
