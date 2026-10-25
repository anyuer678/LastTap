package com.lasttap.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getById(id: Long): ActivityEntity?

    @Query("SELECT MAX(sortOrder) FROM activities")
    suspend fun maxSortOrder(): Int?

    @Insert
    suspend fun insert(activity: ActivityEntity): Long

    @Update
    suspend fun update(activity: ActivityEntity)

    @Delete
    suspend fun delete(activity: ActivityEntity)

    @Query("DELETE FROM activities")
    suspend fun deleteAll()
}

@Dao
interface RecordDao {
    @Insert
    suspend fun insert(record: RecordEntity): Long

    @Query("SELECT * FROM records WHERE activityId = :activityId ORDER BY recordedAt DESC, id DESC")
    fun observeForActivity(activityId: Long): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE activityId = :activityId ORDER BY recordedAt DESC, id DESC LIMIT 1")
    suspend fun latestForActivity(activityId: Long): RecordEntity?

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getById(id: Long): RecordEntity?

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COALESCE(MAX(recordedAt), 0) FROM records WHERE activityId = :activityId")
    suspend fun maxRecordedAt(activityId: Long): Long

    @Query("SELECT COUNT(*) FROM records WHERE activityId = :activityId")
    suspend fun countForActivity(activityId: Long): Int

    @Query("SELECT * FROM records")
    suspend fun getAll(): List<RecordEntity>

    @Query("DELETE FROM records")
    suspend fun deleteAll()
}
