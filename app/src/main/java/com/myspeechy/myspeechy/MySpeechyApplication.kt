package com.myspeechy.myspeechy

import android.app.Application
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MySpeechyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        //init persistence here since when enabling it in MainActivity onCreate
        //and quitting the app by pressing back button crashes it
        Firebase.database.setPersistenceEnabled(true)
    }
}