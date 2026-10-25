package com.lasttap.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lasttap.app.data.ActivityEntity
import com.lasttap.app.util.TimeFormat
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    vm: AppViewModel,
    onOpen: (Long) -> Unit,
) {
    val activities by vm.activities.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var actionsTarget by remember { mutableStateOf<ActivityEntity?>(null) }
    var renameTarget by remember { mutableStateOf<ActivityEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<ActivityEntity?>(null) }
    var windowTarget by remember { mutableStateOf<ActivityEntity?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                vm.export(uri)
                    .onSuccess { toast(context, "已导出") }
                    .onFailure { toast(context, "导出失败：${it.message}") }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("点时") },
                actions = {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("导出备份（JSON）") },
                            onClick = {
                                menuOpen = false
                                exportLauncher.launch("lasttap-backup-${LocalDate.now()}.json")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("导入备份") },
                            onClick = {
                                menuOpen = false
                                importLauncher.launch(arrayOf("application/json", "text/*"))
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建活动")
            }
        },
    ) { padding ->
        if (activities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "还没有活动\n\n点击右下角 + 创建活动，\n然后长按桌面空白处添加“点时”小组件。",
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(activities, key = { it.id }) { activity ->
                    ListItem(
                        headlineContent = { Text(activity.name) },
                        supportingContent = {
                            Text(
                                "上次：" + (
                                    activity.lastRecordedAt
                                        ?.let {
                                            TimeFormat.formatForActivity(
                                                it, activity.mergeWindowSeconds
                                            )
                                        }
                                        ?: "尚无记录"
                                    ),
                            )
                        },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalIconButton(onClick = {
                                    scope.launch {
                                        if (!vm.record(activity.id)) {
                                            toast(context, "合并窗口内：视为同一次记录")
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "记录")
                                }
                                IconButton(onClick = { actionsTarget = activity }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "操作")
                                }
                            }
                        },
                        modifier = Modifier.clickable { onOpen(activity.id) },
                    )
                }
            }
        }
    }

    if (showAdd) {
        CreateActivityDialog(
            onConfirm = { name, window ->
                vm.add(name, window)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }

    renameTarget?.let { target ->
        NameDialog(
            title = "重命名",
            initial = target.name,
            confirmLabel = "保存",
            onConfirm = {
                vm.rename(target.id, it)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            name = target.name,
            onConfirm = {
                vm.delete(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }

    actionsTarget?.let { target ->
        ItemActionsDialog(
            name = target.name,
            onRename = {
                renameTarget = target
                actionsTarget = null
            },
            onMergeWindow = {
                windowTarget = target
                actionsTarget = null
            },
            onMoveUp = {
                vm.move(target.id, up = true)
                actionsTarget = null
            },
            onMoveDown = {
                vm.move(target.id, up = false)
                actionsTarget = null
            },
            onDelete = {
                deleteTarget = target
                actionsTarget = null
            },
            onDismiss = { actionsTarget = null },
        )
    }

    windowTarget?.let { target ->
        MergeWindowDialog(
            currentSeconds = target.mergeWindowSeconds,
            onPick = {
                vm.setMergeWindow(target.id, it)
                windowTarget = null
            },
            onDismiss = { windowTarget = null },
        )
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("导入备份？") },
            text = {
                Text("导入会替换当前全部数据。备份文件会先完整解析，解析失败不会影响现有数据。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri = null
                        scope.launch {
                            vm.import(uri)
                                .onSuccess { toast(context, "导入成功") }
                                .onFailure { toast(context, "导入失败：${it.message}") }
                        }
                    },
                ) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text("取消")
                }
            },
        )
    }
}
