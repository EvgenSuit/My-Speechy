package com.example.myspeechy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myspeechy.domain.NotificationRepository

class MeditationNotificationOnCancelReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationRepository.canceled.value = true
    }
}