package com.amplifyframework.auth

import android.content.Context
import com.amplifyframework.auth.client.Cognito
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import org.json.JSONObject

class AWSAuthPlugin : NotImplementedAuthPlugin<Unit>() {
    private val client = Cognito()
    private lateinit var poolId: String
    private lateinit var clientId: String
    private lateinit var clientSecret: String
    private lateinit var credentialStorage: CredentialStorage

    override fun getPluginKey(): String {
        // TODO: This is a lie. But this is what the CLI generates right now!
        return "awsCognitoAuthPlugin"
    }

    override fun configure(pluginConfiguration: JSONObject?, context: Context) {
        val userPoolJson = pluginConfiguration!!
            .getJSONObject("CognitoUserPool")
            .getJSONObject("Default")
        clientId = userPoolJson.getString("AppClientId")
        clientSecret = userPoolJson.getString("AppClientSecret")
        poolId = userPoolJson.getString("PoolId")
        credentialStorage = SecureCredentialStorage(context)
    }

    override fun getEscapeHatch() {
        return
    }

    override fun getVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun signUp(
        username: String,
        password: String,
        options: AuthSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        SignUpOperation(
            client,
            clientId,
            clientSecret,
            username,
            password,
            options,
            onSuccess,
            onError
        ).start()
    }

    override fun confirmSignUp(
        username: String,
        confirmationCode: String,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        ConfirmSignUpOperation(
            client,
            clientId,
            clientSecret,
            username,
            confirmationCode,
            onSuccess,
            onError
        ).start()
    }

    override fun signIn(
        username: String?,
        password: String?,
        options: AuthSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        SignInOperation(
            client, credentialStorage, clientId, clientSecret,
            poolId, username!!, password!!, options, onSuccess, onError
        ).start()
    }

    override fun fetchAuthSession(
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>,
    ) {
        FetchAuthSessionOperation(
            credentialStorage,
            client,
            clientId,
            clientSecret,
            onSuccess,
            onError
        ).start()
    }

    override fun signOut(
        options: AuthSignOutOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        SignOutOperation(client, credentialStorage, onSuccess, onError).start()
    }
}
