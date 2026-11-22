package com.example.myreminderai

import java.util.Calendar

object ReminderParser {
    // Returns cleaned title and local-time timestamp for simple Hindi/Hinglish date phrases.
    fun parse(raw: String): Pair<String, Long>? {
        val s = raw.trim().lowercase()
        val cal = Calendar.getInstance()
        when {
            s.contains("tomorrow") || s.contains("कल") -> cal.add(Calendar.DAY_OF_YEAR, 1)
            s.contains("today") || s.contains("आज") -> Unit
            else -> return null
        }
        val timeRegex = Regex("(?i)(\\d{1,2})(?:[:.](\\d{2}))?\\s*(am|pm|बजे|baje)?")
        val matches = timeRegex.findAll(s).toList()
        val timeMatch = matches.lastOrNull { m ->
            val hr = m.groupValues[1].toIntOrNull() ?: 0
            hr in 1..12 || m.groupValues[2].isNotEmpty()
        } ?: return null
        var hour = timeMatch.groupValues[1].toIntOrNull() ?: return null
        val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
        val marker = timeMatch.groupValues[3].lowercase()
        if (marker == "pm" || s.contains("शाम") || s.contains("रात")) { if (hour < 12) hour += 12 }
        else if (marker == "am" || s.contains("सुबह")) { if (hour == 12) hour = 0 }
        if (hour !in 0..23 || minute !in 0..59) return null
        cal.set(Calendar.HOUR_OF_DAY, hour); cal.set(Calendar.MINUTE, minute); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        var title = raw.trim()
        title = title.replace(Regex("(?i)\\b(today|tomorrow|आज|कल|सुबह|शाम|रात|\\d{1,2}([:.]?\\d{2})?\\s*(am|pm|बजे|baje)?)\\b"), " ")
            .replace(Regex("\\s+"), " ").trim().trim(',', '.', '।')
        if (title.isBlank()) title = raw.trim()
        return title to cal.timeInMillis
    }
}
