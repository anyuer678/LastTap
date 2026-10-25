package com.lasttap.app

import com.lasttap.app.util.TimeFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TimeFormatTest {

    private val originalTimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        // 固定时区，保证断言确定。
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `格式为 yyyy-MM-dd HH_mm`() {
        val epoch = ZonedDateTime.of(2026, 10, 24, 22, 16, 0, 0, ZoneId.of("Asia/Shanghai"))
            .toInstant().toEpochMilli()
        assertEquals("2026-10-24 22:16", TimeFormat.format(epoch))
    }

    @Test
    fun `同一时刻在不同时区展示不同`() {
        val epoch = ZonedDateTime.of(2026, 10, 24, 22, 16, 0, 0, ZoneId.of("Asia/Shanghai"))
            .toInstant().toEpochMilli()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals("2026-10-24 14:16", TimeFormat.format(epoch))
    }

    @Test
    fun `30秒窗口内显示对齐到0或30秒`() {
        val base = ZonedDateTime.of(2026, 10, 24, 22, 0, 0, 0, ZoneId.of("Asia/Shanghai"))
            .toInstant().toEpochMilli()
        // 窗口内（0-29 秒）显示 22:00:00，窗口外（30-59 秒）显示 22:00:30
        assertEquals("2026-10-24 22:00:00", TimeFormat.formatWindowed(base + 7_000, 30))
        assertEquals("2026-10-24 22:00:00", TimeFormat.formatWindowed(base + 29_999, 30))
        assertEquals("2026-10-24 22:00:30", TimeFormat.formatWindowed(base + 31_000, 30))
    }

    @Test
    fun `1分钟窗口显示对齐到整分`() {
        val base = ZonedDateTime.of(2026, 10, 24, 22, 0, 0, 0, ZoneId.of("Asia/Shanghai"))
            .toInstant().toEpochMilli()
        assertEquals("2026-10-24 22:00:00", TimeFormat.formatWindowed(base + 59_999, 60))
        assertEquals("2026-10-24 22:01:00", TimeFormat.formatWindowed(base + 63_000, 60))
    }

    @Test
    fun `无窗口时按分钟精度显示，有窗口则自动带秒并取整`() {
        val epoch = ZonedDateTime.of(2026, 10, 24, 22, 0, 7, 0, ZoneId.of("Asia/Shanghai"))
            .toInstant().toEpochMilli()
        assertEquals("2026-10-24 22:00", TimeFormat.formatForActivity(epoch, 0))
        assertEquals("2026-10-24 22:00:00", TimeFormat.formatForActivity(epoch, 30))
    }
}
