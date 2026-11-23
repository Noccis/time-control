package com.example.time_controll.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.time_controll.data.model.AlarmFolder
import com.example.time_controll.data.model.AlarmItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditDialog(
    alarm: AlarmItem,
    folders: List<AlarmFolder>,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit,
    onDelete: (() -> Unit)? = null,
    onCreateFolderClick: () -> Unit,
) {
    val context = LocalContext.current
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)

    var name by remember { mutableStateOf(alarm.name) }
    var selectedFolderId by remember { mutableStateOf(alarm.folderId) }
    var isRepeating by remember { mutableStateOf(alarm.isRepeating) }
    var repeatDays by remember { mutableStateOf(alarm.repeatDays) }
    var vibrate by remember { mutableStateOf(alarm.vibrate) }
    var soundUri by remember { mutableStateOf(alarm.soundUri) }
    var soundTitle by remember { mutableStateOf(alarm.soundTitle) }
    var showTimeInputMode by remember { mutableStateOf(value = false) }

    // Security rationale dialog state
    var showPermissionRationaleDialog by remember { mutableStateOf(value = false) }

    // Media preview state
    var isPlayingPreview by remember { mutableStateOf(value = false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
        }
    }

    fun stopAudioPreview() {
        if (isPlayingPreview) {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
            isPlayingPreview = false
        }
    }

    fun toggleAudioPreview() {
        if (isPlayingPreview) {
            stopAudioPreview()
        } else {
            try {
                previewPlayer?.release()
                val uriToPlay = if (!soundUri.isNullOrBlank()) {
                    Uri.parse(soundUri)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }

                previewPlayer = MediaPlayer().apply {
                    setDataSource(context, uriToPlay)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setOnCompletionListener {
                        isPlayingPreview = false
                    }
                    prepare()
                    start()
                }
                isPlayingPreview = true
            } catch (e: Exception) {
                e.printStackTrace()
                isPlayingPreview = false
            }
        }
    }

    // Storage / Document picker launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Security requirement: Take persistable permission so background service/reboot can read the file
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            soundUri = uri.toString()
            soundTitle = getDisplayNameFromUri(context, uri)
            stopAudioPreview()
        }
    }

    // Permission launcher for storage/media
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openDocumentLauncher.launch(arrayOf("audio/*", "application/ogg"))
        }
    }

    fun onBrowseAudioClicked() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            requiredPermission
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            openDocumentLauncher.launch(arrayOf("audio/*", "application/ogg"))
        } else {
            // Show rationale/security question before requesting permission
            showPermissionRationaleDialog = true
        }
    }

    val timePickerState = rememberTimePickerState(
        initialHour = alarm.hour,
        initialMinute = alarm.minute,
        is24Hour = is24Hour
    )

    val weekdays = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )

    AlertDialog(
        onDismissRequest = {
            stopAudioPreview()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNew) "Add Alarm" else "Edit Alarm",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { showTimeInputMode = !showTimeInputMode }) {
                    Icon(
                        imageVector = if (showTimeInputMode) Icons.Default.Schedule else Icons.Default.Keyboard,
                        contentDescription = "Toggle Time Picker Mode"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time Picker
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (showTimeInputMode) {
                        TimeInput(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors()
                        )
                    } else {
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alarm Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Alarm Name (optional)") },
                    placeholder = { Text("e.g. Work, Medication, Gym") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Folder Selection Section
                Text(
                    text = "Folder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "No Folder" chip
                    FilterChip(
                        selected = selectedFolderId == null,
                        onClick = { selectedFolderId = null },
                        label = { Text("No Folder") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Existing folders chips
                    folders.forEach { folder ->
                        val isSelected = selectedFolderId == folder.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFolderId = folder.id },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(folder.colorHex), shape = CircleShape)
                                )
                            },
                            label = { Text(folder.name) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(folder.colorHex).copy(alpha = 0.25f),
                                selectedLabelColor = Color(folder.colorHex)
                            )
                        )
                    }

                    // "+ New Folder" action chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCreateFolderClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New Folder",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Repeat Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Repeat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isRepeating) "Repeats weekly" else "Ring once only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isRepeating,
                        onCheckedChange = { checked ->
                            isRepeating = checked
                            if (checked && repeatDays.isEmpty()) {
                                repeatDays = setOf(1, 2, 3, 4, 5) // Default to weekdays
                            }
                        }
                    )
                }

                // Weekday selection if repeat enabled
                if (isRepeating) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekdays.forEach { (dayInt, label) ->
                            val isDaySelected = repeatDays.contains(dayInt)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDaySelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        repeatDays = if (isDaySelected) {
                                            repeatDays - dayInt
                                        } else {
                                            repeatDays + dayInt
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label.first().toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDaySelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset buttons: Weekdays, Weekends, All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { repeatDays = setOf(1, 2, 3, 4, 5) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Weekdays", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { repeatDays = setOf(6, 7) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Weekends", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { repeatDays = setOf(1, 2, 3, 4, 5, 6, 7) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Every day", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Sound / Music Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alarm Sound",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Play / Preview button
                            IconButton(
                                onClick = { toggleAudioPreview() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingPreview) "Stop Preview" else "Play Preview",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Current sound label
                        Text(
                            text = soundTitle ?: "Default Alarm Tone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onBrowseAudioClicked() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Browse Files")
                            }

                            if (soundUri != null) {
                                TextButton(
                                    onClick = {
                                        soundUri = null
                                        soundTitle = null
                                        stopAudioPreview()
                                    }
                                ) {
                                    Text("Reset to Default")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vibrate toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Vibrate",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    stopAudioPreview()
                    val updated = alarm.copy(
                        name = name.trim(),
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                        isEnabled = true,
                        isRepeating = isRepeating && repeatDays.isNotEmpty(),
                        repeatDays = if (isRepeating) repeatDays else emptySet(),
                        folderId = selectedFolderId,
                        vibrate = vibrate,
                        soundUri = soundUri,
                        soundTitle = soundTitle
                    )
                    onSave(updated)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if ((!isNew) && (onDelete != null)) {
                    TextButton(
                        onClick = {
                            stopAudioPreview()
                            onDelete()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
                TextButton(
                    onClick = {
                        stopAudioPreview()
                        onDismiss()
                    },
                ) {
                    Text("Cancel")
                }
            }
        },
    )

    // Security & Rationale Dialog
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Allow Audio File Access?")
            },
            text = {
                Text(
                    "Time Control needs permission to read audio files on your device so that you can select your own custom music or sound for this alarm.\n\n" +
                    "Your privacy is protected: only the sound file you select will be used by the app to ring your alarm."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        requestPermissionLauncher.launch(requiredPermission)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun getDisplayNameFromUri(context: Context, uri: Uri): String {
    var displayName: String? = null
    try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    displayName = cursor.getString(nameIndex)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return displayName ?: uri.lastPathSegment ?: "Custom Audio File"
}
