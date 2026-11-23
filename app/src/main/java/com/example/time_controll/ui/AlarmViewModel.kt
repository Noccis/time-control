package com.example.time_controll.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.time_controll.data.AlarmRepository
import com.example.time_controll.data.model.AlarmFolder
import com.example.time_controll.data.model.AlarmItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenView {
    object FoldersHub : ScreenView()
    data class FolderDetail(val folderId: String) : ScreenView()
    object AllAlarms : ScreenView()
}

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AlarmRepository.getInstance(application)

    val alarms: StateFlow<List<AlarmItem>> = repository.alarms
    val folders: StateFlow<List<AlarmFolder>> = repository.folders

    private val _currentView = MutableStateFlow<ScreenView>(ScreenView.FoldersHub)
    val currentView: StateFlow<ScreenView> = _currentView.asStateFlow()

    // Alarm Editor State
    private val _editingAlarm = MutableStateFlow<AlarmItem?>(null)
    val editingAlarm: StateFlow<AlarmItem?> = _editingAlarm.asStateFlow()
    private val _isNewAlarm = MutableStateFlow(false)
    val isNewAlarm: StateFlow<Boolean> = _isNewAlarm.asStateFlow()

    // Folder Editor State
    private val _editingFolder = MutableStateFlow<AlarmFolder?>(null)
    val editingFolder: StateFlow<AlarmFolder?> = _editingFolder.asStateFlow()
    private val _isCreatingFolder = MutableStateFlow(false)
    val isCreatingFolder: StateFlow<Boolean> = _isCreatingFolder.asStateFlow()

    // Confirm dialogs
    private val _alarmToDelete = MutableStateFlow<AlarmItem?>(null)
    val alarmToDelete: StateFlow<AlarmItem?> = _alarmToDelete.asStateFlow()

    private val _folderToDelete = MutableStateFlow<AlarmFolder?>(null)
    val folderToDelete: StateFlow<AlarmFolder?> = _folderToDelete.asStateFlow()

    fun navigateTo(view: ScreenView) {
        _currentView.value = view
    }

    fun openCreateAlarm(preselectedFolderId: String? = null) {
        val folderId = when {
            preselectedFolderId != null -> preselectedFolderId
            _currentView.value is ScreenView.FolderDetail -> (_currentView.value as ScreenView.FolderDetail).folderId
            else -> folders.value.firstOrNull()?.id
        }
        _editingAlarm.value = AlarmItem(
            id = System.currentTimeMillis(),
            name = "",
            hour = 8,
            minute = 0,
            isEnabled = true,
            isRepeating = false,
            repeatDays = setOf(1, 2, 3, 4, 5),
            folderId = folderId
        )
        _isNewAlarm.value = true
    }

    fun openEditAlarm(alarm: AlarmItem) {
        _editingAlarm.value = alarm
        _isNewAlarm.value = false
    }

    fun dismissAlarmEditor() {
        _editingAlarm.value = null
    }

    fun saveAlarm(alarm: AlarmItem) {
        if (_isNewAlarm.value) {
            repository.addAlarm(alarm)
        } else {
            repository.updateAlarm(alarm)
        }
        _editingAlarm.value = null
    }

    fun toggleAlarm(alarmId: Long, isEnabled: Boolean) {
        repository.toggleAlarm(alarmId, isEnabled)
    }

    fun requestDeleteAlarm(alarm: AlarmItem) {
        _alarmToDelete.value = alarm
    }

    fun confirmDeleteAlarm() {
        _alarmToDelete.value?.let {
            repository.deleteAlarm(it.id)
        }
        _alarmToDelete.value = null
        if (_editingAlarm.value != null) {
            _editingAlarm.value = null
        }
    }

    fun cancelDeleteAlarm() {
        _alarmToDelete.value = null
    }

    fun openCreateFolder() {
        _isCreatingFolder.value = true
        _editingFolder.value = AlarmFolder(
            name = "",
            colorHex = AlarmFolder.PRESET_COLORS.random()
        )
    }

    fun openEditFolder(folder: AlarmFolder) {
        _isCreatingFolder.value = false
        _editingFolder.value = folder
    }

    fun dismissFolderEditor() {
        _editingFolder.value = null
        _isCreatingFolder.value = false
    }

    fun saveFolder(name: String, colorHex: Long) {
        val current = _editingFolder.value
        if (_isCreatingFolder.value || current == null) {
            repository.addFolder(name = name, colorHex = colorHex)
        } else {
            repository.updateFolder(current.copy(name = name, colorHex = colorHex))
        }
        dismissFolderEditor()
    }

    fun requestDeleteFolder(folder: AlarmFolder) {
        _folderToDelete.value = folder
    }

    fun confirmDeleteFolder() {
        _folderToDelete.value?.let { folder ->
            repository.deleteFolder(folder.id)
            if (_currentView.value is ScreenView.FolderDetail &&
                (_currentView.value as ScreenView.FolderDetail).folderId == folder.id
            ) {
                _currentView.value = ScreenView.FoldersHub
            }
        }
        _folderToDelete.value = null
    }

    fun cancelDeleteFolder() {
        _folderToDelete.value = null
    }
}
