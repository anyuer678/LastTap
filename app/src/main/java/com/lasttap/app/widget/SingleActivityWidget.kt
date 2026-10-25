package com.lasttap.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.lasttap.app.MainActivity
import com.lasttap.app.data.Repository
import com.lasttap.app.util.TimeFormat

/**
 * 单活动小组件：整卡即按钮。
 * 时间是主视觉（大号 + 加粗 + 主题主色 + ✓ 图标），点击记录后变化一目了然。
 * 绑定哪个活动存在 Glance 状态（DataStore）里，由 WidgetConfigActivity 写入。
 */
class SingleActivityWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState<Preferences>(context, id)
        val activityId = prefs[KEY_ACTIVITY_ID]
        val repo = Repository.get(context)
        val activity = activityId?.let { repo.getActivity(it) }

        provideContent {
            GlanceTheme {
                if (activity == null) {
                    // 未绑定（尚未配置，或活动已被删除）：点击进入应用处理。
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(GlanceTheme.colors.widgetBackground)
                            .cornerRadius(16.dp)
                            .clickable(
                                actionStartActivity(Intent(context, MainActivity::class.java))
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "点时：长按小组件选择活动",
                            maxLines = 2,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                            ),
                        )
                    }
                } else {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(GlanceTheme.colors.widgetBackground)
                            .cornerRadius(16.dp)
                            .clickable(recordAction(activity.id)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = activity.name,
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                            ),
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(com.lasttap.app.R.drawable.ic_widget_check),
                                contentDescription = null,
                                modifier = GlanceModifier.size(17.dp),
                            )
                            Spacer(modifier = GlanceModifier.width(5.dp))
                            Text(
                                text = activity.lastRecordedAt
                                    ?.let {
                                        TimeFormat.formatForActivity(
                                            it, activity.mergeWindowSeconds
                                        )
                                    }
                                    ?: "尚无记录",
                                maxLines = 1,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onBackground,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        val KEY_ACTIVITY_ID = longPreferencesKey("activity_id")
    }
}
