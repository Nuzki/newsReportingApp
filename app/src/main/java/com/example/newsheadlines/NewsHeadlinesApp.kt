package com.example.newsheadlines

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class NewsHeadlinesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("NewsHeadlinesApp", "Initializing Firebase")
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}