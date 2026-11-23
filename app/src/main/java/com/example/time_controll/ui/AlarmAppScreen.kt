package com.example.time_controll.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.time_controll.alarm.AlarmScheduler
import com.example.time_controll.data.model.AlarmFolder
import com.example.time_controll.data.model.AlarmItem
import com.example.time_controll.ui.components.AlarmCard
import com.example.time_controll.ui.components.AlarmEditDialog
import com.example.time_controll.ui.components.ConfirmDeleteDialog
import com.example.time_controll.ui.components.FolderCard
import com.example.time_controll.ui.components.FolderEditDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmAppScreen(
    viewModel: AlarmViewModel = viewModel()
) {
    val context = LocalContext.current
    val alarms by viewModel.alarms.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val currentView by viewModel.currentView.collectAsState()

    val editingAlarm by viewModel.editingAlarm.collectAsState()
    val isNewAlarm by viewModel.isNewAlarm.collectAsState()
    val editingFolder by viewModel.editingFolder.collectAsState()
    val isCreatingFolder by viewModel.isCreatingFolder.collectAsState()

    val alarmToDelete by viewModel.alarmToDelete.collectAsState()
    val folderToDelete by viewModel.folderToDelete.collectAsState()

    // Android 13+ Notification permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Handled */ }
    )

    var hasExactAlarmPermission by remember {
        mutableStateOf(AlarmScheduler.canScheduleExactAlarms(context))
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        hasExactAlarmPermission = AlarmScheduler.canScheduleExactAlarms(context)
    }

    // Handle back button when in FolderDetail view
    BackHandler(enabled = currentView is ScreenView.FolderDetail) {
        viewModel.navigateTo(ScreenView.FoldersHub)
    }

    Scaffold(
        topBar = {
            AlarmTopBar(
                currentView = currentView,
                folders = folders,
                onBackClick = { viewModel.navigateTo(ScreenView.FoldersHub) },
                onEditFolderClick = { folder -> viewModel.openEditFolder(folder) },
                onDeleteFolderClick = { folder -> viewModel.requestDeleteFolder(folder) }
            )
        },
        floatingActionButton = {
            AlarmFab(
                currentView = currentView,
                onAddAlarm = { viewModel.openCreateAlarm() },
                onCreateFolder = { viewModel.openCreateFolder() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Exact Alarm permission banner if needed
            if (!hasExactAlarmPermission) {
                ExactAlarmPermissionBanner(
                    onOpenSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                )
            }

            // Tabs to switch between "Folders" and "All Alarms" (only shown when not inside a folder detail)
            if (currentView !is ScreenView.FolderDetail) {
                val selectedTabIndex = if (currentView is ScreenView.FoldersHub) 0 else 1
                SecondaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { viewModel.navigateTo(ScreenView.FoldersHub) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Folders", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { viewModel.navigateTo(ScreenView.AllAlarms) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("All Alarms (${alarms.size})", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
            }

            // Main Content Area based on View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (val view = currentView) {
                    is ScreenView.FoldersHub -> {
                        FoldersHubContent(
                            folders = folders,
                            alarms = alarms,
                            onFolderClick = { folderId ->
                                viewModel.navigateTo(ScreenView.FolderDetail(folderId))
                            },
                            onEditFolder = { folder -> viewModel.openEditFolder(folder) },
                            onDeleteFolder = { folder -> viewModel.requestDeleteFolder(folder) },
                            onCreateFolder = { viewModel.openCreateFolder() }
                        )
                    }

                    is ScreenView.FolderDetail -> {
                        val folder = folders.find { it.id == view.folderId }
                        FolderDetailContent(
                            folder = folder,
                            alarms = alarms.filter { it.folderId == view.folderId },
                            onAlarmClick = { alarm -> viewModel.openEditAlarm(alarm) },
                            onToggleAlarm = { id, enabled -> viewModel.toggleAlarm(id, enabled) },
                            onDeleteAlarm = { alarm -> viewModel.requestDeleteAlarm(alarm) },
                            onAddAlarm = { viewModel.openCreateAlarm(preselectedFolderId = view.folderId) }
                        )
                    }

                    is ScreenView.AllAlarms -> {
                        AllAlarmsContent(
                            alarms = alarms,
                            folders = folders,
                            onAlarmClick = { alarm -> viewModel.openEditAlarm(alarm) },
                            onToggleAlarm = { id, enabled -> viewModel.toggleAlarm(id, enabled) },
                            onDeleteAlarm = { alarm -> viewModel.requestDeleteAlarm(alarm) },
                            onAddAlarm = { viewModel.openCreateAlarm() }
                        )
                    }
                }
            }
        }
    }

    // Alarm Edit Dialog
    editingAlarm?.let { alarm ->
        AlarmEditDialog(
            alarm = alarm,
            folders = folders,
            isNew = isNewAlarm,
            onDismiss = { viewModel.dismissAlarmEditor() },
            onSave = { updatedAlarm -> viewModel.saveAlarm(updatedAlarm) },
            onDelete = if (!isNewAlarm) {
                { viewModel.requestDeleteAlarm(alarm) }
            } else null,
            onCreateFolderClick = { viewModel.openCreateFolder() }
        )
    }

    // Folder Edit Dialog
    if (editingFolder != null) {
        FolderEditDialog(
            folder = editingFolder!!,
            isNew = isCreatingFolder,
            onDismiss = { viewModel.dismissFolderEditor() },
            onSave = { name, colorHex -> viewModel.saveFolder(name, colorHex) }
        )
    }

    // Delete Alarm Confirmation Dialog
    alarmToDelete?.let { alarm ->
        ConfirmDeleteDialog(
            title = "Delete Alarm",
            message = "Are you sure you want to delete this alarm (${alarm.formattedTime()} ${alarm.amPmMarker()})?",
            onConfirm = { viewModel.confirmDeleteAlarm() },
            onDismiss = { viewModel.cancelDeleteAlarm() }
        )
    }

    // Delete Folder Confirmation Dialog
    folderToDelete?.let { folder ->
        ConfirmDeleteDialog(
            title = "Delete Folder",
            message = "Are you sure you want to delete folder \"${folder.name}\"? Any alarms inside it will be moved to Uncategorized.",
            onConfirm = { viewModel.confirmDeleteFolder() },
            onDismiss = { viewModel.cancelDeleteFolder() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmTopBar(
    currentView: ScreenView,
    folders: List<AlarmFolder>,
    onBackClick: () -> Unit,
    onEditFolderClick: (AlarmFolder) -> Unit,
    onDeleteFolderClick: (AlarmFolder) -> Unit
) {
    when (currentView) {
        is ScreenView.FolderDetail -> {
            val folder = folders.find { it.id == currentView.folderId }
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (folder != null) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(folder.colorHex), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = folder.name,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        } else {
                            Text("Folder")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to folders"
                        )
                    }
                },
                actions = {
                    if (folder != null) {
                        IconButton(onClick = { onEditFolderClick(folder) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit folder")
                        }
                        IconButton(onClick = { onDeleteFolderClick(folder) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete folder",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
        else -> {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Time Control",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
private fun AlarmFab(
    currentView: ScreenView,
    onAddAlarm: () -> Unit,
    onCreateFolder: () -> Unit
) {
    when (currentView) {
        is ScreenView.FoldersHub -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCreateFolder,
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Folder")
                }

                ExtendedFloatingActionButton(
                    onClick = onAddAlarm,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Alarm") },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        is ScreenView.FolderDetail -> {
            ExtendedFloatingActionButton(
                onClick = onAddAlarm,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Alarm") },
                shape = RoundedCornerShape(16.dp)
            )
        }
        is ScreenView.AllAlarms -> {
            FloatingActionButton(
                onClick = onAddAlarm,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    }
}

@Composable
private fun FoldersHubContent(
    folders: List<AlarmFolder>,
    alarms: List<AlarmItem>,
    onFolderClick: (String) -> Unit,
    onEditFolder: (AlarmFolder) -> Unit,
    onDeleteFolder: (AlarmFolder) -> Unit,
    onCreateFolder: () -> Unit
) {
    val unfiledAlarms = alarms.filter { it.folderId == null }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Alarm Folders",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
            Text(
                text = "Organize alarms into categories so you only see what you need.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
            )
        }

        items(folders, key = { it.id }) { folder ->
            FolderCard(
                folder = folder,
                alarms = alarms,
                onClick = { onFolderClick(folder.id) },
                onEdit = { onEditFolder(folder) },
                onDelete = { onDeleteFolder(folder) }
            )
        }

        // Unfiled alarms card if any exist
        if (unfiledAlarms.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onFolderClick("") }, // Empty string for unfiled
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Uncategorized",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${unfiledAlarms.size} alarm${if (unfiledAlarms.size != 1) "s" else ""}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // "Create New Folder" dashed/outlined card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onCreateFolder() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Create New Folder",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun FolderDetailContent(
    folder: AlarmFolder?,
    alarms: List<AlarmItem>,
    onAlarmClick: (AlarmItem) -> Unit,
    onToggleAlarm: (Long, Boolean) -> Unit,
    onDeleteAlarm: (AlarmItem) -> Unit,
    onAddAlarm: () -> Unit
) {
    if (alarms.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Alarms Here",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This folder has no alarms yet.\nTap below to set an alarm for this category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onAddAlarm,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Alarm")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    folder = folder,
                    onToggle = { enabled -> onToggleAlarm(alarm.id, enabled) },
                    onClick = { onAlarmClick(alarm) },
                    onDelete = { onDeleteAlarm(alarm) },
                    showFolderBadge = false
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun AllAlarmsContent(
    alarms: List<AlarmItem>,
    folders: List<AlarmFolder>,
    onAlarmClick: (AlarmItem) -> Unit,
    onToggleAlarm: (Long, Boolean) -> Unit,
    onDeleteAlarm: (AlarmItem) -> Unit,
    onAddAlarm: () -> Unit
) {
    if (alarms.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Alarms Created",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap the + button to create your first alarm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onAddAlarm,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Alarm")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(alarms, key = { it.id }) { alarm ->
                val folder = folders.find { it.id == alarm.folderId }
                AlarmCard(
                    alarm = alarm,
                    folder = folder,
                    onToggle = { enabled -> onToggleAlarm(alarm.id, enabled) },
                    onClick = { onAlarmClick(alarm) },
                    onDelete = { onDeleteAlarm(alarm) },
                    showFolderBadge = true
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun ExactAlarmPermissionBanner(
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Exact Alarm Permission Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Allow exact alarms so your alarms ring on time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
            TextButton(onClick = onOpenSettings) {
                Text(
                    text = "ALLOW",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
