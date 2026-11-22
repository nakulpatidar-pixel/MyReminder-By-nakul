package com.example.myreminderai

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object ReminderAlarm {
    const val CHANNEL = "reminder_alerts"
    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply { description = "Scheduled personal reminders" }
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    fun schedule(ctx: Context, item: ReminderItem) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, ReminderReceiver::class.java).putExtra("id", item.id).putExtra("title", item.title).putExtra("wa", item.whatsApp)
        val pi = PendingIntent.getBroadcast(ctx, item.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.whenMillis, pi) }
        catch (_: SecurityException) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.whenMillis, pi) }
    }
    fun cancel(ctx: Context, id: Long) {
        val pi = PendingIntent.getBroadcast(ctx, id.toInt(), Intent(ctx, ReminderReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
    }
}
class ReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", System.currentTimeMillis())
        val title = intent.getStringExtra("title") ?: "Reminder"
        val wa = intent.getBooleanExtra("wa", false)
        ReminderAlarm.createChannel(context)
        val openIntent = if (wa) Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/")) else Intent(context, MainActivity::class.java)
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val content = PendingIntent.getActivity(context, (id + 1).toInt(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, ReminderAlarm.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (wa) "WhatsApp follow-up reminder" else "My Reminder AI")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(content)
            .build()
        NotificationManagerCompat.from(context).notify(id.toInt(), notification)
    }
}
class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderStore(context).all().filter { !it.done && it.whenMillis > System.currentTimeMillis() }.forEach { ReminderAlarm.schedule(context, it) }
        }
    }
}
