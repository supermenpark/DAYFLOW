package com.dayflow.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "DAYFLOW"
        val text = intent.getStringExtra("text") ?: ""
        val id = intent.getIntExtra("id", 0)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "dayflow_alarm"
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(channelId, "DAYFLOW 알림", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(ch)
        }

        val open = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, id, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val b = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, channelId)
        } else {
            Notification.Builder(context)
        }
        b.setSmallIcon(android.R.drawable.ic_popup_reminder)
        b.setContentTitle(title)
        b.setContentText(text)
        b.setContentIntent(pi)
        b.setAutoCancel(true)

        nm.notify(id, b.build())
    }
}
