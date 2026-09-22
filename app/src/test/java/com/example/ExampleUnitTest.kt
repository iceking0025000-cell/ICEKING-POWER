package com.example

import com.example.data.BatteryHistoryPoint
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testBatteryHistoryMetrics() {
        val now = System.currentTimeMillis()
        val history = listOf(
            BatteryHistoryPoint(now - 86400000L, 90),
            BatteryHistoryPoint(now - 43200000L, 40),
            BatteryHistoryPoint(now, 75)
        )

        val levels = history.map { it.level }
        val maxLevel = levels.maxOrNull() ?: 0
        val minLevel = levels.minOrNull() ?: 0

        assertEquals(90, maxLevel)
        assertEquals(40, minLevel)

        val safetyFloor = 20
        val target = 15
        val safeMargin = 75 - safetyFloor
        val canSupportTransfer = safeMargin >= target
        assertTrue(canSupportTransfer)
    }
}

