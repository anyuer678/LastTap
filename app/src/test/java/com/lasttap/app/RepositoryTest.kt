package com.lasttap.app

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lasttap.app.data.AppDatabase
import com.lasttap.app.data.Repository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 合并窗口与撤销的核心数据逻辑（内存 Room + Robolectric）。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: Repository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = Repository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `合并窗口内的重复点击只记一次`() = runBlocking {
        repo.add("背单词")
        val id = repo.getAllActivities().first().id
        repo.setMergeWindow(id, 60)

        assertTrue(repo.record(id, 1_000_000L))
        // 窗口内：视为同一次，不插入新记录、不更新时间
        assertFalse(repo.record(id, 1_000_000L + 30_000))
        // 窗口外：正常记录
        assertTrue(repo.record(id, 1_000_000L + 61_000))

        val activity = repo.getActivity(id)!!
        assertEquals(2, activity.recordCount)
        assertEquals(1_000_000L + 61_000, activity.lastRecordedAt)
    }

    @Test
    fun `未设置窗口时每次点击都产生记录`() = runBlocking {
        repo.add("检查 N100")
        val id = repo.getAllActivities().first().id
        assertTrue(repo.record(id, 100L))
        assertTrue(repo.record(id, 200L))
        assertTrue(repo.record(id, 300L))
        assertEquals(3, repo.getActivity(id)!!.recordCount)
    }

    @Test
    fun `撤销后重算最近时间与次数`() = runBlocking {
        repo.add("复习计组")
        val id = repo.getAllActivities().first().id
        repo.record(id, 100L)
        repo.record(id, 200L)

        assertTrue(repo.undoLastRecord(id))
        var activity = repo.getActivity(id)!!
        assertEquals(1, activity.recordCount)
        assertEquals(100L, activity.lastRecordedAt)

        assertTrue(repo.undoLastRecord(id))
        activity = repo.getActivity(id)!!
        assertEquals(0, activity.recordCount)
        assertNull(activity.lastRecordedAt)

        // 没有可撤销的记录了
        assertFalse(repo.undoLastRecord(id))
    }

    @Test
    fun `删除任意一条记录后重算冗余字段`() = runBlocking {
        repo.add("复习计组")
        val id = repo.getAllActivities().first().id
        repo.record(id, 100L)
        repo.record(id, 200L)
        repo.record(id, 300L)

        // 删除中间一条（200），最近记录仍是 300
        val middle = repo.getAllRecords().first { it.recordedAt == 200L }
        assertTrue(repo.deleteRecord(middle.id))
        var activity = repo.getActivity(id)!!
        assertEquals(2, activity.recordCount)
        assertEquals(300L, activity.lastRecordedAt)

        // 删除最新一条（300），最近记录回退到 100
        val latest = repo.getAllRecords().first { it.recordedAt == 300L }
        assertTrue(repo.deleteRecord(latest.id))
        activity = repo.getActivity(id)!!
        assertEquals(1, activity.recordCount)
        assertEquals(100L, activity.lastRecordedAt)

        // 删光后清空
        val rest = repo.getAllRecords().first()
        assertTrue(repo.deleteRecord(rest.id))
        activity = repo.getActivity(id)!!
        assertEquals(0, activity.recordCount)
        assertNull(activity.lastRecordedAt)

        // 记录不存在时返回 false
        assertFalse(repo.deleteRecord(middle.id))
    }
}
