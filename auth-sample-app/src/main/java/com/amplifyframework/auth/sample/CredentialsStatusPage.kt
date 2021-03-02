package com.amplifyframework.auth.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amplifyframework.auth.Session.ValidSession
import com.amplifyframework.auth.sample.databinding.ActivityCredentialsStatusBinding
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CredentialsStatusPage : AppCompatActivity() {
    private lateinit var view: ActivityCredentialsStatusBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivityCredentialsStatusBinding.inflate(layoutInflater)
        setContentView(view.root)
        view.signOutButton.setOnClickListener {
            signOut()
        }
        displayMessage(intent.getStringExtra("message"))
        navigate()
    }

    private fun signOut() {
        lifecycleScope.launch {
            Amplify.Auth.signOut()
            view.tokenInfo.visibility = INVISIBLE
            displayMessage("Signed out!")
        }
    }

    private fun navigate() {
        lifecycleScope.launch(Dispatchers.IO) {
            when (val session = Amplify.Auth.fetchAuthSession()) {
                is ValidSession -> displayTokens(session)
                else -> goToSignIn(source = this@CredentialsStatusPage)
            }
        }
    }

    private fun displayMessage(text: String?) {
        if (text != null) {
            view.message.text = text
            view.message.visibility = VISIBLE
        } else {
            view.message.visibility = INVISIBLE
        }
    }

    private fun displayTokens(session: ValidSession) {
        Log.i("CredentialStatus", "ID token = ${session.idToken}")
        Log.i("CredentialStatus", "Access token = ${session.accessToken}")

        view.accessToken.text = session.accessToken
        view.idToken.text = session.idToken
        view.tokenInfo.visibility = VISIBLE
    }
}

fun goToCredentialsStatus(origin: Activity, message: String) {
    val intent = Intent(origin, CredentialsStatusPage::class.java)
    intent.putExtra("message", message)
    origin.startActivity(intent)
}
