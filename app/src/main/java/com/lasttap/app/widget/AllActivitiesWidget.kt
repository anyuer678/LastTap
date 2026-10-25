package com.lasttap.app.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.lasttap.app.data.Repository
import com.lasttap.app.util.TimeFormat

/**
 * 全部活动小组件：窄尺寸单列列表，宽尺寸自动两列网格（紧凑排布），
 * 每行/每格独立可点，点谁就记录谁。
 */
class AllActivitiesWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(140.dp, 110.dp), DpSize(280.dp, 110.dp)),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val activities = Repository.get(context).getAllActivities()

        provideContent {
            GlanceTheme {
                val wide = LocalSize.current.width >= 220.dp
                val rows = if (wide) activities.chunked(2) else activities.map { listOf(it) }
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp),
                ) {
                    if (activities.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "点时：还没有活动，打开应用创建",
                                maxLines = 2,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            itemsIndexed(rows) { _, row ->
                                Row(modifier = GlanceModifier.fillMaxWidth()) {
                                    row.forEach { activity ->
                                        Row(
                                            modifier = GlanceModifier
                                                .defaultWeight()
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .clickable(recordAction(activity.id)),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = GlanceModifier.defaultWeight()) {
                                                Text(
                                                    text = activity.name,
                                                    maxLines = 1,
                                                    style = TextStyle(
                                                        color = GlanceTheme.colors.onBackground,
                                                        fontWeight = FontWeight.Medium,
                                                    ),
                                                )
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
                                                        color = GlanceTheme.colors.onSurfaceVariant,
                                                        fontSize = 12.sp,
                                                    ),
                                                )
                                            }
                                            // 行尾的 ✓ 是“点这行=记录一次”的固定锚点。
                                            Image(
                                                provider = ImageProvider(
                                                    com.lasttap.app.R.drawable.ic_widget_check
                                                ),
                                                contentDescription = null,
                                                modifier = GlanceModifier.size(15.dp),
                                            )
                                        }
                                    }
                                    // 宽布局奇数行补一个空位，保证每格等宽。
                                    if (row.size == 1 && wide) {
                                        Box(modifier = GlanceModifier.defaultWeight()) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
