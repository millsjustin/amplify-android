package com.amplifyframework.auth.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amplifyframework.auth.ValidSession
import com.amplifyframework.auth.sample.databinding.ActivityLandingPageBinding
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.coroutines.launch

class LandingPageActivity : AppCompatActivity() {
    private lateinit var view: ActivityLandingPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivityLandingPageBinding.inflate(layoutInflater)
        setContentView(view.root)
        view.message.text = intent.getStringExtra("message")
        displayTokens()
    }

    private fun displayTokens() {
        lifecycleScope.launch {
            when (val session = Amplify.Auth.fetchAuthSession()) {
                is ValidSession -> {
                    view.accessToken.text = session.accessToken
                    view.idToken.text = session.idToken
                }
                else -> {
                    view.accessToken.text = "Failed to obtain access token."
                    view.idToken.text = "Failed to obtain ID token."
                }
            }
        }
    }
}

fun goToLandingPage(origin: Activity, message: String) {
    val intent = Intent(origin, LandingPageActivity::class.java)
    intent.putExtra("message", message)
    origin.startActivity(intent)
}
