package com.lasttap.app

import com.lasttap.app.data.Backup
import com.lasttap.app.data.RecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 验证备份导出/导入的序列化与校验逻辑（org.json 需要 Robolectric 提供 Android 运行时）。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupTest {

    @Test
    fun `导出再导入保留全部活动与记录`() {
        val activities = listOf(
            com.lasttap.app.data.ActivityEntity(
                id = 1, name = "复习计组", sortOrder = 0, createdAt = 1000,
                lastRecordedAt = 3000, recordCount = 2,
            ),
            com.lasttap.app.data.ActivityEntity(
                id = 2, name = "检查 N100", sortOrder = 1, createdAt = 2000,
            ),
        )
        val records = listOf(
            RecordEntity(id = 1, activityId = 1, recordedAt = 2000),
            RecordEntity(id = 2, activityId = 1, recordedAt = 3000),
        )

        val json = Backup.toJson(activities, records)
        val parsed = Backup.fromJson(json)

        assertEquals(2, parsed.activities.size)
        assertEquals("复习计组", parsed.activities[0].name)
        assertEquals(listOf(2000L, 3000L), parsed.activities[0].records)
        assertTrue(parsed.activities[1].records.isEmpty())
        assertEquals("检查 N100", parsed.activities[1].name)
    }

    @Test
    fun `非法文件解析抛出异常而不是返回空数据`() {
        assertThrows(Exception::class.java) { Backup.fromJson("这不是 JSON") }
        assertThrows(Exception::class.java) { Backup.fromJson("""{"foo": 1}""") }
    }

    @Test
    fun `缺失字段使用默认值且记录按时间排序`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1,
              "activities": [
                {"name": "背单词", "records": [500, 100, 300]}
              ]
            }
        """.trimIndent()

        val parsed = Backup.fromJson(json)
        assertEquals(1, parsed.activities.size)
        assertEquals(listOf(100L, 300L, 500L), parsed.activities[0].records)
        assertTrue(parsed.activities[0].createdAt > 0)
    }

    @Test
    fun `空名称的活动被跳过`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 1,
              "activities": [
                {"name": "  ", "records": []},
                {"name": "有效活动", "records": []}
              ]
            }
        """.trimIndent()

        val parsed = Backup.fromJson(json)
        assertEquals(1, parsed.activities.size)
        assertEquals("有效活动", parsed.activities[0].name)
    }
}
