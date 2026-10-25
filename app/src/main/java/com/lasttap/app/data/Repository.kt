package com.lasttap.app.data

import android.content.Context
import androidx.room.withTransaction

class Repository internal constructor(private val db: AppDatabase) {

    private val activityDao = db.activityDao()
    private val recordDao = db.recordDao()

    fun observeActivities() = activityDao.observeAll()
    fun observeRecords(activityId: Long) = recordDao.observeForActivity(activityId)

    suspend fun getActivity(id: Long): ActivityEntity? = activityDao.getById(id)
    suspend fun getAllActivities(): List<ActivityEntity> = activityDao.getAll()
    suspend fun getAllRecords(): List<RecordEntity> = recordDao.getAll()

    suspend fun add(name: String, mergeWindowSeconds: Int = 0) {
        activityDao.insert(
            ActivityEntity(
                name = name,
                sortOrder = (activityDao.maxSortOrder() ?: -1) + 1,
                mergeWindowSeconds = mergeWindowSeconds,
            )
        )
    }

    suspend fun setMergeWindow(id: Long, seconds: Int) {
        val activity = activityDao.getById(id) ?: return
        activityDao.update(activity.copy(mergeWindowSeconds = seconds))
    }

    suspend fun rename(id: Long, name: String) {
        val activity = activityDao.getById(id) ?: return
        activityDao.update(activity.copy(name = name))
    }

    suspend fun delete(id: Long) {
        val activity = activityDao.getById(id) ?: return
        activityDao.delete(activity)
    }

    /** 与相邻项交换 sortOrder，实现上移/下移。 */
    suspend fun move(id: Long, up: Boolean) {
        val list = activityDao.getAll()
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        val target = if (up) index - 1 else index + 1
        if (target !in list.indices) return
        val current = list[index]
        val neighbor = list[target]
        db.withTransaction {
            activityDao.update(current.copy(sortOrder = neighbor.sortOrder))
            activityDao.update(neighbor.copy(sortOrder = current.sortOrder))
        }
    }

    /**
     * 记录一次执行：插入历史 + 更新冗余的 lastRecordedAt/recordCount，
     * 两者在同一事务中完成。
     *
     * 连击合并：若该活动设置了合并窗口，且本次点击距上一次“已采纳”的记录
     * 不足窗口时长，则视为同一次记录——不插入新记录、不更新时间，返回 false。
     * 返回 false 的另一种情况是活动不存在（已被删除）。
     */
    suspend fun record(activityId: Long, at: Long): Boolean {
        val activity = activityDao.getById(activityId) ?: return false
        val windowMs = activity.mergeWindowSeconds * 1000L
        val last = activity.lastRecordedAt
        if (windowMs > 0 && last != null && at - last < windowMs) {
            return false
        }
        db.withTransaction {
            recordDao.insert(RecordEntity(activityId = activityId, recordedAt = at))
            activityDao.update(
                activity.copy(lastRecordedAt = at, recordCount = activity.recordCount + 1)
            )
        }
        return true
    }

    /**
     * 撤销某活动最近一次记录：删除最新一条，并用剩余记录重算冗余字段。
     * 返回 false 表示该活动没有可撤销的记录。
     */
    suspend fun undoLastRecord(activityId: Long): Boolean {
        val latest = recordDao.latestForActivity(activityId) ?: return false
        db.withTransaction {
            recordDao.deleteById(latest.id)
            recomputeAggregate(activityId)
        }
        return true
    }

    /**
     * 删除任意一条历史记录（应用内），并用剩余记录重算该活动的冗余字段。
     * 返回 false 表示记录不存在（可能已被删除）。
     */
    suspend fun deleteRecord(recordId: Long): Boolean {
        val record = recordDao.getById(recordId) ?: return false
        db.withTransaction {
            recordDao.deleteById(recordId)
            recomputeAggregate(record.activityId)
        }
        return true
    }

    /** 用剩余记录重算活动的 lastRecordedAt / recordCount（事务内调用）。 */
    private suspend fun recomputeAggregate(activityId: Long) {
        val count = recordDao.countForActivity(activityId)
        val last = recordDao.maxRecordedAt(activityId)
        val activity = activityDao.getById(activityId) ?: return
        activityDao.update(
            activity.copy(
                lastRecordedAt = if (last == 0L) null else last,
                recordCount = count,
            )
        )
    }

    /** 用备份整体替换现有数据（调用方必须先完成解析校验）。 */
    suspend fun import(backup: BackupData) {
        db.withTransaction {
            recordDao.deleteAll()
            activityDao.deleteAll()
            backup.activities.forEachIndexed { order, item ->
                val newId = activityDao.insert(
                    ActivityEntity(
                        name = item.name,
                        sortOrder = order,
                        createdAt = item.createdAt,
                        mergeWindowSeconds = item.mergeWindowSeconds,
                    )
                )
                item.records.forEach { at ->
                    recordDao.insert(RecordEntity(activityId = newId, recordedAt = at))
                }
                activityDao.update(
                    ActivityEntity(
                        id = newId,
                        name = item.name,
                        sortOrder = order,
                        createdAt = item.createdAt,
                        lastRecordedAt = item.records.lastOrNull(),
                        recordCount = item.records.size,
                        mergeWindowSeconds = item.mergeWindowSeconds,
                    )
                )
            }
        }
    }

    companion object {
        @Volatile
        private var instance: Repository? = null

        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(AppDatabase.get(context)).also { instance = it }
            }

        /** 仅供测试：单测（Robolectric）之间需要隔离状态。 */
        internal fun resetForTests() {
            synchronized(this) {
                AppDatabase.resetForTests()
                instance = null
            }
        }
    }
}
