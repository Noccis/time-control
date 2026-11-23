package com.example.time_controll.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.time_controll.MainActivity
import com.example.time_controll.data.model.AlarmItem

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    const val ACTION_TRIGGER_ALARM = "com.example.time_controll.ACTION_TRIGGER_ALARM"
    const val ACTION_DISMISS_ALARM = "com.example.time_controll.ACTION_DISMISS_ALARM"
    const val ACTION_SNOOZE_ALARM = "com.example.time_controll.ACTION_SNOOZE_ALARM"
    const val EXTRA_ALARM_ID = "extra_alarm_id"

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    fun schedule(context: Context, alarm: AlarmItem) {
        if (!alarm.isEnabled) {
            cancel(context, alarm.id)
            return
        }

        val alarmManager = (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager) ?: return
        val triggerMillis = alarm.calculateNextTriggerMillis()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerMillis, showIntent),
                    pendingIntent,
                )
            }
            Log.d(TAG, "Scheduled alarm ${alarm.id} at $triggerMillis")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling alarm: ${e.message}")
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent,
                )
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed fallback alarm scheduling: ${fallbackEx.message}")
            }
        }
    }

    fun snooze(context: Context, alarmId: Long, snoozeMinutes: Int = 10) {
        val alarmManager = (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager) ?: return
        val triggerMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val showIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerMillis, showIntent),
                pendingIntent,
            )
        } catch (_: Exception) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent,
            )
        }
    }

    fun cancel(context: Context, alarmId: Long) {
        val alarmManager = (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager) ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled alarm $alarmId")
    }
}
