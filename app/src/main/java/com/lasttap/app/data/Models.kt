package com.lasttap.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一个可记录的活动。lastRecordedAt / recordCount 是冗余字段，
 * 供小组件单次查询直接渲染；随每次记录在同一事务内更新。
 *
 * mergeWindowSeconds：连击合并窗口。窗口内的重复点击视为同一次记录，
 * 不再插入新记录、也不更新时间；0 表示不合并。
 */
@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRecordedAt: Long? = null,
    val recordCount: Int = 0,
    val mergeWindowSeconds: Int = 0,
)

/** 每次点击产生一条历史记录，时间戳为 epoch 毫秒（UTC），展示时再转本地时区。 */
@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("activityId"), Index("recordedAt")],
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val activityId: Long,
    val recordedAt: Long,
)
