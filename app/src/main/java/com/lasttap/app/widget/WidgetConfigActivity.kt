package com.lasttap.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.lasttap.app.MainActivity
import com.lasttap.app.data.ActivityEntity
import com.lasttap.app.data.Repository
import com.lasttap.app.ui.LastTapTheme
import com.lasttap.app.util.TimeFormat
import kotlinx.coroutines.launch

/**
 * 单活动小组件的配置页：添加小组件时选择要记录的活动。
 * 选择结果写入该小组件实例的 Glance 状态（DataStore），随系统持久化，
 * 重启 / 换机后仍生效。
 */
class WidgetConfigActivity : ComponentActivity() {

    private val appWidgetId: Int
        get() = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = appWidgetId
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // 用户中途退出时按“取消”处理，系统不会放置小组件。
        setResult(RESULT_CANCELED, resultIntent(widgetId))

        lifecycleScope.launch {
            val activities = Repository.get(applicationContext).getAllActivities()
            setContent {
                LastTapTheme {
                    ConfigContent(
                        activities = activities,
                        onPick = { bind(widgetId, it) },
                        onCancel = { finish() },
                        onOpenApp = {
                            startActivity(Intent(this@WidgetConfigActivity, MainActivity::class.java))
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun resultIntent(widgetId: Int) =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

    private fun bind(widgetId: Int, activityId: Long) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity)
                .getGlanceIdBy(widgetId)
            updateAppWidgetState(applicationContext, glanceId) { prefs ->
                prefs[SingleActivityWidget.KEY_ACTIVITY_ID] = activityId
            }
            SingleActivityWidget().update(this@WidgetConfigActivity, glanceId)
            setResult(RESULT_OK, resultIntent(widgetId))
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigContent(
    activities: List<ActivityEntity>,
    onPick: (Long) -> Unit,
    onCancel: () -> Unit,
    onOpenApp: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择要记录的活动") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "取消")
                    }
                },
            )
        },
    ) { padding ->
        if (activities.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("还没有活动。先打开应用创建一个活动，再添加小组件。")
                Spacer(Modifier.height(16.dp))
                Button(onClick = onOpenApp) { Text("打开应用创建") }
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
                                activity.lastRecordedAt
                                    ?.let { TimeFormat.formatForActivity(it, activity.mergeWindowSeconds) }
                                    ?: "尚无记录"
                            )
                        },
                        modifier = Modifier.clickable { onPick(activity.id) },
                    )
                }
            }
        }
    }
}
