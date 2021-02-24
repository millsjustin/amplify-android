package com.amplifyframework.auth.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.amplifyframework.auth.result.step.AuthSignInStep.DONE
import com.amplifyframework.auth.sample.databinding.ActivitySignInBinding
import com.amplifyframework.kotlin.core.Amplify

import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {
    private lateinit var view: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(view.root)
        view.username.setText(intent.getStringExtra("username"))
        view.submitButton.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        lifecycleScope.launch {
            val username = view.username.text.toString()
            val password = view.password.text.toString()
            val result = Amplify.Auth.signIn(username, password)
            when (result.nextStep.signInStep) {
                DONE -> goToLandingPage(this@SignInActivity, "Sign in complete.")
                else -> goToLandingPage(this@SignInActivity, "Unhandled step: ${result.nextStep.signInStep}")
            }
        }
    }
}

fun goToSignIn(source: Activity, username: String) {
    val intent = Intent(source, SignInActivity::class.java)
    intent.putExtra("username", username)
    source.startActivity(intent)
}
