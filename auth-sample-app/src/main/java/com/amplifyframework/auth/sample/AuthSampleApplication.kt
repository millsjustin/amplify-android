package com.amplifyframework.auth.sample

import android.app.Application
import com.amplifyframework.auth.AWSAuthPlugin
import com.amplifyframework.kotlin.core.Amplify

class AuthSampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Amplify.addPlugin(AWSAuthPlugin())
        Amplify.configure(applicationContext)
    }
}
