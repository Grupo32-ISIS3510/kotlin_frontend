package com.app.secondserving

import android.app.Application
import com.app.secondserving.data.SessionManager
import com.app.secondserving.data.network.RetrofitClient

class SecondServingApp : Application() {

    lateinit var sessionManager: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)
    }
}
