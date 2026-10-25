package com.lasttap.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * GlanceAppWidgetReceiver 会在系统启动、桌面请求 APPWIDGET_UPDATE、
 * 应用数据更新等时机自动触发 provideGlance 重新渲染，
 * 因此重启手机 / 进程被杀后小组件都会从数据库恢复正确内容。
 */
class SingleActivityWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleActivityWidget()
}

class AllActivitiesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AllActivitiesWidget()
}
