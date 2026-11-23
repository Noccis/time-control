package com.example.time_controll.data

import android.content.Context
import android.content.SharedPreferences
import com.example.time_controll.alarm.AlarmScheduler
import com.example.time_controll.data.model.AlarmFolder
import com.example.time_controll.data.model.AlarmItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AlarmRepository private constructor(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _folders = MutableStateFlow<List<AlarmFolder>>(emptyList())
    val folders: StateFlow<List<AlarmFolder>> = _folders.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val foldersJson = prefs.getString(KEY_FOLDERS, null)
        val loadedFolders = mutableListOf<AlarmFolder>()
        if (foldersJson != null) {
            try {
                val array = JSONArray(foldersJson)
                for (i in 0 until array.length()) {
                    loadedFolders.add(AlarmFolder.fromJson(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Seed default initial folders
            loadedFolders.addAll(
                listOf(
                    AlarmFolder(name = "Morning", colorHex = 0xFFFF9800), // Orange
                    AlarmFolder(name = "Work", colorHex = 0xFF2196F3),    // Blue
                    AlarmFolder(name = "Fitness", colorHex = 0xFF4CAF50)  // Green
                )
            )
            saveFoldersInternal(loadedFolders)
        }
        _folders.value = loadedFolders

        val alarmsJson = prefs.getString(KEY_ALARMS, null)
        val loadedAlarms = mutableListOf<AlarmItem>()
        if (alarmsJson != null) {
            try {
                val array = JSONArray(alarmsJson)
                for (i in 0 until array.length()) {
                    loadedAlarms.add(AlarmItem.fromJson(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Seed a starter alarm
            val firstFolderId = loadedFolders.firstOrNull()?.id
            loadedAlarms.add(
                AlarmItem(
                    id = System.currentTimeMillis(),
                    name = "Wake up",
                    hour = 7,
                    minute = 0,
                    isEnabled = false,
                    isRepeating = true,
                    repeatDays = setOf(1, 2, 3, 4, 5), // Weekdays
                    folderId = firstFolderId
                )
            )
            saveAlarmsInternal(loadedAlarms)
        }
        _alarms.value = loadedAlarms
    }

    private fun saveAlarmsInternal(list: List<AlarmItem>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_ALARMS, array.toString()).apply()
    }

    private fun saveFoldersInternal(list: List<AlarmFolder>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_FOLDERS, array.toString()).apply()
    }

    fun addAlarm(alarm: AlarmItem) {
        val updated = _alarms.value.toMutableList().apply { add(alarm) }
        _alarms.value = updated
        saveAlarmsInternal(updated)
        if (alarm.isEnabled) {
            AlarmScheduler.schedule(context, alarm)
        }
    }

    fun updateAlarm(alarm: AlarmItem) {
        val updated = _alarms.value.map {
            if (it.id == alarm.id) alarm else it
        }
        _alarms.value = updated
        saveAlarmsInternal(updated)
        if (alarm.isEnabled) {
            AlarmScheduler.schedule(context, alarm)
        } else {
            AlarmScheduler.cancel(context, alarm.id)
        }
    }

    fun deleteAlarm(alarmId: Long) {
        AlarmScheduler.cancel(context, alarmId)
        val updated = _alarms.value.filter { it.id != alarmId }
        _alarms.value = updated
        saveAlarmsInternal(updated)
    }

    fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        val alarm = _alarms.value.find { it.id == alarmId } ?: return
        val updatedAlarm = alarm.copy(isEnabled = enabled)
        updateAlarm(updatedAlarm)
    }

    fun getAlarm(alarmId: Long): AlarmItem? {
        return _alarms.value.find { it.id == alarmId }
    }

    fun addFolder(name: String, colorHex: Long): AlarmFolder {
        val newFolder = AlarmFolder(name = name, colorHex = colorHex)
        val updated = _folders.value.toMutableList().apply { add(newFolder) }
        _folders.value = updated
        saveFoldersInternal(updated)
        return newFolder
    }

    fun updateFolder(folder: AlarmFolder) {
        val updated = _folders.value.map {
            if (it.id == folder.id) folder else it
        }
        _folders.value = updated
        saveFoldersInternal(updated)
    }

    fun deleteFolder(folderId: String) {
        // Move any alarms in this folder to null (unfiled)
        val updatedAlarms = _alarms.value.map {
            if (it.folderId == folderId) it.copy(folderId = null) else it
        }
        _alarms.value = updatedAlarms
        saveAlarmsInternal(updatedAlarms)

        val updatedFolders = _folders.value.filter { it.id != folderId }
        _folders.value = updatedFolders
        saveFoldersInternal(updatedFolders)
    }

    fun getFolder(folderId: String?): AlarmFolder? {
        if (folderId == null) return null
        return _folders.value.find { it.id == folderId }
    }

    fun rescheduleAllAlarms() {
        _alarms.value.filter { it.isEnabled }.forEach {
            AlarmScheduler.schedule(context, it)
        }
    }

    companion object {
        private const val PREFS_NAME = "time_controll_alarms"
        private const val KEY_ALARMS = "saved_alarms"
        private const val KEY_FOLDERS = "saved_folders"

        @Volatile
        private var instance: AlarmRepository? = null

        fun getInstance(context: Context): AlarmRepository {
            return instance ?: synchronized(this) {
                instance ?: AlarmRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
