package com.lasttap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lasttap.app.ui.ActivitiesScreen
import com.lasttap.app.ui.AppViewModel
import com.lasttap.app.ui.HistoryScreen
import com.lasttap.app.ui.LastTapTheme
import com.lasttap.app.widget.WidgetRefresher

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 自愈：应用被打开时请求一次组件刷新，修复任何极端情况下
        // 组件内容与数据库不一致的残留状态（例如系统丢更新）。
        WidgetRefresher.request(applicationContext)
        setContent {
            LastTapTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot(vm: AppViewModel = viewModel()) {
    // 应用只有两级界面：活动列表 <-> 活动详情（历史记录）。
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = selectedId
    if (selected == null) {
        ActivitiesScreen(vm = vm, onOpen = { selectedId = it })
    } else {
        HistoryScreen(vm = vm, activityId = selected, onBack = { selectedId = null })
    }
}
