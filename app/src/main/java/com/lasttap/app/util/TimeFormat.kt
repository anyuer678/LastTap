package com.lasttap.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 时间展示：普通活动固定 "yyyy-MM-dd HH:mm"；设了连击合并窗口的活动
 * 自动带秒并向下取整到窗口整数。均按设备当前时区转换。
 */
object TimeFormat {
    private val withMinutes = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val withSeconds = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun format(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(withMinutes)

    /**
     * 连击合并窗口的时间显示：时间戳向下取整到窗口的整数倍，且总是显示秒。
     * 例如 30 秒窗口：22:00:07 与 22:00:29 都显示 22:00:00，22:00:31 起显示 22:00:30——
     * 同一窗口内无论怎么点击，显示保持恒定，与“窗口内视为同一次”语义一致。
     */
    fun formatWindowed(epochMillis: Long, windowSeconds: Int): String {
        val windowMs = windowSeconds * 1000L
        val floored = epochMillis - Math.floorMod(epochMillis, windowMs)
        return Instant.ofEpochMilli(floored)
            .atZone(ZoneId.systemDefault())
            .format(withSeconds)
    }

    /** 按活动配置选择展示方式：设了合并窗口则窗口对齐 + 自动带秒；否则到分钟。 */
    fun formatForActivity(epochMillis: Long, mergeWindowSeconds: Int): String =
        if (mergeWindowSeconds > 0) {
            formatWindowed(epochMillis, mergeWindowSeconds)
        } else {
            format(epochMillis)
        }
}
