package com.myspeechy.myspeechy.domain

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.myspeechy.myspeechy.MeditationNotificationOnCancelReceiver
import com.myspeechy.myspeechy.R

interface NotificationService{
    fun sendMeditationNotification(passedTime: String)
    fun cancelNotification()
}
class MeditationNotificationServiceImpl(private val context: Context):
    NotificationService {
        private val id: Int = 1
    private val notificationControlId: Int = 2
    override fun sendMeditationNotification(passedTime: String) {
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationControlId,
            Intent(context, MeditationNotificationOnCancelReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        val notificationBuilder = NotificationCompat.Builder(context, "1")
            .setLargeIcon(BitmapFactory.decodeResource(context.resources ,R.drawable.meditation))
            .setSmallIcon(R.drawable.meditation)
            .setContentTitle("Meditation")
            .setContentText(passedTime)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .addAction(
                R.drawable.meditation,
                "Cancel",
                cancelPendingIntent
            )
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(id,
                notificationBuilder.build()
            )
        }
    }

    override fun cancelNotification() {
        NotificationManagerCompat.from(context).cancel(id)
    }
}
