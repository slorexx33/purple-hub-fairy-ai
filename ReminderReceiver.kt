package com.purplehub.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

internal object ReminderScheduler {
    private const val CHANNEL_ID = "purple_hub_reminders"
    private const val EXTRA_ID = "reminder_id"
    private const val EXTRA_TITLE = "reminder_title"

    fun hasExactAlarmAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return manager.canScheduleExactAlarms()
    }

    /** Returns true only when the alarm was scheduled exactly. */
    fun schedule(context: Context, reminder: HubReminder): Boolean {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_ID, reminder.id)
            putExtra(EXTRA_TITLE, reminder.title)
        }
        val pending = PendingIntent.getBroadcast(
            context, reminder.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val exact = hasExactAlarmAccess(context)
        if (exact) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.at, pending)
                return true
            } catch (_: SecurityException) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.at, pending)
                return false
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.at, pending)
        return false
    }

    fun cancel(context: Context, reminder: HubReminder) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, reminder.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.cancel(pending)
        pending.cancel()
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("reminder_title")?.take(120).orEmpty().ifBlank { "Напоминание Purple Hub" }
        val reminderId = intent.getLongExtra("reminder_id", System.currentTimeMillis())
        val id = reminderId.toInt()
        val prefs = HubPrefs(context)
        prefs.saveReminders(prefs.loadReminders().filterNot { it.id == reminderId })
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel("purple_hub_reminders", "Напоминания Purple Hub", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, "purple_hub_reminders")
            .setSmallIcon(com.purplehub.app.R.drawable.ic_stat_purple)
            .setContentTitle("Напоминание")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try { manager.notify(id, notification) } catch (_: SecurityException) { }
    }
}


class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        HubPrefs(context).loadReminders().filter { it.at > System.currentTimeMillis() }
            .forEach { ReminderScheduler.schedule(context, it) }
    }
}
