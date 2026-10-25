package com.lasttap.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetSync {
    /** 让两种小组件的所有已放置实例重新走一遍 provideGlance，从数据库读取最新数据。 */
    suspend fun updateAllWidgets(context: Context) {
        SingleActivityWidget().updateAll(context)
        AllActivitiesWidget().updateAll(context)
    }
}
