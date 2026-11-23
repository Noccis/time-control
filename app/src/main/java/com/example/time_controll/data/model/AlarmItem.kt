package com.example.time_controll.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

data class AlarmItem(
    val id: Long = System.currentTimeMillis(),
    val name: String = "",
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val isRepeating: Boolean = false,
    val repeatDays: Set<Int> = emptySet(), // 1 = Monday ... 7 = Sunday (DayOfWeek.value)
    val folderId: String? = null,
    val vibrate: Boolean = true,
    val soundUri: String? = null,
    val soundTitle: String? = null,
) {
    fun formattedTime(use24Hour: Boolean = false): String {
        return if (use24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        } else {
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            String.format(Locale.getDefault(), "%d:%02d", h, minute)
        }
    }

    fun amPmMarker(): String {
        return if (hour < 12) "AM" else "PM"
    }

    fun repeatSummary(): String {
        if (!isRepeating || repeatDays.isEmpty()) {
            return "Once"
        }
        if (repeatDays.size == 7) {
            return "Every day"
        }
        if (repeatDays == setOf(1, 2, 3, 4, 5)) {
            return "Weekdays"
        }
        if (repeatDays == setOf(6, 7)) {
            return "Weekends"
        }
        val dayNames = listOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun",
        )
        return dayNames
            .asSequence()
            .filter { (dayInt) -> repeatDays.contains(dayInt) }
            .map { (_, label) -> label }
            .joinToString(", ")
    }

    fun calculateNextTriggerMillis(): Long {
        val now = LocalDateTime.now()
        var candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        if (!isRepeating || repeatDays.isEmpty()) {
            if (!candidate.isAfter(now)) {
                candidate = candidate.plusDays(1)
            }
            return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        for (dayOffset in 0..7) {
            val testTime = candidate.plusDays(dayOffset.toLong())
            val dayOfWeekValue = testTime.dayOfWeek.value
            if (repeatDays.contains(dayOfWeekValue)) {
                if (testTime.isAfter(now)) {
                    return testTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }
        }
        return candidate.plusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("hour", hour)
            put("minute", minute)
            put("isEnabled", isEnabled)
            put("isRepeating", isRepeating)
            val daysArray = JSONArray()
            repeatDays.forEach { daysArray.put(it) }
            put("repeatDays", daysArray)
            if (folderId != null) {
                put("folderId", folderId)
            } else {
                put("folderId", JSONObject.NULL)
            }
            put("vibrate", vibrate)
            if (soundUri != null) {
                put("soundUri", soundUri)
            } else {
                put("soundUri", JSONObject.NULL)
            }
            if (soundTitle != null) {
                put("soundTitle", soundTitle)
            } else {
                put("soundTitle", JSONObject.NULL)
            }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AlarmItem {
            val daysSet = mutableSetOf<Int>()
            val daysArray = json.optJSONArray("repeatDays")
            if (daysArray != null) {
                for (i in 0 until daysArray.length()) {
                    daysSet.add(daysArray.getInt(i))
                }
            }
            val folderIdRaw = if (json.isNull("folderId")) null else json.optString("folderId", "")
            val resolvedFolderId = if (folderIdRaw.isNullOrEmpty()) null else folderIdRaw

            val soundUriRaw = if (json.isNull("soundUri")) null else json.optString("soundUri", "")
            val resolvedSoundUri = if (soundUriRaw.isNullOrEmpty()) null else soundUriRaw

            val soundTitleRaw = if (json.isNull("soundTitle")) null else json.optString("soundTitle", "")
            val resolvedSoundTitle = if (soundTitleRaw.isNullOrEmpty()) null else soundTitleRaw

            return AlarmItem(
                id = json.optLong("id", System.currentTimeMillis()),
                name = json.optString("name", ""),
                hour = json.optInt("hour", 8),
                minute = json.optInt("minute", 0),
                isEnabled = json.optBoolean("isEnabled", true),
                isRepeating = json.optBoolean("isRepeating", false),
                repeatDays = daysSet,
                folderId = resolvedFolderId,
                vibrate = json.optBoolean("vibrate", true),
                soundUri = resolvedSoundUri,
                soundTitle = resolvedSoundTitle
            )
        }
    }
}
