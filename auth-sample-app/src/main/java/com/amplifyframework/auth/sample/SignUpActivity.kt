package com.amplifyframework.auth.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.step.AuthSignUpStep.CONFIRM_SIGN_UP_STEP
import com.amplifyframework.auth.result.step.AuthSignUpStep.DONE
import com.amplifyframework.auth.sample.databinding.ActivitySignUpBinding
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {
    private lateinit var view: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(view.root)
        view.submitButton.setOnClickListener { submitSignIn() }
    }

    private fun submitSignIn() {
        lifecycleScope.launch {
            val username = view.username.text.toString()
            val password = view.password.text.toString()
            val options = AuthSignUpOptions.builder()
                .userAttribute(AuthUserAttributeKey.email(), view.email.text.toString())
                .build()
            val result = withContext(Dispatchers.IO) {
                Amplify.Auth.signUp(username, password, options)
            }
            when (result.nextStep.signUpStep) {
                DONE -> goToSignIn(this@SignUpActivity, username)
                CONFIRM_SIGN_UP_STEP -> goToConfirmSignUp(this@SignUpActivity, username)
            }
        }
    }
}


fun goToSignUp(source: Activity) {
    val signUpIntent = Intent(source, SignUpActivity::class.java)
    source.startActivity(signUpIntent)
}
