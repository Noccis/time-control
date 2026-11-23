package com.example.time_controll

import com.example.time_controll.data.model.AlarmFolder
import com.example.time_controll.data.model.AlarmItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmItemTest {

    @Test
    fun testFormattedTime_24Hour_European() {
        val alarmMorning = AlarmItem(hour = 7, minute = 5)
        assertEquals("07:05", alarmMorning.formattedTime(use24Hour = true))

        val alarmAfternoon = AlarmItem(hour = 14, minute = 30)
        assertEquals("14:30", alarmAfternoon.formattedTime(use24Hour = true))

        val alarmEvening = AlarmItem(hour = 23, minute = 45)
        assertEquals("23:45", alarmEvening.formattedTime(use24Hour = true))

        val alarmMidnight = AlarmItem(hour = 0, minute = 0)
        assertEquals("00:00", alarmMidnight.formattedTime(use24Hour = true))
    }

    @Test
    fun testFormattedTime_12Hour() {
        val alarmMorning = AlarmItem(hour = 7, minute = 5)
        assertEquals("7:05", alarmMorning.formattedTime(use24Hour = false))
        assertEquals("AM", alarmMorning.amPmMarker())

        val alarmNoon = AlarmItem(hour = 12, minute = 0)
        assertEquals("12:00", alarmNoon.formattedTime(use24Hour = false))
        assertEquals("PM", alarmNoon.amPmMarker())

        val alarmEvening = AlarmItem(hour = 23, minute = 45)
        assertEquals("11:45", alarmEvening.formattedTime(use24Hour = false))
        assertEquals("PM", alarmEvening.amPmMarker())

        val alarmMidnight = AlarmItem(hour = 0, minute = 0)
        assertEquals("12:00", alarmMidnight.formattedTime(use24Hour = false))
        assertEquals("AM", alarmMidnight.amPmMarker())
    }

    @Test
    fun testRepeatSummary() {
        val nonRepeating = AlarmItem(hour = 8, minute = 0, isRepeating = false)
        assertEquals("Once", nonRepeating.repeatSummary())

        val weekdays = AlarmItem(
            hour = 8,
            minute = 0,
            isRepeating = true,
            repeatDays = setOf(1, 2, 3, 4, 5),
        )
        assertEquals("Weekdays", weekdays.repeatSummary())

        val weekends = AlarmItem(
            hour = 8,
            minute = 0,
            isRepeating = true,
            repeatDays = setOf(6, 7),
        )
        assertEquals("Weekends", weekends.repeatSummary())

        val everyday = AlarmItem(
            hour = 8,
            minute = 0,
            isRepeating = true,
            repeatDays = setOf(1, 2, 3, 4, 5, 6, 7),
        )
        assertEquals("Every day", everyday.repeatSummary())

        val custom = AlarmItem(
            hour = 8,
            minute = 0,
            isRepeating = true,
            repeatDays = setOf(1, 3, 5),
        )
        assertEquals("Mon, Wed, Fri", custom.repeatSummary())
    }

    @Test
    fun testNextTriggerIsAlwaysInTheFuture() {
        val alarm = AlarmItem(
            hour = 8,
            minute = 0,
            isRepeating = true,
            repeatDays = setOf(1, 2, 3, 4, 5),
        )
        val nextTrigger = alarm.calculateNextTriggerMillis()
        assertTrue("Next trigger should be in the future", nextTrigger > System.currentTimeMillis())
    }

    @Test
    fun testAlarmFolderPresetColors() {
        val folder = AlarmFolder(name = "Work", colorHex = 0xFF2196F3)
        assertEquals("Work", folder.name)
        assertEquals(0xFF2196F3, folder.colorHex)
        assertTrue(AlarmFolder.PRESET_COLORS.isNotEmpty())
    }

    @Test
    fun testAlarmWithCustomSoundProperties() {
        val alarm = AlarmItem(
            hour = 7,
            minute = 30,
            soundUri = "content://media/external/audio/media/123",
            soundTitle = "Morning_Birds.mp3",
        )
        assertEquals("content://media/external/audio/media/123", alarm.soundUri)
        assertEquals("Morning_Birds.mp3", alarm.soundTitle)
    }
}
