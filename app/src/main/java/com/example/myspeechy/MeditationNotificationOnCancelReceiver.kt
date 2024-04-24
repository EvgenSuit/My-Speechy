package com.example.myspeechy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myspeechy.data.DataStoreManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MeditationNotificationOnCancelReceiver: BroadcastReceiver() {
    @Named("NotificationModuleDataStore")
    @Inject
    lateinit var dataStoreManager: DataStoreManager
    override fun onReceive(context: Context, intent: Intent?) {
        runBlocking {
            dataStoreManager.editMeditationNotificationStatus(true)
        }
    }
}