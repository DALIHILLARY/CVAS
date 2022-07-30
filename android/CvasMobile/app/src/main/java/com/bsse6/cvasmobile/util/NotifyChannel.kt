package com.bsse6.cvasmobile.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class NotifyChannel {
    companion object{
        const val CHANNEL_ID = "com.bsse6.cvasmobile"
        const val name = "com.bsse6.cvasmobile"
        const val descriptionText = "CVAS Blind Aid System"
        const val importance = NotificationManager.IMPORTANCE_DEFAULT

        fun createNotificationChannel(mContext: Context) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                // Register the channel with the system
                val notificationManager: NotificationManager =
                    mContext.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

    }
}