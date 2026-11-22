package com.example.myreminderai

import android.app.*
import android.content.*
import android.os.Build
import org.json.JSONArray


class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra("text") ?: "Reminder"
        val id = intent?.getIntExtra("id", 0) ?: 0
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("reminders", "Reminders", NotificationManager.IMPORTANCE_HIGH))
        val n = Notification.Builder(context, if (Build.VERSION.SDK_INT >= 26) "reminders" else "")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("My Reminder AI").setContentText(title)
            .setAutoCancel(true).build()
        manager.notify(id, n)
    }
}
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        val items = try { JSONArray(prefs.getString("items", "[]")) } catch (_: Exception) { JSONArray() }
        for (i in 0 until items.length()) { val x = items.getJSONObject(i); if (!x.optBoolean("done") && x.optLong("time") > System.currentTimeMillis()) ReminderScheduler.schedule(context, x.optInt("id"), x.optString("text"), x.optLong("time")) }
    }
}
object ReminderScheduler {
    fun schedule(context: Context, id: Int, text: String, time: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra("id", id).putExtra("text", text)
        val pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi) else am.setExact(AlarmManager.RTC_WAKEUP, time, pi)
    }
    fun cancel(context: Context, id: Int) { val pi = PendingIntent.getBroadcast(context, id, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi) }
}
