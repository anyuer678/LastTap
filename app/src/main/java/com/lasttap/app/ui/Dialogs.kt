package com.lasttap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 连击合并窗口可选项：窗口内的重复点击视为同一次记录。 */
val MERGE_WINDOW_OPTIONS: List<Pair<Int, String>> = listOf(
    0 to "不合并",
    30 to "30 秒",
    60 to "1 分钟",
    300 to "5 分钟",
)

/** 创建活动：名称 + 连击合并窗口。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateActivityDialog(
    onConfirm: (name: String, mergeWindowSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var window by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建活动") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("活动名称") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "连击合并：窗口内的重复点击只记一次（以第一次为准）",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MERGE_WINDOW_OPTIONS.forEach { (seconds, label) ->
                        FilterChip(
                            selected = window == seconds,
                            onClick = { window = seconds },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), window) },
                enabled = name.isNotBlank(),
            ) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 修改已有活动的连击合并窗口。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MergeWindowDialog(
    currentSeconds: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("连击合并窗口") },
        text = {
            Column {
                Text(
                    "窗口内的重复点击只记一次（以第一次为准）。适合“背单词”这类需要快速连点、不想产生重复记录的活动。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MERGE_WINDOW_OPTIONS.forEach { (seconds, label) ->
                        FilterChip(
                            selected = currentSeconds == seconds,
                            onClick = { onPick(seconds) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("活动名称") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) onConfirm(trimmed)
                },
                enabled = name.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun DeleteConfirmDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除“$name”？") },
        text = {
            Text("将删除该活动的全部历史记录，对应的小组件也不会再产生有效记录。此操作不可撤销。")
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("删除") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 列表项的行内操作菜单（重命名 / 合并间隔 / 上移 / 下移 / 删除）。 */
@Composable
fun ItemActionsDialog(
    name: String,
    onRename: () -> Unit,
    onMergeWindow: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onRename) { Text("重命名") }
                TextButton(onClick = onMergeWindow) { Text("连击合并窗口") }
                TextButton(onClick = onMoveUp) { Text("上移") }
                TextButton(onClick = onMoveDown) { Text("下移") }
                TextButton(onClick = onDelete) { Text("删除") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
