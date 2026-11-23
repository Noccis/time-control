package com.example.time_controll.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.time_controll.data.AlarmRepository

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        Log.d(TAG, "onReceive action=$action, alarmId=$alarmId")

        when (action) {
            AlarmScheduler.ACTION_TRIGGER_ALARM -> {
                if (alarmId != -1L) {
                    val repository = AlarmRepository.getInstance(context)
                    val alarm = repository.getAlarm(alarmId)
                    if (alarm != null && alarm.isEnabled) {
                        // Start Alarm Service
                        val serviceIntent = Intent(context, AlarmService::class.java).apply {
                            this.action = AlarmService.ACTION_START_ALARM
                            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                        }
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start AlarmService: ${e.message}")
                        }

                        // Reschedule if repeating, or disable if one-time
                        if (alarm.isRepeating && alarm.repeatDays.isNotEmpty()) {
                            AlarmScheduler.schedule(context, alarm)
                        } else {
                            repository.toggleAlarm(alarmId, false)
                        }
                    }
                }
            }
            AlarmScheduler.ACTION_DISMISS_ALARM -> {
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_STOP_ALARM
                }
                context.startService(serviceIntent)
            }
            AlarmScheduler.ACTION_SNOOZE_ALARM -> {
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_STOP_ALARM
                }
                context.startService(serviceIntent)
                if (alarmId != -1L) {
                    AlarmScheduler.snooze(context, alarmId, 10)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
