package com.lasttap.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import com.lasttap.app.data.Repository

/**
 * 小组件上的“记录”点击回调。由系统以广播方式唤醒进程执行，全程不打开应用。
 * 写库成功后只向 WidgetRefresher 请求一次刷新；连击会被合并，
 * 渲染串行执行，避免并发渲染导致组件卡死不更新。
 */
class RecordAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val activityId = parameters[KEY_ACTIVITY_ID] ?: return
        val startedAt = android.os.SystemClock.elapsedRealtime()
        val recorded = Repository.get(context).record(activityId, System.currentTimeMillis())
        android.util.Log.d(
            "LastTapPerf",
            "点击处理 ${android.os.SystemClock.elapsedRealtime() - startedAt}ms" +
                "（${if (recorded) "已记录" else "合并窗口内/活动不存在"}）",
        )
        if (recorded) {
            WidgetRefresher.request(context, activityId)
        }
    }

    companion object {
        val KEY_ACTIVITY_ID = ActionParameters.Key<Long>("activity_id")
    }
}

/** 构造一个“点击即记录该活动”的 Action，供两种小组件复用。 */
fun recordAction(activityId: Long): Action =
    actionRunCallback<RecordAction>(
        actionParametersOf(RecordAction.KEY_ACTIVITY_ID to activityId)
    )
