package br.edu.robotcontroller

import org.junit.Assert.*
import org.junit.Test

class JoystickGeometryTest {
    @Test fun thumbFitsAtBothExtremesOnShortAndWideScreens() {
        for (width in listOf(140f, 260f, 480f)) for (height in listOf(100f, 180f, 300f)) {
            val g = JoystickGeometry.fit(width, height, 32f, 8f)
            val reach = g.travel + g.thumbRadius
            assertTrue(g.centerX - reach >= 0f)
            assertTrue(g.centerX + reach <= width)
            assertTrue(g.centerY - reach >= 0f)
            assertTrue(g.centerY + reach <= height - 32f)
        }
    }
    @Test fun smallWindowsDoNotProduceNegativeRadius() {
        val g = JoystickGeometry.fit(10f, 20f, 32f, 8f)
        assertEquals(0f, g.radius, 0f)
        assertEquals(0f, g.travel, 0f)
    }
}
