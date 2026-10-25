package com.lasttap.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.lasttap.app.data.RecordEntity
import com.lasttap.app.util.TimeFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vm: AppViewModel,
    activityId: Long,
    onBack: () -> Unit,
) {
    val activities by vm.activities.collectAsState()
    val activity = activities.firstOrNull { it.id == activityId }
    val records by vm.observeRecords(activityId).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<RecordEntity?>(null) }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.name ?: "活动详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 连续快速点击难免手滑多记，这里可撤销最近一次。
                    TextButton(
                        onClick = {
                            vm.undoLastRecord(activityId)
                            toast(context, "已撤销最近一次记录")
                        },
                        enabled = records.isNotEmpty(),
                    ) { Text("撤销") }
                },
            )
        },
        floatingActionButton = {
            if (activity != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (!vm.record(activityId)) {
                                toast(context, "合并窗口内：视为同一次记录")
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    text = { Text("立即记录") },
                )
            }
        },
    ) { padding ->
        if (activity == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("该活动已删除")
            }
            return@Scaffold
        }
        val window = activity.mergeWindowSeconds
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("上次记录", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = activity.lastRecordedAt
                            ?.let { TimeFormat.formatForActivity(it, window) }
                            ?: "尚无记录",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "共 ${activity.recordCount} 次记录",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                text = "历史记录（最近在前）",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(records, key = { it.id }) { record ->
                    ListItem(
                        headlineContent = {
                            Text(TimeFormat.formatForActivity(record.recordedAt, window))
                        },
                        trailingContent = {
                            IconButton(onClick = { deleteTarget = record }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除这条记录")
                            }
                        },
                    )
                }
                if (records.isEmpty()) {
                    item {
                        Text(
                            text = "尚无记录",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除这条记录？") },
            text = {
                val targetWindow = activities
                    .firstOrNull { it.id == target.activityId }?.mergeWindowSeconds ?: 0
                Text(
                    "将删除 ${TimeFormat.formatForActivity(target.recordedAt, targetWindow)} " +
                        "的记录，该活动的最近记录时间与次数会随之重算。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteRecord(target)
                    toast(context, "已删除该条记录")
                    deleteTarget = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}
