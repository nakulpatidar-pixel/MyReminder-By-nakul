package com.example.myreminderai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ReminderStore(context: Context) {
    private val prefs = context.getSharedPreferences("my_reminder_ai", Context.MODE_PRIVATE)
    fun all(): List<ReminderItem> {
        val array = try { JSONArray(prefs.getString("items", "[]")) } catch (_: Exception) { JSONArray() }
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                add(ReminderItem(o.optLong("id"), o.optString("title"), o.optLong("when"), o.optBoolean("done"), o.optBoolean("wa")))
            }
        }
    }
    private fun save(list: List<ReminderItem>) {
        val a = JSONArray()
        list.forEach { item -> a.put(JSONObject().put("id", item.id).put("title", item.title).put("when", item.whenMillis).put("done", item.done).put("wa", item.whatsApp)) }
        prefs.edit().putString("items", a.toString()).apply()
    }
    fun add(item: ReminderItem) { save(all().filterNot { it.id == item.id } + item) }
    fun markDone(id: Long) { save(all().map { if (it.id == id) it.copy(done = true) else it }) }
    fun remove(id: Long) { save(all().filterNot { it.id == id }) }
}
