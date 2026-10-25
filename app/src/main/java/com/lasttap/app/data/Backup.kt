package com.lasttap.app.data

import org.json.JSONArray
import org.json.JSONObject

data class BackupActivity(
    val name: String,
    val createdAt: Long,
    val records: List<Long>,
    val mergeWindowSeconds: Int = 0,
)

data class BackupData(
    val version: Int,
    val exportedAt: Long,
    val activities: List<BackupActivity>,
)

/** JSON 备份的序列化/反序列化（使用 Android 内置 org.json，不引额外依赖）。 */
object Backup {
    private const val VERSION = 1

    fun toJson(activities: List<ActivityEntity>, records: List<RecordEntity>): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        val array = JSONArray()
        for (activity in activities) {
            val obj = JSONObject()
            obj.put("id", activity.id)
            obj.put("name", activity.name)
            obj.put("createdAt", activity.createdAt)
            obj.put("mergeWindowSeconds", activity.mergeWindowSeconds)
            val times = JSONArray()
            records.filter { it.activityId == activity.id }
                .sortedBy { it.recordedAt }
                .forEach { times.put(it.recordedAt) }
            obj.put("records", times)
            array.put(obj)
        }
        root.put("activities", array)
        return root.toString(2)
    }

    /**
     * 解析失败时抛出异常；调用方只有在解析成功后才允许执行覆盖式导入。
     */
    fun fromJson(text: String): BackupData {
        val root = JSONObject(text)
        val array = root.optJSONArray("activities")
            ?: throw IllegalArgumentException("备份文件缺少 activities 字段")
        val now = System.currentTimeMillis()
        val activities = ArrayList<BackupActivity>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.optString("name").trim()
            if (name.isEmpty()) continue
            val times = ArrayList<Long>()
            val records = obj.optJSONArray("records")
            if (records != null) {
                for (j in 0 until records.length()) times.add(records.optLong(j))
            }
            activities.add(
                BackupActivity(
                    name = name,
                    createdAt = obj.optLong("createdAt", now),
                    records = times.sorted(),
                    mergeWindowSeconds = obj.optInt("mergeWindowSeconds", 0),
                )
            )
        }
        if (activities.isEmpty() && root.optInt("version", -1) != VERSION) {
            throw IllegalArgumentException("备份文件格式不正确")
        }
        return BackupData(
            version = root.optInt("version", VERSION),
            exportedAt = root.optLong("exportedAt", now),
            activities = activities,
        )
    }
}
