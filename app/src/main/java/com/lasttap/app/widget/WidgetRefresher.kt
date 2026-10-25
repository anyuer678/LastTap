package com.lasttap.app.widget

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.updateIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 组件刷新调度器：连击合并 + 串行渲染 + 渲染限速。
 *
 * 背景：连续高频点击时，如果每次点击都各自触发全量重渲染，多个渲染并发交错，
 * 既拖慢桌面，也可能让渲染互相踩踏，表现为组件不再更新（数据其实已写入数据库）。
 *
 * 机制：
 *  - 每次点击/数据变更只登记“哪个活动脏了”（或全量脏），再往 CONFLATED 通道发一个唤醒信号；
 *  - 唯一的消费循环收到信号后立刻渲染一次（孤立点击零人为延迟）；
 *  - 渲染进行中新到达的点击留在脏集合里，下一轮立即渲染——永不丢失、永不并发；
 *  - **渲染限速**：两次渲染至少间隔 250ms。连击风暴时每一轮都会把积压的点击
 *    合并成一次渲染（始终渲染最新状态），渲染队列不会越积越多，桌面不被拖卡。
 *  - 若活动设置了连击合并窗口，窗口内的点击根本不会产生新记录，也就不会触发渲染。
 *
 * 排查：`adb logcat -s LastTapPerf` 可看到每轮渲染耗时。
 */
object WidgetRefresher {

    private const val TAG = "LastTapPerf"
    private const val MIN_RENDER_INTERVAL_MS = 250L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val wake = Channel<Unit>(capacity = Channel.CONFLATED)
    private val dirtyActivities = ConcurrentHashMap.newKeySet<Long>()
    private val started = AtomicBoolean(false)
    private val renderMutex = Mutex()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var allDirty = false

    @Volatile
    private var lastRenderAt = 0L

    /**
     * 请求刷新小组件。[activityId] 传本次变更的活动 id（定向刷新）；
     * 传 null 表示结构性变更（增删、排序、导入、偏好），走全量渲染。
     */
    fun request(context: Context, activityId: Long? = null) {
        appContext = context.applicationContext
        if (activityId != null) {
            dirtyActivities.add(activityId)
        } else {
            allDirty = true
        }
        wake.trySend(Unit)
        if (started.compareAndSet(false, true)) {
            scope.launch { consumeLoop() }
        }
    }

    private suspend fun consumeLoop() {
        for (ignored in wake) {
            try {
                val ctx = appContext ?: continue
                // 渲染限速：距上一轮渲染不足 250ms 时先等待，
                // 等待期间新到的点击继续合并进即将开始的这一轮。
                val sinceLast = SystemClock.elapsedRealtime() - lastRenderAt
                if (sinceLast in 1 until MIN_RENDER_INTERVAL_MS) {
                    delay(MIN_RENDER_INTERVAL_MS - sinceLast)
                }
                val startedAt = SystemClock.elapsedRealtime()
                val doAll = allDirty
                allDirty = false
                val ids = dirtyActivities.toList()
                ids.forEach { dirtyActivities.remove(it) }
                renderMutex.withLock {
                    runCatching {
                        when {
                            doAll -> WidgetSync.updateAllWidgets(ctx)
                            ids.isNotEmpty() -> {
                                SingleActivityWidget().updateIf<Preferences>(ctx) { prefs ->
                                    (prefs[SingleActivityWidget.KEY_ACTIVITY_ID] ?: -1L) in ids
                                }
                                AllActivitiesWidget().updateAll(ctx)
                            }
                        }
                    }.onFailure { Log.e(TAG, "小组件刷新失败", it) }
                }
                lastRenderAt = SystemClock.elapsedRealtime()
                Log.d(TAG, "渲染一轮耗时 ${lastRenderAt - startedAt}ms（全量=$doAll, 活动=$ids）")
            } catch (t: Throwable) {
                Log.e(TAG, "刷新循环异常", t)
            }
        }
    }
}
