package com.lasttap.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lasttap.app.data.ActivityEntity
import com.lasttap.app.data.Backup
import com.lasttap.app.data.RecordEntity
import com.lasttap.app.data.Repository
import com.lasttap.app.widget.WidgetRefresher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository.get(app)

    val activities: StateFlow<List<ActivityEntity>> = repo.observeActivities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeRecords(activityId: Long) = repo.observeRecords(activityId)

    /**
     * 数据变更后刷新小组件。[activityId] 非空时走定向渲染
     * （只刷绑定了该活动的单活动组件 + 全部活动组件）；结构性变更传 null 走全量。
     */
    private fun mutate(activityId: Long? = null, block: suspend (Repository) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block(repo)
            WidgetRefresher.request(getApplication(), activityId)
        }
    }

    fun add(name: String, mergeWindowSeconds: Int = 0) =
        mutate { it.add(name, mergeWindowSeconds) }
    fun rename(id: Long, name: String) = mutate(id) { it.rename(id, name) }
    fun setMergeWindow(id: Long, seconds: Int) = mutate(id) { it.setMergeWindow(id, seconds) }
    fun delete(id: Long) = mutate(id) { it.delete(id) }
    fun move(id: Long, up: Boolean) = mutate { it.move(id, up) }
    fun undoLastRecord(id: Long) = mutate(id) { it.undoLastRecord(id) }

    /** 删除任意一条历史记录，随后重算该活动的最近时间/次数并定向刷新小组件。 */
    fun deleteRecord(record: RecordEntity) = mutate(record.activityId) {
        it.deleteRecord(record.id)
    }

    /**
     * 记录一次（应用内入口）。返回 false 表示本次点击被合并窗口吸收
     * （视为同一次记录，未插入新记录）或活动已不存在。
     */
    suspend fun record(id: Long): Boolean = withContext(Dispatchers.IO) {
        val recorded = repo.record(id, System.currentTimeMillis())
        if (recorded) WidgetRefresher.request(getApplication(), id)
        recorded
    }

    suspend fun export(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = Backup.toJson(repo.getAllActivities(), repo.getAllRecords())
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("无法写入所选文件")
        }
    }

    suspend fun import(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val text = getApplication<Application>().contentResolver.openInputStream(uri)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: error("无法读取所选文件")
            // 先完整解析校验；解析失败会在此抛出，不会执行覆盖。
            val backup = Backup.fromJson(text)
            repo.import(backup)
            WidgetRefresher.request(getApplication())
        }
    }
}
